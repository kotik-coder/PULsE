package pulse.math;

import static java.lang.Double.doubleToLongBits;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Math.log;

/**
 * A utility class containing some common methods potentially enhancing the
 * performance of standard mathematical operations. Largely based on
 * https://martin.ankerl.com
 *
 */

public class MathUtils {

	public final static double EQUALS_TOLERANCE = 1E-5;
	
	private MathUtils() {
		// intentionally blank
	}
	
	/**
	 * Checks if two numbers are approximately equal by comparing the modulus 
	 * of their difference to {@value EQUALS_TOLERANCE}. 
	 * @param a a number
	 * @param b another number
	 * @return {@code true} if numbers are approximately equal, {@code false} otherwise
	 */
	
	public static boolean approximatelyEquals(final double a, final double b) {
		return Math.abs(a - b) < EQUALS_TOLERANCE;
	}

	/**
	 * A method for the approximate calculation of the hyperbolic tangent.
	 * 
	 * @param a the argument of {@code tanh(a)}.
	 * @return the approximate {@code tanh(a)} value.
	 * @see MathUtils.fastExp(double)
	 */

	public static double fastTanh(final double a) {
		final double e2x = fastExp(2.0 * a);
		return (e2x - 1.0) / (e2x + 1.0);
	}

	/**
	 * Calculate the hyperbolic arctangent (exact).
	 * 
	 * @param a the argument of {@code atanh(a)}
	 * @return the exact value of the {@code atanh(a)}.
	 */

	public static double atanh(final double a) {
		return 0.5 * log((1 + a) / (1 - a));
	}

	/**
	 * Rapid calculation of {@code b}-th power of {@code a}, which is simply a
	 * repeated multiplication, in case of positive {@code b}, or a repeated
	 * division.
	 * 
	 * @param a the base .
	 * @param b the exponent.
	 * @return the exponentiation result.
	 */

	public static double fastPowLoop(final double a, final int b) {
		double re = 1;
		// if positive (i < b is never satisfied for negative b's)
		for (int i = 0; i < b; i++) {
			re *= a;
		}
		// if negative
		for (int i = 0; i < -b; i++) {
			re /= a;
		}
		return re;
	}

	/**
	 * Rapid calculation of (-1)<sup>n</sup> using a ternary conditional operator.
	 * 
	 * @param n a positive integer number
	 * @return the result of exponentiation.
	 */

	public static int fastPowSgn(int n) {
		return n % 2 == 0 ? 1 : -1;
	}

	/**
	 * Rapid exponentiation where the base and the exponent are integer value. Uses
	 * bitwise shiftiing.
	 * 
	 * @param x the base
	 * @param y the exponent
	 * @return result of the exponentiation
	 */

	public static long fastPowInt(long x, int y) {
		long result = 1;
		while (y > 0) {
			if ((y & 1) == 0) {
				x *= x;
				y >>>= 1;
			} else {
				result *= x;
				y--;
			}
		}
		return result;
	}

	/**
	 * Approximate calculation of {@code exp(val)}.
	 * 
	 * @param val the argument of the exponent.
	 * @return the result.
	 */

	public static double fastExp(double val) {
		final long tmp = (long) (1512775 * val + 1072632447);
		return longBitsToDouble(tmp << 32);
	}

	/**
	 * A highly-approximate calculation of {@code ln(val)}.
	 * 
	 * @param val the argument of the natural logarithm.
	 * @return the result.
	 */

	public static double fastLog(double val) {
		final double x = (doubleToLongBits(val) >> 32);
		return (x - 1072632447) / 1512775;
	}

	/**
	 * Approximate calculation of a<sup>b</sup>.
	 * 
	 * @param a the base (real)
	 * @param b the exponent (real)
	 * @return the result of calculation
	 */

	public static double fastPowGeneral(final double a, final double b) {
		final long tmp = doubleToLongBits(a);
		final long tmp2 = (long) (b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
		return longBitsToDouble(tmp2);
	}

	/**
	 * A fast bitwise calculation of the absolute value. Arguably faster than
	 * {@code Math.abs}.
	 * 
	 * @param a the argument
	 * @return the calculation result
	 */

	public static double fastAbs(final double a) {
		return longBitsToDouble((doubleToLongBits(a) << 1) >>> 1);
	}

}