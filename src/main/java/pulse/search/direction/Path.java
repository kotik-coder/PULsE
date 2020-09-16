package pulse.search.direction;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.ITERATION;

import pulse.math.linear.Vector;
import pulse.properties.NumericProperty;
import pulse.tasks.SearchTask;

/**
 * <p>
 * A {@code Path} stores information relevant to the selected
 * {@code PathSolver}, which is related to a specific {@code SearchTask}. This
 * information is used by the {@code PathSolver} to perform the next search
 * step.
 * </p>
 * 
 * <p>
 * This is the most basic implementation, which stores only the gradient, the
 * direction (equal to the inverse gradient), minimum point achieved by the
 * linear solver, and the number of current iteration. It is used in combination
 * with the {@code SteepestDescentSolver}. Note the constructors for
 * {@code Path} are protected, as they should not be invoked directly. Instead,
 * they are invoked from within the {@code PathSolver}.
 * </p>
 * 
 *
 */

public class Path {

	private Vector direction;
	private Vector gradient;
	private double minimumPoint;
	private int iteration;

	protected Path(SearchTask t) {
		reset(t);
	}

	/**
	 * Resets the {@code Path}: calculates the current gradient and the direction of
	 * search. Sets the minimum point to 0.0.
	 * 
	 * @param t the {@code SearchTask}, for which this {@code Path} is created.
	 * @see pulse.search.direction.PathSolver.direction(Path)
	 */

	public void reset(SearchTask t) {
		this.gradient = PathOptimiser.gradient(t);
		this.direction = PathOptimiser.getInstance().direction(this);
		minimumPoint = 0.0;
	}

	public Vector getDirection() {
		return direction;
	}

	public void setDirection(Vector currentDirection) {
		this.direction = currentDirection;
	}

	public Vector getGradient() {
		return gradient;
	}

	public void setGradient(Vector currentGradient) {
		this.gradient = currentGradient;
	}

	public double getMinimumPoint() {
		return minimumPoint;
	}

	public void setLinearStep(double min) {
		minimumPoint = min;
	}

	public NumericProperty getIteration() {
		return derive(ITERATION, iteration);
	}

	public void incrementStep() {
		iteration++;
	}

}