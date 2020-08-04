package pulse.math.linear;

/**
 * A basic wrapper interface for binary arithmetic operations.
 *
 */

interface ArithmeticOperation {

	/**
	 * Calculates the result of the binary operation.
	 * @param x a number
	 * @param y another number
	 * @return the result of arithmetic operation
	 */
	
	double evaluate(double x, double y);
	
}