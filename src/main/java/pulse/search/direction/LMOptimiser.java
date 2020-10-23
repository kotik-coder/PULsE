package pulse.search.direction;

import static pulse.math.linear.SquareMatrix.asSquareMatrix;
import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.isDiscrete;

import pulse.math.IndexedVector;
import pulse.math.linear.Matrices;
import pulse.math.linear.RectangularMatrix;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.search.statistics.OptimiserStatistic;
import pulse.search.statistics.ResidualStatistic;
import pulse.search.statistics.SumOfSquares;
import pulse.tasks.SearchTask;
import pulse.tasks.logs.Status;
import pulse.ui.Messages;

public class LMOptimiser extends PathOptimiser {

	private static LMOptimiser instance = new LMOptimiser();
	private boolean computeJacobian;
	
	private final static double EPS = 1e-10; //for numerical comparison

	private LMOptimiser() {
		super();
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
			var parameters		= task.searchVector()[0]; // get current search vector
			
			prepare(task); // do the preparatory step
 
			var candidateParams = parameters.sum(getSolver().direction(p));
			task.assign(new IndexedVector(candidateParams, parameters.getIndices())); // assign new parameters to this task
			
			double newCost = task.solveProblemAndCalculateCost(); // calculate the sum of squared residuals
			
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

	public RectangularMatrix jacobian(SearchTask task) throws SolverException {

		var residualCalculator = task.getCurrentCalculation().getOptimiserStatistic();

		final var params = task.searchVector()[0];

		final int numPoints = residualCalculator.getResiduals().size();
		final int numParams = params.dimension();

		var jacobian = new double[numPoints][numParams];

		boolean discreteGradient = params.getIndices().stream().anyMatch(index -> isDiscrete(index));
		final double dxGrid = task.getCurrentCalculation().getScheme().getGrid().getXStep();
		final double dx = discreteGradient ? dxGrid : (double) getGradientResolution().getValue();

		for (int i = 0; i < numParams; i++) {

			final var shift = new Vector(numParams);
			shift.set(i, 0.5 * dx);

			// + shift
			task.assign(new IndexedVector(params.sum(shift), params.getIndices()));
			task.solveProblemAndCalculateCost();
			var r1 = residualVector(residualCalculator);

			// - shift
			task.assign(new IndexedVector(params.subtract(shift), params.getIndices()));
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
		computeJacobian = true;
		return new LMPath(t);
	}

	private Vector halfGradient(SearchTask task) {
		var jacobian = ((LMPath) task.getPath()).getJacobian();
		var residuals = residualVector(task.getCurrentCalculation().getOptimiserStatistic());
		return jacobian.transpose().multiply(new Vector(residuals));
	}

	private SquareMatrix halfHessian(SearchTask task) {
		var jacobian = ((LMPath) task.getPath()).getJacobian();
		return asSquareMatrix(jacobian.transpose().multiply(jacobian));
	}

	@Override
	public void prepare(SearchTask task) throws SolverException {
		var p = (LMPath) task.getPath();

		// Calculate the Jacobian -- if needed
		if (computeJacobian) {
			p.setJacobian(jacobian(task)); //J
			p.setNonregularisedHessian(halfHessian(task)); //this is just J'J
		}

		// the Jacobian is then used to calculate the 'gradient'
		Vector g1 = halfGradient(task); // g1
		p.setGradient(g1);
		
		// the Hessian is then regularised by adding labmda*I

		var hessian = p.getNonregularisedHessian();
		var lambdaI = Matrices.createIdentityMatrix(hessian.getData().length).multiply(p.getLambda());
		var regularisedHessian = asSquareMatrix( hessian.sum( lambdaI ) ); //J'J + lambda I 
		
		p.setHessian(regularisedHessian); //so this is the new Hessian
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

	/*
	 * Path
	 */

	class LMPath extends ComplexPath {

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
		}

		public SquareMatrix getNonregularisedHessian() {
			return nonregularisedHessian;
		}

		public void setNonregularisedHessian(SquareMatrix nonregularisedHessian) {
			this.nonregularisedHessian = nonregularisedHessian;
		}

	}
	
	/**
	 * The Levenberg-Marquardt optimiser will only accept ordinary least-squares 
	 * as its objective function. Therefore, {@code os} should be an instance of
	 * {@code SumOfSquares}.
	 * @return {@code true} if {@code.getClass()} returns {@code SumOfSquares.class}, {@code false} otherwise
	 */
	
	@Override
	public boolean compatibleWith(OptimiserStatistic os) {
		return os.getClass().equals(SumOfSquares.class);
	}

}