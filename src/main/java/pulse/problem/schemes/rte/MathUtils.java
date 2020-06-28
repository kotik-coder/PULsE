package pulse.problem.schemes.rte;

/**
 * Largely based on https://martin.ankerl.com
 *
 */

public class MathUtils {

	private MathUtils() {
	}

	public static double fastTanh(double a) {
		double e2x = MathUtils.fastExp(2.0 * a);
		return (e2x - 1.0) / (e2x + 1.0);
	}

	public static double atanh(double a) {
		return 0.5 * Math.log((1 + a) / (1 - a));
	}

	public static double fastPowLoop(double a, int b) {
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

	public static int fastPowSgn(int n) {
		return n % 2 == 0 ? 1 : -1;
	}

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

	public static double fastExp(double val) {
		final long tmp = (long) (1512775 * val + 1072632447);
		return Double.longBitsToDouble(tmp << 32);
	}

	public static double fastLog(double val) {
		final double x = (Double.doubleToLongBits(val) >> 32);
		return (x - 1072632447) / 1512775;
	}

	public static double fastPowGeneral(final double a, final double b) {
		final long tmp = Double.doubleToLongBits(a);
		final long tmp2 = (long) (b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
		return Double.longBitsToDouble(tmp2);
	}

	public static double fastAbs(double a) {
		return Double.longBitsToDouble((Double.doubleToLongBits(a) << 1) >>> 1);
	}

}