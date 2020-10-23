package pulse.search.direction;

import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;

public interface DirectionSolver {

	/**
	 * Finds the direction of the minimum using the previously calculated values
	 * stored in {@code p}.
	 * 
	 * @param p a {@code Path} object
	 * @return a {@code Vector} pointing to the minimum direction for this
	 *         {@code Path}
	 * @throws SolverException 
	 * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
	 */

	public Vector direction(Path p) throws SolverException;
	
}