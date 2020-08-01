package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.FluxesAndExplicitDerivatives;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

/**
 * A class that manages the solution of the radiative transfer equation using the discrete ordinates method.
 * The class provides an interface between the phase function, the ODE adaptive integrator and the iterative solver,
 * a combination of which is used to solve the RTE. 
 *
 */

public class DiscreteOrdinatesMethod extends RadiativeTransferSolver {

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

	/**
	 * Constructs a discrete ordinates solver using the parameters (emissivity,
	 * scattering albedo and optical thickness) declared by the {@code problem}
	 * object. 
	 * 
	 * @param problem the coupled problem statement
	 * @param grid the heat problem grid
	 */

	public DiscreteOrdinatesMethod(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
		final int N = (int) grid.getGridDensity().getValue();
		final double tau0 = (double) problem.getOpticalThickness().getValue();
		setFluxes(new FluxesAndExplicitDerivatives(N, tau0));

		var discrete = new DiscreteIntensities(problem);

		var emissionFunction = new BlackbodySpectrum(problem);
		
		phaseFunction = phaseFunctionSelector.newInstance(PhaseFunction.class, problem, discrete);
		integrator = integratorDescriptor.newInstance(AdaptiveIntegrator.class, problem, discrete, emissionFunction,
				phaseFunction);
		integrator.setIntensities(discrete);
		integrator.setParent(this);
		iterativeSolver = iterativeSolverSelector.newInstance(IterativeSolver.class);
		iterativeSolver.setParent(this);
		
		init(problem, grid);

		integratorDescriptor.addListener(() -> setIntegrator(integratorDescriptor.newInstance(AdaptiveIntegrator.class,
				problem, discrete, emissionFunction, phaseFunction)));

		phaseFunctionSelector.addListener(
				() -> setPhaseFunction(phaseFunctionSelector.newInstance(PhaseFunction.class, problem, discrete)));

		iterativeSolverSelector
				.addListener(() -> setIterativeSolver(iterativeSolverSelector.newInstance(IterativeSolver.class)));

	}

	@Override
	public RTECalculationStatus compute(double[] tempArray) {
		integrator.getEmissionFunction().setInterpolation( interpolateTemperatureProfile(tempArray) );

		var status = iterativeSolver.doIterations(integrator.getIntensities(), integrator);

		if (status == RTECalculationStatus.NORMAL)
			fluxesAndDerivatives(tempArray.length);

		fireStatusUpdate(status);
		return status;
	}

	private void fluxesAndDerivatives(final int nExclusive) {
		final var interpolation = HermiteInterpolator.interpolateOnExternalGrid(nExclusive, integrator);
		
		final double DOUBLE_PI = 2.0 * Math.PI;
		final var discrete = integrator.getIntensities();
		var fluxes = (FluxesAndExplicitDerivatives)getFluxes();
		
		for (int i = 0; i < nExclusive; i++) {
			fluxes.setFlux(i, DOUBLE_PI * discrete.flux(interpolation[0], i));
			fluxes.setFluxDerivative(i, -DOUBLE_PI * discrete.flux(interpolation[1], i) );
		}
	}

	@Override
	public String getDescriptor() {
		return "Discrete Ordinates Method (DOM)";
	}

	@Override
	public void init(ParticipatingMedium problem, Grid grid) {
		super.init(problem, grid);

		phaseFunction.setAnisotropyFactor((double) problem.getScatteringAnisostropy().getValue());

		getFluxes().setDensity((int)grid.getGridDensity().getValue());
		integrator.init(problem);
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