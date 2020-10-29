package pulse.search.direction;

import static pulse.math.linear.SquareMatrix.asSquareMatrix;
import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.DAMPING_RATIO;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.math.linear.Matrices;
import pulse.math.linear.RectangularMatrix;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.statistics.OptimiserStatistic;
import pulse.search.statistics.ResidualStatistic;
import pulse.search.statistics.SumOfSquares;
import pulse.tasks.SearchTask;
import pulse.tasks.logs.Status;
import pulse.ui.Messages;

public class LMOptimiser extends PathOptimiser {

	private static LMOptimiser instance = new LMOptimiser();
	private boolean computeJacobian;

	private final static double EPS = 1e-10; // for numerical comparison
	private double dampingRatio;
	
	private double geodesicParameter = 0.1;
	private boolean geodesicCorrection = false;

	private LMOptimiser() {
		super();
		dampingRatio = (double)def(DAMPING_RATIO).getValue();
		this.setSolver(new HessianDirectionSolver() {
			// see default implementation
		});
	}

	@Override
	public void reset() {
		super.reset();
		computeJacobian = true;
	}

	@Override
	public boolean iteration(SearchTask task) throws SolverException {
		var p = (LMPath) task.getPath(); // the previous path of the task

		/*
		 * Checks whether an iteration limit has been already reached
		 */

		if (compare(p.getIteration(), getMaxIterations()) > 0) {

			task.setStatus(Status.TIMEOUT);
			return true;

		}

		else {

			double initialCost	= task.solveProblemAndCalculateCost();
			var parameters		= task.searchVector()[0];
			p.setParameters(parameters); // store current parameters

			prepare(task); // do the preparatory step

			var lmDirection = getSolver().direction(p);
			
			/*
			 * Geodesic acceleration
			 */
			
			var acceleration = p.getJacobian().transpose().multiply( directionalDerivative(task) ); // J' dr/dp
			var correction = HessianDirectionSolver.solve(p, acceleration.inverted() ); // H^-1 J'dr/dp

			double newCost = Double.POSITIVE_INFINITY;
						
			/*
			 *  Additional conditions imposed by geodesic acceleration.
			 */
			
			if( !geodesicCorrection || correction.length() / lmDirection.length() <= geodesicParameter) {
				var candidate = parameters.sum(lmDirection);
				task.assign(new IndexedVector( 
						geodesicCorrection ? candidate.sum( correction.multiply(0.5) ) : candidate, 
						parameters.getIndices() ) ); // assign new parameters
				newCost = task.solveProblemAndCalculateCost(); // calculate the sum of squared residuals
			}

			/*
			 * Delayed gratification
			 */

			if (newCost > initialCost + EPS) {
				p.setLambda(p.getLambda() * 2.0);
				task.assign(parameters); // roll back if cost increased
				computeJacobian = true;
			} else {
				p.setLambda(p.getLambda() / 3.0);
				computeJacobian = false;
				p.incrementStep(); // increment the counter of successful steps
			}

			return newCost < initialCost + EPS;

		}

	}
	

	@Override
	public void prepare(SearchTask task) throws SolverException {
		var p = (LMPath) task.getPath();
		
		//store residual vector at current parameters
		p.setResidualVector( new Vector( residualVector(task.getCurrentCalculation().getOptimiserStatistic())  ));

		// Calculate the Jacobian -- if needed
		if (computeJacobian) {
			p.setJacobian(jacobian(task)); // J
			p.setNonregularisedHessian(halfHessian(p)); // this is just J'J
		}

		// the Jacobian is then used to calculate the 'gradient'
		Vector g1 = halfGradient(p); // g1
		p.setGradient(g1);

		// the Hessian is then regularised by adding labmda*I

		var hessian = p.getNonregularisedHessian();
		var damping = 	( levenbergDamping(hessian).multiply(dampingRatio)
					 	.sum(marquardtDamping(hessian).multiply(1.0 - dampingRatio)) 
					 	)
					 	.multiply(p.getLambda());
		var regularisedHessian = asSquareMatrix(hessian.sum(damping)); // J'J + lambda I

		p.setHessian(regularisedHessian); // so this is the new Hessian
	}
	
	
	public RectangularMatrix jacobian(SearchTask task) throws SolverException {

		var residualCalculator = task.getCurrentCalculation().getOptimiserStatistic();

		var p = ((LMPath) task.getPath());

		final var params = p.getParameters();
		final var indices = params.getIndices();
		
		final int numPoints = p.getResidualVector().dimension();
		final int numParams = params.dimension();

		var jacobian = new double[numPoints][numParams];

		final double dx = super.getGradientStep();
		
		for (int i = 0; i < numParams; i++) {

			final var shift = new Vector(numParams);
			shift.set(i, 0.5 * dx);

			// + shift
			task.assign(new IndexedVector(params.sum(shift), indices));
			task.solveProblemAndCalculateCost();
			var r1 = residualVector(residualCalculator);

			// - shift
			task.assign(new IndexedVector(params.subtract(shift), indices));
			task.solveProblemAndCalculateCost();
			var r2 = residualVector(residualCalculator);

			for (int j = 0; j < numPoints; j++) {

				jacobian[j][i] = (r1[j] - r2[j]) / dx;

			}

		}

		// revert to original params
		task.assign(params);

		return Matrices.createMatrix(jacobian);

	}

