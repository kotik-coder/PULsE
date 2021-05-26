package pulse.math.transforms;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.sqrt;

/**
 * A utility class containing standard mathematical transforms and their inverses for non-bounded parameters.
 *
 */

public class StandardTransformations {
		
	/**
	 * Logarithmic parameter transform. The parameter space is only bounded by positive numbers, so no bounding segment required.
	 */
	
	public final static Transformable LOG = new Transformable() {

		@Override
		public double transform(double a) {
			return log(a);
		}

		@Override
		public double inverse(double t) {
			return exp(t);
		}
		
	};
	
	public final static Transformable SQRT = new Transformable() {

		@Override
		public double transform(double a) {
			return sqrt(a);
		}

		@Override
		public double inverse(double t) {
			return t*t;
		}
		
	};
	
	private StandardTransformations() {
		//empty
	}
	
}