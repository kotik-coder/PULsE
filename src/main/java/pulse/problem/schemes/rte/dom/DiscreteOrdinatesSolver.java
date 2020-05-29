package pulse.problem.schemes.rte.dom;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.schemes.rte.MathUtils;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;

public class DiscreteOrdinatesSolver extends RadiativeTransferSolver {
		
	private int n; //number of direction pairs
	private int nT; 
	
	private double albedo;
	private double A1;
	private double emissivity;
	
	private EmissionFunction emissionFunction;
	private LegendrePoly discreteDirections;
	private StretchedGrid grid;
	
	private double[][] I;
	private double[] U;
	
	private double[] mu;
	private double[] w;
	
	private final static double DOUBLE_PI	= 2.0*Math.PI;
//	private final static double L_STABLE_X1 = 0.4358665215;
	
	private final static double adaptiveErSq = 5e-4;
	
	/*
	 * RK Coefficients
	 */
	
	public Vector b, bHat;
	public Vector c;	
	public Matrix rk;
	
	private double qLeft, qRight;
	
	private double iterationError;
	private double boundaryFluxFactor;
	
	private double[] localFlux, localFluxDerivative;
	private double[] storedF, storedFD;
	
	/**
	 * Constructs a discrete ordinates solver using the parameters (emissivity, scattering albedo and optical thickness)
	 * declared by the {@code problem} object. The nodes and weights of the quadrature are initialised using an instance of the 
	 * {@code LegendrePoly class}.
	 * @see pulse.problem.schemes.rte.dom.GaussianQuadrature
	 * @param problem statement
	 * @param n even number of direction pairs for DOM passed to the {@Code LegendrePoly constructor}
	 * @param N is the number of equal-length grid segments
	 * @throws IllegalArgumentException if n is odd or less than two 
	 */
	
	private DiscreteOrdinatesSolver(ParticipatingMedium problem, Grid grid, final int n) {
		super(problem, grid);
		
		if(n % 2 != 0 || n < 2)
			throw new IllegalArgumentException("Odd n received: " + n + " (please use an even integer)");
	
		iterationError = (double)NumericProperty.theDefault(NumericPropertyKeyword.DOM_ITERATION_ERROR).getValue();
		
		emissionFunction = new EmissionFunction(0.0, (int)(grid.getGridDensity().getValue()));
		init(problem, grid);
		
		this.setQuadraturePoints( NumericProperty.derive(NumericPropertyKeyword.DOM_DIRECTIONS, n));
		
		this.rkInitFehlberg();
				
	}
	
	
	public DiscreteOrdinatesSolver(ParticipatingMedium problem, Grid grid) {
		this(problem, grid, (int)NumericProperty.theDefault(NumericPropertyKeyword.DOM_DIRECTIONS).getValue() );
	}
	
	@Override
	public void init(ParticipatingMedium problem, Grid grid) {		
		super.init(problem, grid);
		
		if(! (problem instanceof ParticipatingMedium))
			throw new IllegalArgumentException("Not a participating medimum problem statement: " + problem.getSimpleName());
		
		this.albedo = (double)((ParticipatingMedium)problem).getScatteringAlbedo().getValue();
		this.A1 = (double)((ParticipatingMedium)problem).getScatteringAnisostropy().getValue();
		
		this.emissivity = problem.getEmissivity();
		boundaryFluxFactor = (1.0 - emissivity)/(emissivity*Math.PI);
				
		emissionFunction.init(problem);
		
		this.grid = new StretchedGrid(this.getOpticalThickness());
		this.reinitArrays(this.grid.getDensity());
	}
	
	@Override
	public void compute(double[] tempArray) {
		
		this.U = new double[tempArray.length + 1]; //create an array with an extra element to avoid conditional statements in the source(...) method
		System.arraycopy(tempArray, 0, this.U, 0, tempArray.length); //copy all elements 
		this.U[tempArray.length] = U[tempArray.length - 1]; //copy the last element
		nT = tempArray.length - 1;
		
		emissionFunction.setGridStep( 1.0/(double)nT );
		
		this.clear();

		for( double iterationErrorSq = iterationError*iterationError, ql = 1, qr = 1; 
			   (MathUtils.fastPowLoop(this.qLeft - ql, 2)*
			   (MathUtils.fastPowLoop(this.qRight - qr, 2)) > iterationErrorSq); )  {
			ql = this.qLeft;
			qr = this.qRight;
			stepAdaptive();
		}
		
		this.fluxes();
	}
	
