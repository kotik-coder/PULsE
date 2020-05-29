package pulse.problem.schemes.solvers;

import static java.lang.Math.pow;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;

import java.util.List;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.exact.AnalyticalDerivativeCalculator;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

public class ExplicitCoupledSolver 
					extends ExplicitScheme 
						implements Solver<ParticipatingMedium> {

	
	private double[] U;
	private double[] V;
	
	private RadiativeTransferSolver rte;
	private double opticalThickness;
	private double Np;
	private double Bi1, Bi2;	
	
	private HeatingCurve curve;
	private int N;
	private int counts;
	private double hx;
	private double tau;
	private double maxTemp;
	private double a,b;

	private final static double EPS = 1e-7; // a small value ensuring numeric stability
	
	private Mode mode = Mode.GENERAL;
	
	private double nonlinearPrecision = (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();	
	
	private static InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor
					= new InstanceDescriptor<RadiativeTransferSolver>("RTE Solver Selector", RadiativeTransferSolver.class);

	static { 
		instanceDescriptor.setSelectedDescriptor( AnalyticalDerivativeCalculator.class.getSimpleName() );
	}
	
	public ExplicitCoupledSolver() {
		this(GRID_DENSITY, TAU_FACTOR);
	}
	
	public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor) {
		super(GRID_DENSITY, TAU_FACTOR);
	}
	
	public ExplicitCoupledSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public void prepare(Problem problem) {
		super.prepare(problem);	
		
		if(rte == null)
			initRTE( (ParticipatingMedium) problem, grid);
		
		curve = problem.getHeatingCurve();
		
		N	= (int)grid.getGridDensity().getValue();
		hx	= grid.getXStep();
		tau	= grid.getTimeStep();
		
		U		= new double[N + 1];
		V		= new double[N + 1];
		
		Bi1 = (double) problem.getFrontHeatLoss().getValue();
		Bi2 = (double) problem.getHeatLossRear().getValue();
		maxTemp = (double) problem.getMaximumTemperature().getValue(); 
				
		counts = (int) curve.getNumPoints().getValue();
		
		a = 1./(1. + Bi1*hx);
		b = 1./(1. + Bi2*hx);
		
		if(problem instanceof ParticipatingMedium) {
		
			var p = (ParticipatingMedium)problem;
			rte.init(p, grid);
			opticalThickness = (double)p.getOpticalThickness().getValue();
			Np = (double)p.getPlanckNumber().getValue();
			
		}
		
	}
	
	public void solveGeneral(ParticipatingMedium problem) {
		prepare(problem);
		
		int i, m, w;
		double pls;
		final double TAU_HH = tau/pow(hx,2);	
		final double HX_NP = hx/Np; 
		
		final double prefactor = tau*opticalThickness/Np;
		
		double V_0, V_N;
				
		final double errorSq  = pow( nonlinearPrecision, 2);
		
		double wFactor = timeInterval * tau * problem.timeFactor();
		
		rte.compute(U);
		
		/*
		 * The outer cycle iterates over the number of points of the HeatingCurve
		 */
		
		for (w = 1; w < counts; w++) {
			
			/*
			 * Two adjacent points of the heating curves are 
			 * separated by timeInterval on the time grid. Thus, to calculate
			 * the next point on the heating curve, timeInterval/tau time steps
			 * have to be made first.
			 */
			
			for (m = (w - 1)*timeInterval + 1; m < w*timeInterval + 1; m++) {
				
				/*
				 * Do the iterations
				 */
				
				/*
				 * Temperature at boundaries will strongly change the radiosities.
				 * This recalculates the latter using the solution at previous iteration
				 */
				
				for( V_0 = Double.POSITIVE_INFINITY, V_N = Double.POSITIVE_INFINITY; 
					   (pow((V[0] - V_0), 2) > errorSq) ||
					   (pow((V[N] - V_N), 2) > errorSq)
					 ; rte.compute(V) ) {
				
				/*
				 * Uses the heat equation explicitly to calculate the 
				 * grid-function everywhere except the boundaries
				 */
				
				for(i = 1; i < N; i++) 
					V[i] =	U[i] +  TAU_HH*( U[i+1] - 2.*U[i] + U[i-1] )
							+ prefactor*rte.getFluxDerivative(i);
				
				/*
				 * Calculates boundary values
				 */
				
				pls  = discretePulse.evaluateAt( (m - EPS)*tau );
					
					// Front face
					V_0 = V[0];
					V[0] = ( V[1] + hx*pls 
							 - HX_NP*rte.getFlux(0) )*a;
					// Rear face
					V_N = V[N];
					V[N] = ( V[N-1] 
							 + HX_NP*rte.getFlux(N) )*b;
					
			    }	
				
				System.arraycopy(V, 0, U, 0, N + 1);
							
			}
			
			curve.addPoint( w * wFactor, V[N] );
			
		}			
		
		curve.scale( maxTemp/curve.apparentMaximum() );
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ExplicitCoupledSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}

	@Override
	public Class<? extends Problem> domain() {
		return ParticipatingMedium.class;
	}

	@Override
	public void solve(ParticipatingMedium problem) {
		switch(mode) {
		case GENERAL : solveGeneral(problem); break;
//		case OPTICALLY_THIN : solveThin(problem); break;
//		case OPTICALLY_THICK : solveRosseland(problem); break;
		default : throw new IllegalStateException("Mode not recognised: " + mode);
		}
	}

	public enum Mode {
		GENERAL, OPTICALLY_THIN, OPTICALLY_THICK;
	}

	public RadiativeTransferSolver getRadiativeTransferEquation() {
		return rte;
	}
		
	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.NONLINEAR_PRECISION));
		list.add(instanceDescriptor);
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case NONLINEAR_PRECISION : setNonlinearPrecision(property); break;
		default : throw new IllegalArgumentException("Property not recognised: " + property);
		}
	}
	
	public void initRTE(ParticipatingMedium problem, Grid grid) {
		rte = instanceDescriptor.newInstance(RadiativeTransferSolver.class, problem, grid);
		rte.setParent(this);
	}
	
	public static InstanceDescriptor<? extends RadiativeTransferSolver> getInstanceDescriptor() {
		return instanceDescriptor;
	}
	
	public static void setInstanceDescriptor( InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor) {
		ExplicitCoupledSolver.instanceDescriptor = instanceDescriptor;
	}
	
}