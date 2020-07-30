package pulse.math;

import pulse.properties.NumericProperty;

/**
 * Implements the midpoint integration scheme for the evaluation of definite
 * integrals.
 * 
 * @see <a href="Wiki page">https://en.wikipedia.org/wiki/Midpoint_method</a>
 *
 */

public abstract class MidpointIntegrator extends FixedIntervalIntegrator {

	public MidpointIntegrator(Segment bounds, NumericProperty segments) {
		super(bounds, segments);
	}

	public MidpointIntegrator(Segment bounds) {
		super(bounds);
	}

	/**
	 * Performs the integration according to the midpoint scheme. This scheme should
	 * be used when the function is not well-defined at either of the integration
	 * bounds.
	 */

	@Override
	public double integrate() {
		final double a = getBounds().getMinimum();

		final int points = (int) getIntegrationSegments().getValue();

		double sum = 0;
		double h = getStepSize();
		for (int i = 0; i < points; i++) {
			sum += integrand(a + (i + 0.5) * h) * h;
		}
		return sum;
	}

}