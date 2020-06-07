package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.MathUtils;

public class HermiteInterpolator {

	protected static double y1;
	protected static double y0;
	protected static double d1;
	protected static double d0;
	
	protected static double a;
	protected static double len;
	
	private HermiteInterpolator() { }
		
	public static void clear() {
		y1 = 0;
		y0 = 0;
		d1 = 0;
		d0 = 0;
		a = 0;
		len = 0;
	}
	
	public static double interpolate(double x) {
		double t = (x - a)/len;
		return t*t*(3.0 - 2.0*t)*y1 + MathUtils.fastPowLoop(t - 1.0, 2)*(1.0 + 2.0*t)*y0 +
			   (t*t*(t - 1.0)*d1 + MathUtils.fastPowLoop(t - 1.0, 2)*t*d0)*len;
	}
	
}