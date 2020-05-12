package pulse.problem.schemes.solvers;

import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.schemes.MixedScheme;
import pulse.problem.schemes.radiation.RadiativeFluxCalculator;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public abstract class MixedCoupledSolver 
					extends MixedScheme 
							implements Solver<AbsorbingEmittingProblem> {

	/**
	 * The default value of {@code tauFactor}, which is set to {@code 1.0} for this
	 * scheme.
	 */

	public final static NumericProperty TAU_FACTOR = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 1.0);

	/**
	 * The default value of {@code gridDensity}, which is set to {@code 30} for this
	 * scheme.
	 */

	public final static NumericProperty GRID_DENSITY = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 16);
	
	protected int N;
	protected int counts;
	protected double hx;
	protected double tau;
	protected double maxTemp;
	
	protected double sigma; 
	
	protected HeatingCurve curve;
	
	protected double[] U, V;
	protected double[] alpha, beta;
	
	protected RadiativeFluxCalculator rte;
	protected double opticalThickness;
	protected double Np;
	
	protected final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	protected double a;
	protected double b;
	protected double c;

	protected double Bi1, Bi2;
	
	protected double HX2;
	
	protected double nonlinearPrecision = (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();	
	
	public MixedCoupledSolver() {
		this(GRID_DENSITY, TAU_FACTOR);
	}
	
	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(GRID_DENSITY, TAU_FACTOR);
		initRTE();
		sigma = (double)NumericProperty.theDefault(NumericPropertyKeyword.SCHEME_WEIGHT).getValue();
	}
	
	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	protected void prepare(AbsorbingEmittingProblem problem) {
		super.prepare(problem);
				
		curve = problem.getHeatingCurve();
		
		N	= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		maxTemp = (double) problem.getMaximumTemperature().getValue(); 

		counts = (int) curve.getNumPoints().getValue();

		Bi1 = (double) problem.getFrontHeatLoss().getValue();
		Bi2 = (double) problem.getHeatLossRear().getValue();

		rte.init(problem, grid);
		
		opticalThickness = (double)problem.getOpticalThickness().getValue();
		Np = (double)problem.getPlanckNumber().getValue();
		
		U = new double[N + 1];
		V = new double[N + 1];
		alpha = new double[N + 2];
		beta = new double[N + 2];
		
		adjustSchemeWeight();
			
		HX2 = hx*hx;
		
		a = sigma / HX2;
		b = 1. / tau + 2.*sigma / HX2;
		c = sigma / HX2;	
						
		alpha[1] = 1.0/(HX2/(2.0*tau*sigma) + 1. + hx*Bi1);
		
		for (int i = 1; i < N; i++)
			alpha[i + 1] = c / (b - a * alpha[i]);
		
	}
	
	
	
	@Override
	public Class<? extends Problem> domain() {
		return AbsorbingEmittingProblem.class;
	}
	
	public RadiativeFluxCalculator getRadiativeTransferEquation() {
		return rte;
	}
	
	public void setWeight(NumericProperty weight) {
		if(weight.getType() != NumericPropertyKeyword.SCHEME_WEIGHT)
			throw new IllegalArgumentException("Illegal type: " + weight.getType());
		this.sigma = (double)weight.getValue();
	}
	
	public NumericProperty getWeight() {
		return NumericProperty.derive(NumericPropertyKeyword.SCHEME_WEIGHT, sigma);
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.SCHEME_WEIGHT));
		list.add(NumericProperty.def(NumericPropertyKeyword.NONLINEAR_PRECISION));
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case SCHEME_WEIGHT : setWeight(property); break;
		case NONLINEAR_PRECISION : setNonlinearPrecision(property); break;
		default : super.set(type,property);
		}
	}
	
	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}
	
	private void adjustSchemeWeight() {
		double newSigma = 0.5 - hx*hx/(12.0*tau);
		setWeight(NumericProperty.derive(NumericPropertyKeyword.SCHEME_WEIGHT, newSigma > 0 ? newSigma : 0.5));
	}
	
	protected double phiNextToFront() {
		return 0.833333333*rte.getFluxMeanDerivative(1) + 0.083333333*(rte.getFluxMeanDerivativeFront() + rte.getFluxMeanDerivative(2));
	}
	
	protected double phiNextToRear() {
		return 0.833333333*rte.getFluxMeanDerivative(N-1) + 0.083333333*(rte.getFluxMeanDerivative(N-2) + rte.getFluxMeanDerivativeRear());
	}
	
	protected double phi(int i) {
		return 0.833333333*rte.getFluxMeanDerivative(i) + 0.083333333*(rte.getFluxMeanDerivative(i-1) + rte.getFluxMeanDerivative(i+1));
	}
	
	public abstract void initRTE();
	
}