	private static double[] residualVector(ResidualStatistic rs) {
		return rs.getResiduals().stream().mapToDouble(array -> array[1]).toArray();
	}

	@Override
	public Path createPath(SearchTask t) {
		this.configure(t);
		computeJacobian = true;
		return new LMPath(t);
	}

	private Vector halfGradient(LMPath path) {
		var jacobian = path.getJacobian();
		var residuals = path.getResidualVector();
		return jacobian.transpose().multiply(new Vector(residuals));
	}

	private SquareMatrix halfHessian(LMPath path) {
		var jacobian = path.getJacobian();
		return asSquareMatrix(jacobian.transpose().multiply(jacobian));
	}

	private Vector directionalDerivative(SearchTask t) throws SolverException {
		var p = (LMPath) t.getPath();

		var currentParameters	= p.getParameters();
		final int numParams		= currentParameters.dimension();

		final var dir	= p.getDirection();
		var shift		= new Vector(numParams);
	
		final double h = 0.5*super.getGradientStep();
		
		//small shift in a previously calculated direction
		for(int i = 0; i < numParams; i++)
			shift.set(i, h * dir.get(i) );

		final var statistic = t.getCurrentCalculation().getOptimiserStatistic();
		
		var currentResiduals = p.getResidualVector();
		
		t.assign( new IndexedVector( currentParameters.sum(shift), currentParameters.getIndices() ) );
		t.solveProblemAndCalculateCost();
		var newResiduals = residualVector(statistic);

		t.assign(currentParameters); //shift back
		
		var diff = new double[newResiduals.length];
		var jacobian = p.getJacobian();
		
		for(int i = 0 ; i < newResiduals.length; i++) {
			diff[i] = ( newResiduals[i] - currentResiduals.get(i) ) / h;
		
			double add = 0;
			for(int j = 0; j < numParams; j++)
				add += jacobian.get(i, j)*dir.get(j);
			
			diff[i] -= add;
			diff[i] *= 2.0/h;
		}
		
		return new Vector(diff);
	}
	
	/*
	 * Additive damping strategy, where the scaling matrix is simply the identity matrix.
	 */

	private SquareMatrix levenbergDamping(SquareMatrix hessian) {
		return Matrices.createIdentityMatrix(hessian.getData().length);
	}
	
	/*
	 * Multiplicative damping strategy, where the scaling matrix is equal to the 'hessian' block-diagonal matrix.
	 * Works best for badly scaled problems. However, this is also scale-invariant, 
	 * which mean it increases the susceptibility to parameter evaporation.
	 */

	private SquareMatrix marquardtDamping(SquareMatrix hessian) {
		return hessian.blockDiagonal();
	}
	
	@Override
	public List<Property> listedTypes() {
		var list = super.listedTypes();
		list.add(def(DAMPING_RATIO));
		return list;
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static LMOptimiser getInstance() {
		return instance;
	}

	@Override
	public String toString() {
		return Messages.getString("LMOptimiser.Descriptor");
	}
	
	/**
	 * The Levenberg-Marquardt optimiser will only accept ordinary least-squares as
	 * its objective function. Therefore, {@code os} should be an instance of
	 * {@code SumOfSquares}.
	 * 
	 * @return {@code true} if {@code.getClass()} returns
	 *         {@code SumOfSquares.class}, {@code false} otherwise
	 */

	@Override
	public boolean compatibleWith(OptimiserStatistic os) {
		return os.getClass().equals(SumOfSquares.class);
	}

	public NumericProperty getDampingRatio() {
		return derive(DAMPING_RATIO, dampingRatio);
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		if(type == DAMPING_RATIO)
			setDampingRatio(property);
	}

	public void setDampingRatio(NumericProperty dampingRatio) {
		requireType(dampingRatio, DAMPING_RATIO);
		this.dampingRatio = (double)dampingRatio.getValue();
		firePropertyChanged(this, dampingRatio);
	}
	

	/*
	 * Path
	 */

	class LMPath extends ComplexPath {

		private IndexedVector parameters;
		private Vector residualVector;
		private RectangularMatrix jacobian;
		private SquareMatrix nonregularisedHessian;
		private double lambda;

		public LMPath(SearchTask t) {
			super(t);
		}

		public RectangularMatrix getJacobian() {
			return jacobian;
		}

		public void setJacobian(RectangularMatrix jacobian) {
			this.jacobian = jacobian;
		}

		public double getLambda() {
			return lambda;
		}

		public void setLambda(double lambda) {
			this.lambda = lambda;
		}

		@Override
		public void configure(SearchTask t) {
			super.configure(t);
			this.jacobian = null;
			this.setHessian(null);
			nonregularisedHessian = null;
			this.lambda = 1.0;
			this.residualVector = null;
		}

		public SquareMatrix getNonregularisedHessian() {
			return nonregularisedHessian;
		}

		public void setNonregularisedHessian(SquareMatrix nonregularisedHessian) {
			this.nonregularisedHessian = nonregularisedHessian;
		}

		public Vector getResidualVector() {
			return residualVector;
		}

		public void setResidualVector(Vector residualVector) {
			this.residualVector = residualVector;
		}

		public IndexedVector getParameters() {
			return parameters;
		}

		public void setParameters(IndexedVector parameters) {
			this.parameters = parameters;
		}

	}


}