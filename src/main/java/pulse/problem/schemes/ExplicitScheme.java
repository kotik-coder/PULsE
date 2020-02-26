package pulse.problem.schemes;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * This class implements the simple explicit finite-difference scheme 
 * (also called the forward-time centered space scheme) for solving 
 * the one-dimensional heat conduction problem. 
 * <p>The explicit scheme uses a standard 4-point template on a one-dimensional grid
 * that utilises the following grid-function values on each step: <math><i>&Theta;(x<sub>i</sub>,t<sub>m</sub>), 
 * &Theta;(x<sub>i</sub>,t<sub>m+1</sub>), 
 * &Theta;(x<sub>i-1</sub>,t<sub>m</sub>), &Theta;(x<sub>i+1</sub>,t<sub>m</sub>)</i></math>. 
 * Hence, the calculation of the grid-function at the timestep <math><i>m</i>+1</math> 
 * can be done <i>explicitly</i>. The derivative in the boundary conditions is approximated using a simple forward difference. 
 * </p> 
 * <p>The explicit scheme is stable only if <math><i>&tau; &le; h<sup>2</sup></i></math> and has an order of 
 * approximation of <math><i>O(&tau; + h)</i></math>. Note that this scheme is only used for validating more complex
 * schemes and does not give accurate results due to the lower order of approximation. When calculations using this 
 * scheme are performed, the <code>gridDensity</code> is chosen to be at least 80, which ensures that the error 
 * is not too high (typically a {@code 1.5E-2} relative error).</p>    
 * @see pulse.problem.statements.LinearisedProblem
 * @see pulse.problem.statements.NonlinearProblem
 *
 */

public abstract class ExplicitScheme extends DifferenceScheme {
	
	/**
	 * The default value of {@code tauFactor}, which is set to {@code 0.5} 
	 * for this scheme.
	 */
	
	public final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
	
	/**
	 * The default value of {@code gridDensity}, which is set to {@code 80} 
	 * for this scheme.
	 */
	
	public final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 80);

	/**
	 * Constructs a default explicit scheme using the default 
	 * values of {@code GRID_DENSITY} and {@code TAU_FACTOR}. 
	 */
	
	public ExplicitScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}	
	
	/**
	 * Constructs an explicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}.
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 */
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);	
		grid = new Grid(N, timeFactor);	
		grid.setParent(this);
	}
	
	/**
	 * <p>Constructs an explicit scheme on a one-dimensional grid
	 * that is specified by the values {@code N} and {@code timeFactor}. 
	 * Sets the time limit of this scheme to {@code timeLimit}  
	 * @param N the {@code NumericProperty} with the type {@code GRID_DENSITY}
	 * @param timeFactor the {@code NumericProperty} with the type {@code TAU_FACTOR}
	 * @param timeLimit the {@code NumericProperty} with the type {@code TIME_LIMIT}
	 * @see pulse.problem.schemes.DifferenceScheme
	 */
	
	public ExplicitScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}

	@Override
	public String toString() {
		return Messages.getString("ExplicitScheme.4");
	}

}