	private void clear() {
		for(int i = 0, j = 0, N = getExternalGridDensity(); i < n; i++)
			for(j = 0; j < N + 1; j++) 
				I[i][j] = 0;
		qLeft = 0.0;
		qRight = 0.0;
	}
	
	private void step() {
		int N = getExternalGridDensity();

		/*
		 * First set of ODE. Initial condition corresponds to I(0)
		 * /t ----> tau0
		 * The streams propagate in the positive hemisphere
		 */
		
		left(); //initial value for tau = 0
		
		for(int j = 0, i = 0; j < N; j++) {
		
			for(i = 0; i < n/2; i++) 
				I[i][j + 1] = weightedScheme(i, j, 1.0, 0.5);
			
		}
		
		/*
		 * Second set of ODE. Initial condition corresponds to I(tau0)
		 * /0 <---- t
		 * The streams propagate in the negative hemisphere
		 */		
		
		right(); //initial value for tau = tau_0	
		
		for(int j = N, i = 0; j > 0; j--) {
			
			for(i = n/2; i < n; i++) 
				I[i][j - 1] = weightedScheme(i, j, -1.0, 0.5);
			
		}		
		
	}
	
	private void stepAdaptive() {
		int N = getExternalGridDensity();

		double[] array;
		
		/*
		 * First set of ODE. Initial condition corresponds to I(0)
		 * /t ----> tau0
		 * The streams propagate in the positive hemisphere
		 */
			
		outer: for(	double erSq = 1.0 ; erSq > adaptiveErSq ; N = getExternalGridDensity() ) {
			
			erSq = 0;
			left(); //initial value for tau = 0
			
			for(int j = 0, i = 0; j < N; j++) {
			
				for(i = 0; (i < n/2) && (erSq < adaptiveErSq); i++) { 
					array = adaptiveRK(i, j, 1.0);
					I[i][j + 1] = array[0];
					erSq = array[1];						
				}
				
			}
			
			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0)
			 * /0 <---- t
			 * The streams propagate in the negative hemisphere
			 */		
			
			right(); //initial value for tau = tau_0	
			
			for(int j = N, i = 0; j > 0; j--) {
				
				for(i = n/2; (i < n) && (erSq < adaptiveErSq); i++) { 
					array = adaptiveRK(i, j, -1.0);
					I[i][j - 1] = array[0];
					erSq = array[1];				
				}
				
			}
			
			if(erSq < adaptiveErSq)
				break outer;
			else 
				reduceStepSize();

		}
		
	}
	
	private double[] adaptiveRK(int i, int j, final double sign) {
		
		double h = grid.step(j, sign);
		final double hSigned = h*sign;
		final double t = grid.getNode(j);
		
		double[] q = new double[b.dimension()];
		double sum = 0;
		
		double errorSq = 0;
		
		for(int m = 0; m < q.length; m++) {
			
				sum = 0;
				for(int k = 0; k < m; k++) 
					sum += rk.get(m, k)*q[k];
				
				q[m] = rhs(i, j, t + hSigned*c.get(m), I[i][j] + sum*hSigned);
				errorSq += (b.get(m) - bHat.get(m))*q[m];	
		}
		
		return new double[] {I[i][j] + hSigned*b.dot(new Vector(q)), errorSq*hSigned};
		
	}
	
	private boolean isErrorTooHigh(double prev, double next) {
		return (next - prev)*(next - prev) > 0.5*adaptiveErSq*(next + prev)*(next + prev);
	}

