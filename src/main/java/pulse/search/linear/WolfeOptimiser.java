package pulse.search.linear;

import static java.lang.Math.abs;

import pulse.math.IndexedVector;
import pulse.math.Segment;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.search.direction.Path;
import pulse.search.direction.PathOptimiser;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * <p>
 * This is the implementation of the strong Wolfe conditions for performing
 * inexact linear search. This type of linear search works best with the
 * {@code ApproximatedHessianSolver}.
 * </p>
 * 
 * @see pulse.search.direction.ApproximatedHessianOptimiser
 * @see <a href="https://en.wikipedia.org/wiki/Wolfe_conditions">Wikipedia
 *      page</a>
 */

public class WolfeOptimiser extends LinearOptimiser {

	private static WolfeOptimiser instance = new WolfeOptimiser();

	/**
	 * The constant used in the Armijo inequality, equal to {@value C1}.
	 */

	public final static double C1 = 0.05;

	/**
	 * The constant used in the strong Wolfe inequality for the modulus of the
	 * gradient projection, equal to {@value C2}.
	 */

	public final static double C2 = 0.8;

	private WolfeOptimiser() {
		super();
	}

	/**
	 * <p>
	 * This uses a combination of the Wolfe conditions for conducting an inexact
	 * line search with the domain partitioning using a random number generator. The
	 * partitioning is done in such a way that: (a) whenever the Armijo inequality
	 * is not satisfied, the original domain {@code [a; b]} is reduced to
	 * <math>[<i>a</i><sub>i</sub>; &alpha;]</i>, where &alpha; is the random number
	 * confined inside [<i>a</i><sub>i</sub>; <i>b</i><sub>i</sub>]; (b) when the
	 * Armijo inequality is satisfied and the second (strong) Wolfe condition for
	 * the modulus of the gradient projection is not satisfied, the &alpha; value is
	 * used to substitute the lower end point for the search domain: [&alpha;;
	 * <i>b</i><sub>i</sub>]. As this is done iteratively, the length of the
	 * associated {@code Segment} will decrease. The method will return a value if
	 * either the strong Wolfe conditions are strictly satisfied, or if the linear
	 * precision has been reached.
	 * </p>
	 * 
	 * @throws SolverException
	 */

	@Override
	public double linearStep(SearchTask task) throws SolverException {

		Path p = task.getPath();

		final Vector direction = p.getDirection();
		final Vector g1 = p.getGradient();

		final double G1P = g1.dot(direction);
		final double G1P_ABS = abs(G1P);

		IndexedVector[] params = task.searchVector();
		Segment segment = domain(params[0], params[1], direction);

		double ss1 = task.solveProblemAndCalculateDeviation();
		double ss2;

		double randomConfinedValue = 0;
		Vector g2, newParams;
		double g2p;

		for (double initialLength = segment.length(); segment.length() / initialLength > searchResolution;) {

			randomConfinedValue = segment.randomValue();

			newParams = params[0].sum(direction.multiply(randomConfinedValue));
			task.assign(new IndexedVector(newParams, params[0].getIndices()));

			ss2 = task.solveProblemAndCalculateDeviation();

			/**
			 * Checks if the first Armijo inequality is not satisfied. In this case, it will
			 * set the maximum of the search domain to the {@code randomConfinedValue}.
			 */

			if (ss2 - ss1 > C1 * randomConfinedValue * G1P) {
				segment.setMaximum(randomConfinedValue);
				continue;
			}

			g2 = PathOptimiser.gradient(task);
			g2p = g2.dot(direction);

			/**
			 * This is the strong Wolfe condition that ensures that the absolute value of
			 * the projection of the gradient decreases.
			 */

			if (abs(g2p) <= C2 * G1P_ABS)
				break;

			/*
			 * if( g2p >= C2*G1P ) break;
			 */

			segment.setMinimum(randomConfinedValue);

		}

		task.assign(params[0]);
		p.setGradient(g1);

		return randomConfinedValue;

	}

	@Override
	public String toString() {
		return Messages.getString("WolfeSolver.Descriptor"); //$NON-NLS-1$
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static WolfeOptimiser getInstance() {
		return instance;
	}

}