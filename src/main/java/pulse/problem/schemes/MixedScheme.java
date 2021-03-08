package pulse.problem.schemes;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.properties.NumericProperty;

/**
 * An abstraction describing a weighted semi-implicit finite-difference scheme
 * for solving the one-dimensional heat conduction problem.
 * 
 * @see pulse.problem.statements.ClassicalProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public abstract class MixedScheme extends ImplicitScheme {

	/**
	 * Constructs a default semi-implicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public MixedScheme() {
		this(derive(GRID_DENSITY, 30), derive(TAU_FACTOR, 1.0));
	}

	/**
	 * Constructs a semi-implicit scheme on a one-dimensional grid that is specified
	 * by the values {@code N} and {@code timeFactor}.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 */

	public MixedScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	/**
	 * <p>
	 * Constructs a semi-implicit scheme on a one-dimensional grid that is specified
	 * by the values {@code N} and {@code timeFactor}. Sets the time limit of this
	 * scheme to {@code timeLimit}
	 * 
	 * @param N          the {@code NumericProperty} with the type
	 *                   {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type
	 *                   {@code TAU_FACTOR}
	 * @param timeLimit  the {@code NumericProperty} with the type
	 *                   {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */

	public MixedScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}
	
	/**
	 * Prints out the description of this problem type.
	 * 
	 * @return a verbose description of the problem.
	 */

	@Override
	public String toString() {
		return getString("MixedScheme.4");
	}

}