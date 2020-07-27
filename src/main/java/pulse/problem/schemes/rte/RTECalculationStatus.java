package pulse.problem.schemes.rte;

/**
 * A measure of health for radiative transfer calculations.
 *
 */

public enum RTECalculationStatus {
	
	/**
	 * The current calculation step finished normally.
	 */
	
	NORMAL, 
	
	/**
	 * The integrator took too long to finish.
	 */
	
	INTEGRATOR_TIMEOUT, 
	
	/**
	 * The iterative solver took too long to finish.
	 */
	
	ITERATION_TIMEOUT, 
	
	/**
	 * The grid density required to reach the error threshold was too large.
	 */
	
	GRID_TOO_LARGE;
}