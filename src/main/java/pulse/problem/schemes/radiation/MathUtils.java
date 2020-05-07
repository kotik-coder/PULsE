package pulse.problem.schemes.radiation;

/**
 * Largely based on https://martin.ankerl.com
 *
 */

public class MathUtils {

	private MathUtils() { }
	
	public static double fastPowLoop(double a, int b) {
		double re = 1;
		//if positive (i < b is never satisfied for negative b's)
		for(int i = 0; i < b; i++)
			re *= a;
		//if negative
		for(int i = 0; i < -b; i++)
			re /= a;
		return re;
	}
	
	public static int fastPowSgn(int n) {
		return n % 2 == 0 ? 1 : -1;
	}
	
    public static int fastPowInt(int a, long b) {
        int re = 1;
        while (b > 0) {
            if ((b & 1) == 1) {
                re *= a;        
            }
            b >>= 1;
            a *= a; 
        }
        return re;
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
	    final long tmp2 = (long)(b * (tmp - 4606921280493453312L)) + 4606921280493453312L;
	    return Double.longBitsToDouble(tmp2);
	}
	
	public static double fastAbs(double a) {
		return Double.longBitsToDouble((Double.doubleToLongBits(a)<<1)>>>1);
	}
	
} 