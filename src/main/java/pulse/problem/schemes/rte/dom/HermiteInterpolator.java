package pulse.problem.schemes.rte.dom;

/**
 * 
 * @author Vadim Zborovskii, Artem Lunev
 *
 */

public class HermiteInterpolator {

	protected static double y1;
	protected static double y0;
	protected static double d1;
	protected static double d0;

	protected static double a;
	protected static double bMinusA;

	private HermiteInterpolator() {
	}

	public static void clear() {
		y1 = 0;
		y0 = 0;
		d1 = 0;
		d0 = 0;
		a = 0;
		bMinusA = 0;
	}

	public static double interpolate(double x) {
		double t = (x - a) / bMinusA;
		double tMinusOne = (t - 1.0);
		return t * t * (3.0 - 2.0 * t) * y1 + tMinusOne * tMinusOne * (1.0 + 2.0 * t) * y0
				+ (t * t * tMinusOne * d1 + tMinusOne * tMinusOne * t * d0) * bMinusA;
	}

	public static double derivative(double x) {
		double t = (x - a) / bMinusA;
		double tt1 = t * (t - 1.0);

		return 6.0 * tt1 * (y0 - y1) / bMinusA + (t * (3.0 * t - 2.0) * d1 + (3.0 * t * t - 4.0 * t + 1.0) * d0);
	}

	public static String printout() {
		return String.format("%n (%3.6f ; %3.6f), (%3.6f ; %3.6f), (%3.6f, %3.6f)", y0, y1, d0, d1, a, (bMinusA - a));
	}

}