//	private void rkInitFSAL54() {
//		
//		rk = new Matrix(new double[][] {
//			{0.0,		0.0,	0.0,	0.0,	0.0}, 
//			{0.220428410259212,		0.220428410259212,	0.0,	0.0,	0.0}, 
//			{0.266080628790066,	0.266080628790066, 	0.220428410259212,	0.0,	0.0},
//			{0.227031047465079,	0.227031047465079,	-0.064393053775127,	0.220428410259212, 0.0},
//			{.175575441883476,	.175575441883476,	-0.415534431720558,	0.84395513769439,	0.220428410259212}
//		});
//		
//		b = new Vector(new double[] {.175575441883476,	.175575441883476,	-0.415534431720558,	0.84395513769439,	0.220428410259212} );
//		bHat = new Vector(new double[]{ 0.217113586697490, 0.217113586697490, 0.414811674412460, 0.150961152192560, 0.0 });	
//		c = new Vector(new double[] { 0.0, 0.440856821, 0.752589667839344, 0.610097451414243,	1.0});
//		
//	}
	
	private void rkInitFehlberg() {
		
		rk = new Matrix(new double[][] {
			{0.0,		0.0,	0.0,	0.0,	0.0}, 
			{0.25,		0.0,	0.0,	0.0,	0.0}, 
			{3.0/32.0,	9.0/32.0, 	0.0,	0.0,	0.0},
			{1932./2197.,	-7200./2197,	7296/2197.,	0.0,	0.0},
			{439./216.,	-8.0,	3680./513.,	-845./4104,	0.0},
			{-8.0/27.0,	2.0,	-3544.0/2565,	1859./4104.,	-11.0/40.0}
		});
		
		b = new Vector(new double[]{ 16.0/135.0, 0.0, 6656./12825.,	28561/56430., -9.0/50.0, 2.0/55.0 });
		bHat = new Vector(new double[]{ 25./216.,	0.0,	1408./2565.,	2197./4104.,	-1.0/5.0,	0.0 });	
		c = new Vector(new double[] { 0.0, 0.25, 3.0/8.0, 12.0/13.0, 1.0, 0.5 });
		
	}
	
