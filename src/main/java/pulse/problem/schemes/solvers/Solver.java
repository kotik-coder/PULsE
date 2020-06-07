package pulse.problem.schemes.solvers;

import pulse.problem.statements.Problem;

/**
 * A solver interface which provides the capability to use the {@code solve}
 * method on a {@code Problem}
 * 
 * @param <T> an instance of Problem
 */

public interface Solver<T extends Problem> {

	/**
	 * Calculates the solution of the {@code t} and stores it in the respective
	 * {@code HeatingCurve}.
	 * 
	 * @param problem - an accepted instance of {@code T}
	 */

	public void solve(T problem);

}