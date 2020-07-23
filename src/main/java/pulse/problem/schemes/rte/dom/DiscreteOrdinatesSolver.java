package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationListener;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

public class DiscreteOrdinatesSolver extends RadiativeTransferSolver {

	private final static double DOUBLE_PI = 2.0 * Math.PI;

	private double[] fluxDerivative;
	private double[] storedFluxDerivative;

	private static InstanceDescriptor<AdaptiveIntegrator> integratorDescriptor = new InstanceDescriptor<AdaptiveIntegrator>(
			"Integrator selector", AdaptiveIntegrator.class);
	private static InstanceDescriptor<PhaseFunction> phaseFunctionSelector = new InstanceDescriptor<PhaseFunction>(
			"Phase function selector", PhaseFunction.class);
	private static InstanceDescriptor<IterativeSolver> iterativeSolverSelector = new InstanceDescriptor<IterativeSolver>(
			"Iterative solver selector", IterativeSolver.class);

	static {
		integratorDescriptor.setSelectedDescriptor(TRBDF2.class.getSimpleName());
		phaseFunctionSelector.setSelectedDescriptor(HenyeyGreensteinPF.class.getSimpleName());
		iterativeSolverSelector.setSelectedDescriptor(FixedIterations.class.getSimpleName());
	}

	private PhaseFunction phaseFunction;
	private AdaptiveIntegrator integrator;
	private IterativeSolver iterativeSolver;

	private DiscreteIntensities discrete;

	/**
	 * Constructs a discrete ordinates solver using the parameters (emissivity,
	 * scattering albedo and optical thickness) declared by the {@code problem}
	 * object. The nodes and weights of the quadrature are initialised using an
	 * instance of the {@code LegendrePoly class}.
	 * 
	 * @see pulse.problem.schemes.rte.dom.CompositeGaussianQuadrature
	 * @param problem statement
	 * @param n       even number of direction pairs for DOM passed to the
	 *                {@Code LegendrePoly constructor}
	 * @param N       is the number of equal-length grid segments
	 * @throws IllegalArgumentException if n is odd or less than two
	 */

	public DiscreteOrdinatesSolver(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);

		discrete = new DiscreteIntensities(problem, (int) grid.getGridDensity().getValue());
		discrete.setParent(this);

		phaseFunction = phaseFunctionSelector.newInstance(PhaseFunction.class, problem, discrete);
		var emissionFunction = new BlackbodySpectrum(problem);
		integrator = integratorDescriptor.newInstance(AdaptiveIntegrator.class, problem, discrete, emissionFunction,
				phaseFunction);
		iterativeSolver = iterativeSolverSelector.newInstance(IterativeSolver.class);

		integrator.setParent(this);
		iterativeSolver.setParent(this);
		init(problem, grid);

		integratorDescriptor.addListener(() -> setIntegrator(integratorDescriptor.newInstance(AdaptiveIntegrator.class,
				problem, discrete, emissionFunction, phaseFunction)));

		phaseFunctionSelector.addListener(
				() -> setPhaseFunction(phaseFunctionSelector.newInstance(PhaseFunction.class, problem, discrete)));

