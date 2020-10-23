package pulse.search.linear;

import pulse.math.IndexedVector;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The golden-section search is a simple dichotomy search for finding the
 * minimum of strictly unimodal functions by successively narrowing the domain
 * of the search using the golden ratio partitioning.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Golden-section_search">Wikipedia
 *      page</a>
 */

public class GoldenSectionOptimiser extends LinearOptimiser {

	/**
	 * The golden section &phi;, which is approximately equal to 0.618033989.
	 */

	public final static double PHI = 1.0 - (3.0 - Math.sqrt(5.0)) / 2.0;

	private static GoldenSectionOptimiser instance = new GoldenSectionOptimiser();

	private GoldenSectionOptimiser() {
		super();
	}

	/**
	 * <p>
	 * Let {@code a} and {@code b} be the start and end point of a {@code Segment},
	 * initially defined by the {@code super.domain(IndexedVector,Vector)} method.
	 * This method will start a loop, which at each step <i>i</i> will compare the
	 * values of the target function at the end points of a {@code Segment}
	 * constructed from one of the end points <i>a<sub>i</sub></i> or
	 * <i>b<sub>i</sub></i> and substituting the second end point with either
	 * <math><i>a<sub>i</sub> + &phi;*(b-a)</i></math> or <math><i>b<sub>i</sub> -
	 * &phi;*(b-a)</i></math>. This theoretically ensures the least number of steps
	 * to reach the minimum (as compared to the standard dichotomy methods).
	 * </p>
	 * 
	 * @throws SolverException
	 */

	@Override
	public double linearStep(SearchTask task) throws SolverException {

		final double EPS = 1e-14;

		final var params = task.searchVector();
		final Vector direction = task.getPath().getDirection();

		var segment = domain(params[0], params[1], direction);

		final double absError = searchResolution * PHI * segment.length();

		for (double t = PHI * segment.length(); Math.abs(t) > absError; t = PHI * segment.length()) {
			final double alpha = segment.getMinimum() + t;
			final double one_minus_alpha = segment.getMaximum() - t;

			final var newParams1 = params[0].sum(direction.multiply(alpha)); // alpha
			task.assign(new IndexedVector(newParams1, params[0].getIndices()));
			final double ss2 = task.solveProblemAndCalculateCost(); // f(alpha)

			final var newParams2 = params[0].sum(direction.multiply(one_minus_alpha)); // 1 - alpha
			task.assign(new IndexedVector(newParams2, params[0].getIndices()));
			final double ss1 = task.solveProblemAndCalculateCost(); // f(1-alpha)

			task.assign(new IndexedVector(newParams2, params[0].getIndices())); // return to old position

			if (ss2 - ss1 > EPS)
				segment.setMaximum(alpha);
			else
				segment.setMinimum(one_minus_alpha);

		}

		return segment.mean();

	}

	@Override
	public String toString() {
		return Messages.getString("GoldenSectionSolver.Descriptor");
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static GoldenSectionOptimiser getInstance() {
		return instance;
	}

}