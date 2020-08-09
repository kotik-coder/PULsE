package pulse.problem.schemes.rte.exact;

import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;

/**
 * A factory class for creating and evaluating {@code ExponentialIntegral}s of 
 * orders from 1 to 4. 
 *
 */

public class ExponentialIntegrals {

	public final static double CUTOFF = 20.0; // corresponds to a precision of 1E-5
	public final static int HIGHEST_ORDER = 4;

	private FunctionWithInterpolation[] exponentialIntegrals = new FunctionWithInterpolation[HIGHEST_ORDER + 1];
	private static ExponentialIntegrals instance = new ExponentialIntegrals();

	private ExponentialIntegrals() {
		final double LOWER_BOUND_E1 = 1E-8;
		for (int i = 1; i < HIGHEST_ORDER + 1; i++) {
			var ei = new ExponentialIntegral(i);
			var min = i == 1 ? LOWER_BOUND_E1 : 0.0; // First-order exponential integral is discontinuous at 0.0, so
														// discard this point

			exponentialIntegrals[i] = new FunctionWithInterpolation(new Segment(min, CUTOFF)) {

				@Override
				public double evaluate(double t) {
					ei.setParameter(t);
					return ei.integrate();
				}

			};
		}
	}

	/**
	 * Retrieves the pre-calculated interpolation functions for the exponential integrals.
	 * @param order the order (1 to 4) of the exponential integral
	 * @return a pre-calculated interpolation with the default bounds
	 */
	
	public static FunctionWithInterpolation get(int order) {
		return instance.exponentialIntegrals[order];
	}

//	public static void main(String[] args) {
//		var ei = get(2);
//		System.out.println(ei.valueAt(0.01));
//	}

}