		iterativeSolverSelector
				.addListener(() -> setIterativeSolver(iterativeSolverSelector.newInstance(IterativeSolver.class)));

	}

	private void temperatureInterpolation(double dimension, double[] tempArray) {
		int N = tempArray.length;
		double h = 1.0 / (N - 1);

		double[] tArray = new double[N + 2];
		System.arraycopy(tempArray, 0, tArray, 1, N);

		double[] xArray = new double[N + 2];

		for (int i = 0; i <= N; i++) {
                    xArray[i + 1] = dimension * i * h;
                }

		/*
		 * Safety margins are introduced below
		 */

		xArray[0] = -h;
		tArray[0] = tArray[1];

		xArray[N + 1] = dimension + h;
		tArray[N + 1] = tArray[N];

		integrator.emissionFunction.setInterpolation(super.temperatureInterpolation(xArray, tArray));
	}

	@Override
	public RTECalculationStatus compute(double[] tempArray) {
		discrete.grid.generateUniform(true);
		discrete.reinitInternalArrays();

		temperatureInterpolation(discrete.getStretchedGrid().getDimension(), tempArray);

		var status = iterativeSolver.doIterations(discrete, integrator);

		if (status == RTECalculationStatus.NORMAL)
			fluxesAndDerivatives(tempArray.length);

		for (RTECalculationListener l : getRTEListeners()) {
                    l.onStatusUpdate(status);
                }

		return status;
	}

	public void fluxesAndDerivatives(int nExclusive) {
		var interpolation = discrete.interpolateOnExternalGrid(nExclusive, integrator.f);
		fluxDerivative = new double[nExclusive];

		for (int i = 0; i < nExclusive; i++) {
			setFlux(i, DOUBLE_PI * discrete.q(interpolation[0], i));
			fluxDerivative[i] = -DOUBLE_PI * discrete.q(interpolation[1], i);
		}
	}

	@Override
	public String getDescriptor() {
		return "Discrete Ordinates Method (DOM)";
	}

	@Override
	public int getExternalGridDensity() {
		return discrete.grid.getDensity();
	}

	@Override
	public double getFluxDerivative(int u) {
		return fluxDerivative[u];
	}

	@Override
	public double getFluxDerivativeFront() {
		return fluxDerivative[0];
	}

	@Override
	public double getFluxDerivativeRear() {
		return fluxDerivative[fluxDerivative.length - 1];
	}

	@Override
	public double getFluxMeanDerivative(int u) {
		return 0.5 * (fluxDerivative[u] + storedFluxDerivative[u]);
	}

	@Override
	public double getFluxMeanDerivativeFront() {
		return 0.5 * (fluxDerivative[0] + storedFluxDerivative[0]);
	}

	@Override
	public double getFluxMeanDerivativeRear() {
		return 0.5
				* (fluxDerivative[fluxDerivative.length - 1] + storedFluxDerivative[storedFluxDerivative.length - 1]);
	}

	@Override
	public void init(ParticipatingMedium problem, Grid grid) {
		super.init(problem, grid);

		phaseFunction.setAnisotropyFactor((double) problem.getScatteringAnisostropy().getValue());

		reinitArrays((int) grid.getGridDensity().getValue());

		discrete.grid = new StretchedGrid((double) problem.getOpticalThickness().getValue());
		discrete.reinitInternalArrays();

		integrator.init(problem, grid);
	}

	@Override
	public void reinitArrays(int gridDensity) {
		super.reinitArrays(gridDensity);
		storedFluxDerivative = new double[gridDensity + 1];
		fluxDerivative = new double[gridDensity + 1];
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(integratorDescriptor);
		list.add(phaseFunctionSelector);
		list.add(iterativeSolverSelector);
		return list;
	}

	public AdaptiveIntegrator getIntegrator() {
		return integrator;
	}

	@Override
	public void store() {
		super.store();
		System.arraycopy(fluxDerivative, 0, storedFluxDerivative, 0, fluxDerivative.length);
	}

	public PhaseFunction getPhaseFunction() {
		return phaseFunction;
	}

	public void setPhaseFunction(PhaseFunction phaseFunction) {
		this.phaseFunction = phaseFunction;
		integrator.setPhaseFunction(phaseFunction);
	}

	public static InstanceDescriptor<AdaptiveIntegrator> getIntegratorDescriptor() {
		return integratorDescriptor;
	}

	public static InstanceDescriptor<PhaseFunction> getPhaseFunctionSelector() {
		return phaseFunctionSelector;
	}

	public void setIntegrator(AdaptiveIntegrator integrator) {
		this.integrator = integrator;
		integrator.setParent(this);
	}

	public IterativeSolver getIterativeSolver() {
		return iterativeSolver;
	}

	public static InstanceDescriptor<IterativeSolver> getIterativeSolverSelector() {
		return iterativeSolverSelector;
	}

	public void setIterativeSolver(IterativeSolver solver) {
		this.iterativeSolver = solver;
		solver.setParent(this);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + this.getPhaseFunction();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// intentionally left blank
	}

}