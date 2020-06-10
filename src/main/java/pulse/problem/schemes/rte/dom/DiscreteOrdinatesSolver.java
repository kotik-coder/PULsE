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

public class DiscreteOrdinatesSolver extends RadiativeTransferSolver {

	private final static double DOUBLE_PI = 2.0 * Math.PI;
	
	private double[] fluxDerivative;
	private double[] storedFluxDerivative;
	
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

		double tFactor = 10.0 / 800.0;

		var rte = new DiscreteOrdinatesSolver(problem, grid);
		rte.integrator.emissionFunction.setReductionFactor(tFactor);
		rte.integrator.setAlbedo(0.0);
		rte.integrator.pf.setAnisotropyFactor(0.0);
		rte.compute(U);

		for (int i = 0; i < rte.discrete.n; i++)
			System.out.println(rte.discrete.I[rte.getExternalGridDensity()][i]);

		System.out.printf("%n%2.4f %4.5f %4.5f", rte.discrete.grid.getNode(0), rte.getFlux(0),
				rte.getFluxDerivativeFront());

		//

//		for (int i = 0; i < rte.discrete.grid.getDensity(); i++)
//			System.out.printf("%n%2.4f %4.5f %4.5f %4.5f %4.5f", rte.discrete.grid.getNode(i), rte.discrete.I[i][0],
//					rte.discrete.I[i][1], rte.discrete.I[i][2], rte.discrete.I[i][3]);

		for (int i = 1; i < U.length - 1; i++)
			System.out.printf("%n%2.4f %4.5f %4.5f", i * (1.0 / (U.length - 1) * rte.getOpticalThickness()),
					rte.getFlux(i), rte.getFluxDerivative(i));

		System.out.printf("%n%2.4f %4.5f %4.5f", (U.length - 1) * (1.0 / (U.length - 1) * rte.getOpticalThickness()),
				rte.getFlux((U.length - 1)), rte.getFluxDerivativeRear());

	}

	private double iterationError;
	private PhaseFunction ipf;
	private AdaptiveIntegrator integrator;

	private DiscreteIntensities discrete;
	private int nT;

	/**
	 * Constructs a discrete ordinates solver using the parameters (emissivity,
	 * scattering albedo and optical thickness) declared by the {@code problem}
	 * object. The nodes and weights of the quadrature are initialised using an
	 * instance of the {@code LegendrePoly class}.
	 * 
	 * @see pulse.problem.schemes.rte.dom.GaussianQuadrature
	 * @param problem statement
	 * @param n       even number of direction pairs for DOM passed to the
	 *                {@Code LegendrePoly constructor}
	 * @param N       is the number of equal-length grid segments
	 * @throws IllegalArgumentException if n is odd or less than two
	 */

	public DiscreteOrdinatesSolver(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);

		iterationError = (double) NumericProperty.theDefault(NumericPropertyKeyword.DOM_ITERATION_ERROR).getValue();

		var emissionFunction = new EmissionFunction(problem, grid);

		discrete = new DiscreteIntensities((int)grid.getGridDensity().getValue(), (double) problem.getOpticalThickness().getValue());
		ipf = new HenyeyGreensteinIPF(discrete);
		integrator = new ExplicitRungeKutta(discrete, emissionFunction, ipf);

		init(problem, grid);
	}

	private void temperatureInterpolation(double[] tempArray) {
		nT = tempArray.length - 1;

		double[] xArray = new double[nT + 1];
		double h = 1.0 / nT;
		double tau0 = discrete.grid.getDimension();

		for (int i = 0; i < xArray.length; i++)
			xArray[i] = tau0 * i * h;

		var interpolator = new SplineInterpolator();
		UnivariateFunction function = interpolator.interpolate(xArray, tempArray);
		integrator.emissionFunction.setInterpolation(function);
	}

	@Override
	public void compute(double[] tempArray) {

		temperatureInterpolation(tempArray);
		discrete.clear();

		double relativeError = 100;

		double qld = 0;
		double qrd = 0;
		double qav = 0;

		for (double relTolSq = iterationError * iterationError, ql = 1, qr = 1; relativeError > relTolSq;) {
			ql = discrete.getQLeft();
			qr = discrete.getQRight();

			integrator.integrate();

			qld = (discrete.getQLeft() - ql);
			qrd = (discrete.getQRight() - qr);
			qav = ql + qr;

			relativeError = (qld * qld + qrd * qrd) / (qav * qav);
			//System.out.printf("%nRelative error: %1.2e", Math.sqrt( relativeError ) );
		}

		fluxesAndDerivatives();
	}
	
	public void fluxesAndDerivatives() {
		var interpolation = discrete.interpolateOnExternalGrid( integrator.f );
		fluxDerivative = new double[nT + 1];
		
		for (int i = 0; i < nT + 1; i++) {
			setFlux(i, DOUBLE_PI * discrete.q( interpolation[0], i));
			fluxDerivative[i] = -DOUBLE_PI * discrete.q( interpolation[1], i);
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
		return 0.5*(fluxDerivative[u] 
				+ storedFluxDerivative[u]);
	}

	@Override
	public double getFluxMeanDerivativeFront() {
		return 0.5*(fluxDerivative[0] + storedFluxDerivative[0]);
	}
	
	@Override
	public double getFluxMeanDerivativeRear() {
		return 0.5*(fluxDerivative[nT] + storedFluxDerivative[nT]);
	}

	public NumericProperty getIterationErrorTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.DOM_ITERATION_ERROR, this.iterationError);
	}

	@Override
	public void init(ParticipatingMedium problem, Grid grid) {
		super.init(problem, grid);

		ipf.setAnisotropyFactor((double) problem.getScatteringAnisostropy().getValue());
		discrete.setEmissivity(problem.getEmissivity());
		integrator.init(problem, grid);

		super.reinitArrays((int) grid.getGridDensity().getValue());
		discrete.grid.setDimension((double) problem.getOpticalThickness().getValue());
		discrete.grid.generateUniform(StretchedGrid.DEFAULT_GRID_DENSITY, true);
		
		discrete.reinitInternalArrays();
		storedFluxDerivative = new double[(int)grid.getGridDensity().getValue() + 1];
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_DIRECTIONS));
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_ITERATION_ERROR));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case DOM_ITERATION_ERROR:
			setIterationErrorTolerance(property);
			break;
		default:
			return;
		}

		notifyListeners(this, property);

	}

	public void setIterationErrorTolerance(NumericProperty e) {
		if (e.getType() != NumericPropertyKeyword.DOM_ITERATION_ERROR)
			throw new IllegalArgumentException("Illegal type: " + e.getType());
		this.iterationError = (double) e.getValue();
	}

	@Override
	public void store() {
		super.store();
		System.arraycopy(fluxDerivative, 0, storedFluxDerivative, 0, fluxDerivative.length);
	}

}