//	private void rkInitLStable(double x) {
//		double alpha = (1.0 + x)*0.5;
//		double beta = -0.25*(6.0*x*x - 16.0*x + 1.0);
//		double gamma = 0.25*(6.0*x*x - 20.0*x + 5.0);
//		
//		rk = new Matrix(new double[][] {
//			{x,			0.0,			0.0}, 
//			{alpha - x,	x,				0.0}, 
//			{beta,		gamma, 			x} 
//		});
//		
//		b = new Vector(new double[]{beta, gamma, x });
//		c = new Vector(new double[]{x, alpha, 1.0});	
//		
//	}
	
	private double weightedScheme(int i, int j, final double sign, double weight) {
		final double h = grid.step(j, sign);
		final double mu_h = sign*mu[i]/h;
		return (source(i, j, grid.getNode(j)) + (mu_h - 1.0 + weight)*I[i][j])/(mu_h + weight);
	}

	private double rhs(int i, int j, double t, double intensity) {
		return 1.0/mu[i]*( source(i, j, t) - intensity );
	}
	
	/**
	 * Calculates the reflected intensity (positive angles, first half of indices)
	 * at the left boundary (tau = 0).
	 */
	
	private void left() {
		
		for(int i = 0; i < n/2; i++) 		//for positive streams
			I[i][0] = emissionFunction.radiance(U[0]) - boundaryFluxFactor*qLeft();

	}
	
	/**
	 * Calculates the reflected intensity (negative angles, second half of indices)
	 * at the right boundary (tau = tau_0).
	 */
	
	private void right() {		
		
		for(int i = n/2, N = getExternalGridDensity(); i < n; i++) 		//for negative streams
			I[i][N] = emissionFunction.radiance(U[nT]) + boundaryFluxFactor*qRight();	
			
	}
	
	private double henyeyGreensteinPF(int i, int j) {
		double result = 0;
		double a1 = 1.0 - A1*A1;
		double a2 = 1.0 + A1*A1;
		double b1 = 2.0*A1*mu[i];
		for(int k = 0; k < n/2; k++)
			result += w[k]*I[k][j]*a1/( (a2 - b1*mu[k]) *Math.sqrt(a2 - b1*mu[k]) ); 
		return result;
	}
	
	private double linearAnisotropicPF(int i, int j) {
		return g(j) + A1*mu[i]*q(j);
	}
	
	private double source(int i, int j, double t) {
		return (1.0 - albedo)*J(t) + 0.5*albedo*henyeyGreensteinPF(i, j);
	}
	
	private double J(double t) {
		double tdim = t/getOpticalThickness();
		double hx = 1.0/(nT);
		
		int floor = (int) ( tdim/hx ); //floor index
		double alpha = tdim/hx - floor;
		
		return emissionFunction.radiance( (1.0 - alpha)*U[floor] + alpha*U[floor + 1]);
		
	}
	
	private void fluxes() {
		int N = getExternalGridDensity();
		//calculate fluxes on DOM grid
		for(int i = 0; i < N + 1; i++)
			localFlux[i] = DOUBLE_PI*q(i);

		localFluxDerivative[0] = (localFlux[0] - localFlux[1])/grid.stepRight(0);
		for(int i = 1; i < N; i++)		
			localFluxDerivative[i] = ( localFlux[i - 1] - localFlux [i + 1] )/( grid.stepRight(i-1) + grid.stepLeft(i + 1) );
		localFluxDerivative[N] = (localFlux[N - 1] - localFlux[N]) / grid.stepLeft(N);
		
		//map on to the temperature grid
		setFlux(0, this.localFlux[0]);
		
		double hx = 1.0/(double)nT;
		for(int i = 1; i < nT - 1; i++) 
			setFlux(i, grid.interpolateUniform(localFlux, hx, i));
		
		setFlux(nT, this.localFlux[N]);
	}
	
	private double qLeft() {
		return qLeft = emissivity*(emissionFunction.power(U[0]) + DOUBLE_PI*q(0, n/2, n));
	}
	
	private double qRight() {
		return qRight = -emissivity*(emissionFunction.power(U[nT]) - DOUBLE_PI*q(getExternalGridDensity(), 0, n/2) );
	}
	
	private double q(int j, int startInclusive, int endExclusive) {
		double integral = 0;
		
		for(int i = startInclusive; i < endExclusive; i++)
			integral += w[i]*I[i][j]*mu[i];
		
		return integral;
		
	}
	
	private double q(int j) {
		double integral = 0;
		
		for(int i = 0, half = n/2; i < half; i++)
			integral += w[i]*(I[i][j] - I[i+half][j])*mu[i];
		
		return integral;
		
	}
	
	/**
	 * Calculates the net zeroth moment of intensity (i.e., the incident radiation) 
	 * for the positive hemisphere).
	 * @param j index on grid
	 * @return incident radiation (positive hemisphere)
	 */
	
	private double g(int j, int startInclusive, int endExclusive) {
		double integral = 0;
		
		for(int i = startInclusive; i < endExclusive; i++)
			integral += w[i]*I[i][j];
		
		return integral;
		
	}
	
	private double g(int j) {
		double integral = 0;
		
		for(int i = 0, half = n/2; i < half; i++)
			integral += w[i]*(I[i][j] + I[i+half][j]);
		
		return integral;
		
	}
	
	public static void main(String[] args) { 
	
		var problem = new ParticipatingMedium();		
		problem.setOpticalThickness(NumericProperty.derive(NumericPropertyKeyword.OPTICAL_THICKNESS, 0.1));
		problem.setEmissivity(NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, 0.85));
		
		File f = null;
		try {
			f = new File(DiscreteOrdinatesSolver.class.getResource("/test/TestSolution_Sharp.dat").toURI());
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
		var tauFactor = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
		Grid grid = new Grid(density, tauFactor);
		
		double tFactor = 10.0/800.0;
			
		var rte = new DiscreteOrdinatesSolver(problem, grid, 8);

		rte.albedo = 0.0;
		rte.A1 = 0.0;
		
		var emissionFunction = new EmissionFunction(tFactor, 1.0/N);
		rte.emissionFunction = emissionFunction;
		
		rte.compute(U);
		System.out.println(rte.getExternalGridDensity());
		
		System.out.printf("%n%2.4f %4.5f %4.5f", 
				rte.grid.getNode(0), 
				rte.getFlux(0),
				rte.getFluxDerivativeFront());
		
		for(int i = 1; i < U.length - 1; i++) 
			System.out.printf("%n%2.4f %4.5f %4.5f", 
					i*(1.0/(U.length - 1)*rte.getOpticalThickness()), 
					rte.getFlux(i),
					rte.getFluxDerivative(i));
		
	}
		
	public NumericProperty getQuadraturePoints() {
		return NumericProperty.derive(NumericPropertyKeyword.DOM_DIRECTIONS, n);
	}
	
	public void setQuadraturePoints(NumericProperty m) {
		if(m.getType() != NumericPropertyKeyword.DOM_DIRECTIONS)
			throw new IllegalArgumentException("Illegal type: " + m.getType());
		this.n = (int)m.getValue();
		
		discreteDirections = new GaussianQuadrature(n);
		discreteDirections.init();
		discreteDirections.setParent(this);
		
		mu = S8QuadratureSet.getNodes();
		w = S8QuadratureSet.getWeights();
		
		I = new double[n][this.getExternalGridDensity() + 1];
		
	}
	
	public NumericProperty getIterationErrorTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.DOM_ITERATION_ERROR, this.iterationError);
	}
	
	public void setIterationErrorTolerance(NumericProperty e) {
		if(e.getType() != NumericPropertyKeyword.DOM_ITERATION_ERROR)
			throw new IllegalArgumentException("Illegal type: " + e.getType());
		this.iterationError = (double)e.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
			case DOM_DIRECTIONS : setQuadraturePoints(property); break;
			case DOM_ITERATION_ERROR : setIterationErrorTolerance(property); break;
			default: return;
		}
		
		notifyListeners(this, property);
		 
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_DIRECTIONS));
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_ITERATION_ERROR));
		return list;				
	}
	
	@Override
	public String getDescriptor() {
		return "Discrete Ordinates Method (DOM)";
	}
	
	public String toString() {
		return "(DOM: " + this.getQuadraturePoints() + ")";
	}
	
	public LegendrePoly getQuadrature() {
		return discreteDirections;
	}
	
	private int roundEven(double a) {
		return (int) (a/2 * 2);
	}
	
	private void reduceStepSize() {
		int nNew = (int)( roundEven(1.5*getExternalGridDensity() ) );
		this.reinitArrays( nNew );
		grid.generateUniform(nNew, true);
	}
	
	@Override 
	public void reinitArrays(int N) { 
		super.reinitArrays(N);
		I = new double[n][N+1];
		localFlux			= new double[N+1];
		localFluxDerivative = new double[N+1];
		storedF		= new double[localFlux.length];
		storedFD	= new double[localFlux.length];
	}
	
	@Override
	public void store() {
		super.store();
		System.arraycopy(localFlux, 0, storedF, 0, storedF.length);
		System.arraycopy(localFluxDerivative, 0, storedFD, 0, storedFD.length);
	}
	
	@Override
	public int getExternalGridDensity() {
		return grid.getDensity();
	}
	
	@Override
	public double getFluxMeanDerivative(int u) {
		double hx = 1.0/(double)nT;
		return 0.5*( grid.interpolateUniform(localFluxDerivative, hx, u) +
				grid.interpolateUniform(storedFD, hx, u) );	
	}
	
	@Override
	public double getFluxMeanDerivativeFront() {
		int N = grid.getDensity();
		return 0.5*( this.localFluxDerivative[0] + this.storedFD[0] );	
	}
	
	@Override
	public double getFluxMeanDerivativeRear() {
		int N = grid.getDensity();
		return 0.5*( this.localFluxDerivative[N] + this.storedFD[N] );
	}
	
	@Override
	public double getFluxDerivative(int u) {
		return grid.interpolateUniform(localFluxDerivative, 1.0/(double)nT, u);	
	}
	
	@Override
	public double getFluxDerivativeFront() {
		return localFluxDerivative[0];
	}
	
	@Override
	public double getFluxDerivativeRear() {
		return localFluxDerivative[this.getExternalGridDensity()];
	}
	
}