package pulse.math;

import pulse.properties.NumericProperty;

/**
 * Implements the Simpson's integration rule for the evaluation of definite
 * integrals.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Simpson%27s_rule">Wiki page</a>
 *
 */

public abstract class SimpsonIntegrator extends FixedIntervalIntegrator {

	public SimpsonIntegrator(Segment bounds) {
		super(bounds);
	}

	public SimpsonIntegrator(Segment bounds, NumericProperty segments) {
		super(bounds, segments);
	}

	/**
	 * Performs the integration with the Simpson's rule. Based on:
	 * https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
	 */

	@Override
	public double integrate() {
		double rmin = getBounds().getMinimum();
		double rmax = getBounds().getMaximum();

		double fa = integrand(rmin);
		double fb = integrand(rmax);

		// 1/3 terms
		double sum = (fa + fb);

		double x = 0;
		double y = 0;

		int integrationSegments = (int) getIntegrationSegments().getValue();
		double h = getStepSize();

		// 4/3 terms
		for (int i = 1; i < integrationSegments; i += 2) {
			x += integrand(rmin + h * i);
		}

		// 2/3 terms
		for (int i = 2; i < integrationSegments; i += 2) {
			y += integrand(rmin + h * i);
		}

		sum += x * 4.0 + y * 2.0;

		return sum * h / 3.0;
	}

}