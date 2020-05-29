package pulse.problem.schemes.solvers;

import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.MixedScheme;
import pulse.problem.schemes.rte.MathUtils;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.dom.DiscreteOrdinatesSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.InstanceDescriptor;

public class MixedCoupledSolver 
				extends MixedScheme 
						implements Solver<ParticipatingMedium> {

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
	
	protected RadiativeTransferSolver rte;
	protected double opticalThickness;
	protected double Np;
	
	protected final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	protected double a;
	protected double b;
	protected double c;

	protected double Bi1, Bi2;
	
	protected double HX2;
	
	protected double nonlinearPrecision = (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();	
	
	private static InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor
							= new InstanceDescriptor<RadiativeTransferSolver>("RTE Solver Selector", RadiativeTransferSolver.class);
	
	static { 
		instanceDescriptor.setSelectedDescriptor( DiscreteOrdinatesSolver.class.getSimpleName() );
	}
	
	public MixedCoupledSolver() {
		this(GRID_DENSITY, TAU_FACTOR);
	}
	
	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(GRID_DENSITY, TAU_FACTOR);
		sigma = (double)NumericProperty.theDefault(NumericPropertyKeyword.SCHEME_WEIGHT).getValue();
	}
	
	public MixedCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	protected void prepare(ParticipatingMedium problem) {
		super.prepare(problem);
				
		if(rte == null)
			initRTE(problem, grid);
		else {
			if(!rte.getSimpleName().equals( instanceDescriptor.getValue() ))
				initRTE(problem,grid);
		}
		
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
	public void solve(ParticipatingMedium problem) {

		prepare(problem);
		
		final double wFactor = timeInterval * tau * problem.timeFactor();
		final double errorSq = MathUtils.fastPowLoop( nonlinearPrecision, 2);
						
		rte.compute(U);
		
		// time cycle

		final double HX_NP = hx/Np;
		final double TAU0_NP = opticalThickness/Np;
		final double _2TAUHX = 2.0*tau*hx;
		final double HX2_2TAU = HX2/(2.0*tau);
		final double Bi2HX = Bi2*hx;
		final double SIGMA_NP = sigma/Np;
		final double ONE_MINUS_SIGMA_NP = (1. - sigma)/Np;
		final double _2TAU_ONE_MINUS_SIGMA = 2.0*tau*(1.0 - sigma);
		final double ONE_PLUS_Bi1_HX = (1. + Bi1*hx);
		final double BETA1_FACTOR = 1.0/(HX2 + 2.0*tau*sigma*(1 + hx*Bi1)); 
		final double ONE_MINUS_SIGMA = 1.0 - sigma;
		
		double phi;
		
		int i, m, w, j;
		double F, pls;		
		double V_0, V_N;
		
		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls 	 = discretePulse.evaluateAt( (m - 1 + EPS)*tau )*ONE_MINUS_SIGMA
						 + discretePulse.evaluateAt( (m - EPS)*tau )*sigma; 

				for( V_0 = errorSq + 1, V_N = errorSq + 1; 
						   (MathUtils.fastPowLoop((V[0] - V_0), 2) > errorSq) ||
						   (MathUtils.fastPowLoop((V[N] - V_N), 2) > errorSq)
						 ; rte.compute(V)) {
					
					//i = 0
					phi = TAU0_NP*rte.getFluxDerivativeFront();
					beta[1] = (_2TAUHX*(pls - SIGMA_NP*rte.getFlux(0) - ONE_MINUS_SIGMA_NP*rte.getStoredFlux(0) ) +
							HX2*(U[0] + phi*tau) + _2TAU_ONE_MINUS_SIGMA*(U[1] - U[0]*ONE_PLUS_Bi1_HX) )
							*BETA1_FACTOR;
					
					//i = 1
					phi = TAU0_NP*phiNextToFront();
					F = U[1] / tau + phi + ONE_MINUS_SIGMA*(U[2] - 2*U[1] + U[0])/HX2;
					beta[2] = (F + a * beta[1]) / (b - a * alpha[1]);

					for (i = 2; i < N - 1; i++) {
						phi = TAU0_NP*phi(i);
						F = U[i] / tau + phi + ONE_MINUS_SIGMA*(U[i+1] - 2*U[i] + U[i-1])/HX2;
						beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
					}
					
					//i = N - 1
					
					phi = TAU0_NP*phiNextToRear();
					F = U[N-1] / tau + phi + ONE_MINUS_SIGMA*(U[N] - 2*U[N-1] + U[N-2])/HX2;
					beta[N] = (F + a * beta[N-1]) / (b - a * alpha[N-1]);
					
					V_N = V[N];
					phi = TAU0_NP*rte.getFluxDerivativeRear();
					V[N] = (sigma*beta[N] + HX2_2TAU*U[N] + 0.5*HX2*phi + ONE_MINUS_SIGMA*(U[N-1] - U[N]*(1. + hx*Bi2) ) 
							+ HX_NP*(sigma*rte.getFlux(N) + ONE_MINUS_SIGMA*rte.getStoredFlux(N) ) )
							/(HX2_2TAU + sigma*(1. - alpha[N] + Bi2HX ));

					V_0 = V[0]; 
					for (j = N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
				}
				
				System.arraycopy(V, 0, U, 0, N + 1);
				rte.store();

			}

			curve.addPoint( w * wFactor, V[N] );
			
			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}
	
		curve.scale( maxTemp/curve.apparentMaximum() );

	}
	
	@Override
	public Class<? extends Problem> domain() {
		return ParticipatingMedium.class;
	}
	
	public RadiativeTransferSolver getRadiativeTransferEquation() {
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
		list.add(instanceDescriptor);
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
	
	public void initRTE(ParticipatingMedium problem, Grid grid) {
		rte = instanceDescriptor.newInstance(RadiativeTransferSolver.class, problem, grid);
		rte.setParent(this);
	}
	
	@Override
	public String toString() {
		return Messages.getString("MixedScheme2.4");
	}
	
	@Override
	public DifferenceScheme copy() {
		return new MixedCoupledSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	public static InstanceDescriptor<? extends RadiativeTransferSolver> getInstanceDescriptor() {
		return instanceDescriptor;
	}
	
	public static void setInstanceDescriptor( InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor) {
		MixedCoupledSolver.instanceDescriptor = instanceDescriptor;
	}
	
}