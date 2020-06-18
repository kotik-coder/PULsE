package pulse.problem.schemes.rte.dom;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.DescriptorChangeListener;
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

	public static void main(String[] args) {

		var problem = new ParticipatingMedium();
		problem.setOpticalThickness(NumericProperty.derive(NumericPropertyKeyword.OPTICAL_THICKNESS, 0.1));
		problem.setEmissivity(NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, 0.85));

		File f = null;
		try {
			f = new File(DiscreteOrdinatesSolver.class.getResource("/test/TestSolution.dat").toURI());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		var data = new ArrayList<Double>();

		try (Scanner scanner = new Scanner(f)) {
			while (scanner.hasNextLine())
				data.add(Double.parseDouble(scanner.nextLine()));

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		double[] U = data.stream().mapToDouble(x -> x).toArray();
		int N = U.length - 1;

		var density = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, N);
		var tauFactor = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.0);
		Grid grid = new Grid(density, tauFactor);

		double tFactor = 36.7 / 800.0;

		var rte = new DiscreteOrdinatesSolver(problem, grid);
		rte.integrator.emissionFunction.setReductionFactor(tFactor);
		rte.integrator.setAlbedo(0.0);
		rte.integrator.pf.setAnisotropyFactor(0.0);
		rte.compute(U);

//		for (int i = 0; i < rte.discrete.n; i++)
//			System.out.println(rte.discrete.I[rte.getExternalGridDensity()][i]);
//

		for(int i = 0; i < rte.discrete.grid.getDensity() + 1; i++) 
			System.out.printf("%n%4.5f %1.6e %1.6e", rte.discrete.grid.getNode(i), rte.discrete.getIntensities()[i][0], rte.discrete.getIntensities()[i][1]);
		
		System.out.println();
		System.out.println();
		
//		for (int i = 0; i < rte.discrete.grid.getDensity(); i++)
//			System.out.printf("%n%2.4f %4.5f %4.5f", rte.discrete.grid.getNode(i), rte.discrete.I[i][0],
//					rte.discrete.I[i][1]);

		System.out.printf("%n%2.4f %4.5f %4.5f", rte.discrete.grid.getNode(0), rte.getFlux(0),
		rte.getFluxDerivativeFront());
		
		for (int i = 1; i < U.length - 1; i++)
			System.out.printf("%n%2.4f %4.5f %4.5f", i * (1.0 / (U.length - 1) * rte.getOpticalThickness()),
					rte.getFlux(i), rte.getFluxDerivative(i));

		System.out.printf("%n%2.4f %4.5f %4.5f", (U.length - 1) * (1.0 / (U.length - 1) * rte.getOpticalThickness()),
				rte.getFlux((U.length - 1)), rte.getFluxDerivativeRear());
		
	}

	private PhaseFunction phaseFunction;
	private AdaptiveIntegrator integrator;
	private IterativeSolver iterativeSolver;

	private DiscreteIntensities discrete;
	private int nT;

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
		
		var emissionFunction = new EmissionFunction(problem, grid);

		discrete = new DiscreteIntensities((int) grid.getGridDensity().getValue(),
				(double) problem.getOpticalThickness().getValue());
		discrete.setParent(this);
		
		phaseFunction = phaseFunctionSelector.newInstance(PhaseFunction.class, discrete);
		integrator = integratorDescriptor.newInstance(AdaptiveIntegrator.class, discrete, emissionFunction,
				phaseFunction);
		iterativeSolver = iterativeSolverSelector.newInstance(IterativeSolver.class);
		
		integrator.setParent(this);
		iterativeSolver.setParent(this);
		init(problem, grid);

		integratorDescriptor.addListener(new DescriptorChangeListener() {

			@Override
			public void onDescriptorChanged() {
				setIntegrator(integratorDescriptor.newInstance(AdaptiveIntegrator.class, discrete, emissionFunction,
						phaseFunction) );
			}

		});

		phaseFunctionSelector.addListener(new DescriptorChangeListener() {

			@Override
			public void onDescriptorChanged() {
				setPhaseFunction(phaseFunctionSelector.newInstance(PhaseFunction.class, discrete));
			}

		});
		
		iterativeSolverSelector.addListener(new DescriptorChangeListener() {

			@Override
			public void onDescriptorChanged() {
				setIterativeSolver(iterativeSolverSelector.newInstance(IterativeSolver.class));
			}
			
			
		});

	}

	private void temperatureInterpolation(double[] tempArray) {
		nT = tempArray.length - 1;

		double[] xArray = new double[nT + 3];
		double h = 1.0 / nT;
		double tau0 = discrete.grid.getDimension();

		for (int i = -1; i < nT + 2; i++)
			xArray[i+1] = tau0 * i * h;
		
		double[] tArray = new double[xArray.length];
		System.arraycopy(tempArray, 0, tArray, 1, tempArray.length);

		tArray[0]		= tArray[1];
		tArray[nT + 2]	= tArray[nT+1];
		
		integrator.emissionFunction.setInterpolation(super.temperatureInterpolation(xArray, tArray));
	}

	@Override
	public void compute(double[] tempArray) {
		temperatureInterpolation(tempArray);
		discrete.clear();
		iterativeSolver.doIterations(discrete, integrator);
		fluxesAndDerivatives();
	}

	public void fluxesAndDerivatives() {
		var interpolation = discrete.interpolateOnExternalGrid(integrator.f);
		fluxDerivative = new double[nT + 1];

		for (int i = 0; i < nT + 1; i++) {
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
		return fluxDerivative[nT];
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
		return 0.5 * (fluxDerivative[nT] + storedFluxDerivative[nT]);
	}

	@Override
	public void init(ParticipatingMedium problem, Grid grid) {
		super.init(problem, grid);

		phaseFunction.setAnisotropyFactor((double) problem.getScatteringAnisostropy().getValue());
		discrete.setEmissivity(problem.getEmissivity());
		integrator.init(problem, grid);

		super.reinitArrays((int) grid.getGridDensity().getValue());
		discrete.grid.setDimension((double) problem.getOpticalThickness().getValue());
		discrete.grid.generateUniform(true);

		discrete.reinitInternalArrays();
		storedFluxDerivative = new double[(int) grid.getGridDensity().getValue() + 1];
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
		//intentionally left blank
	}

}