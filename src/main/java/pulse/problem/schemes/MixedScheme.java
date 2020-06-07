package pulse.problem.schemes;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * This class implements a symmetric (weight <math> = 0.5</math>) second-order
 * in time Crank-Nicolson semi-implicit finite-difference scheme for solving the
 * one-dimensional heat conduction problem.
 * <p>
 * The semi-implicit scheme uses a 6-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step:
 * <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m</sub>),
 * &Theta;(x<sub>i-1</sub>,t<sub>m+1</sub>),
 * &Theta;(x<sub>i+1</sub>,t<sub>m+1</sub>)</i></math>. The boundary conditions
 * are approximated with a Taylor expansion up to the third term, hence the
 * scheme has an increased order of approximation.
 * </p>
 * <p>
 * The semi-implicit scheme is unconditionally stable and has an order of
 * approximation of <math><i>O(&tau;<sup>2</sup> + h<sup>2</sup>)</i></math>.
 * Note this scheme is prone to spurious oscillations when either a high spatial
 * resolution or a large timestep are used. It has been noticed that due to the
 * pulse term in the boundary condition, a higher error is introduced into the
 * calculation than for the implicit scheme.
 * </p>
 * 
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public abstract class MixedScheme extends DifferenceScheme {

	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.25} for this
	 * scheme.
	 */

	public final static NumericProperty TAU_FACTOR = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 1.0);

	/**
	 * The default value of {@code gridDensity}, which is set to {@code 30} for this
	 * scheme.
	 */

	public final static NumericProperty GRID_DENSITY = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);

	/**
	 * Constructs a default semi-implicit scheme using the default values of
	 * {@code GRID_DENSITY} and {@code TAU_FACTOR}.
	 */

	public MixedScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
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
		super();
		initGrid(N, timeFactor);
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
		super(timeLimit);
		initGrid(N, timeFactor);
	}

	@Override
	public String toString() {
		return Messages.getString("MixedScheme.4");
	}

}