package pulse.search.direction;

import static java.lang.Math.abs;
import static pulse.math.linear.SquareMatrix.asSquareMatrix;
import static pulse.math.linear.SquareMatrix.outerProduct;

import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

public class SR1Optimiser extends CompositePathOptimiser {

	private static SR1Optimiser instance = new SR1Optimiser();
	
	private final static double r = 1E-8;

	private SR1Optimiser() {
		super();
		this.setSolver(path -> ((ComplexPath)path).getInverseHessian().multiply(path.getGradient().inverted()));
	}

	/**
	 * <p>
	 * Calculated the gradient at the end of this step. Invokes {@code hessian(...)}
	 * to calculate the Hessian matrix at the {@code <i>k</i>+1} step using the
	 * <math><i>g</i><sub><i>k</i></sub></math> and
	 * <math><i>g</i><sub><i>k</i>+1</sub></math> gradient values, the previously
	 * calculated Hessian matrix on step <i>k</i>, and the result of the linear
	 * search <i>&alpha;<sub>k</i>+1</sub>.
	 * </p>
	 * 
	 * @throws SolverException
	 */

	@Override
	public void prepare(SearchTask task) throws SolverException {
		var p = (ComplexPath) task.getPath();
		Vector dir = p.getDirection();

		final double minimumPoint = p.getMinimumPoint();
		final Vector g0 = p.getGradient();	// g0
		final Vector g1 = gradient(task);	// g1

		/*
		 * Evaluate condition and update if needed		
		 */
		
		Vector y = g1.subtract(g0); // g[k+1] - g[k]

		final var dx = dir.multiply(minimumPoint);
		final var m1 = y.subtract(p.getHessian().multiply(dx));

		if(abs(dx.dot(m1)) > r*dx.length()*m1.length() ) {
		
			var m = p.getHessian().sum((outerProduct(m1, m1)).multiply(1. / m1.dot(dx)));
			p.setHessian( asSquareMatrix(m) );		
			p.setInverseHessian( inverseHessian(g0, g1, dir, p.getInverseHessian(), minimumPoint) );
			
		}
		
		p.setGradient(g1); // set g1 as the new gradient for next step
			
	}
	
	private SquareMatrix inverseHessian(Vector g1, Vector g2, Vector dir, SquareMatrix prevInvHessian, double alpha) {
		Vector y = g2.subtract(g1); // g[k+1] - g[k]

		final var dx = dir.multiply(alpha);
		final var m1 = dx.subtract(prevInvHessian.multiply(y));
		
		var m = prevInvHessian.sum((outerProduct(m1, m1)).multiply(1. / m1.dot(y))); //SR1 formula
		return asSquareMatrix(m);
	}

	@Override
	public String toString() {
		return Messages.getString("SR1.Descriptor");
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static SR1Optimiser getInstance() {
		return instance;
	}
	
}