package pulse.problem.schemes.rte.exact;

import static java.lang.Math.exp;
import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;

import pulse.math.MidpointIntegrator;
import pulse.math.Segment;
import pulse.properties.NumericProperty;

/**
 * A {@code MidpointIntegrator} for calculating the exponential integrals
 * of order from 1 through 4. 
 *
 */

class ExponentialIntegral extends MidpointIntegrator {

	private double t;
	private int order;

	private final static double EPS = 1E-10;
	private final static int DEFAULT_INTEGRATION_SEGMENTS = 2048;
	
	/**
	 * Constructs an {@code ExponentialIntegral} with the specified
	 * {@code order} and the required number of {@code segments}.
	 * @param order the order of exponential integral
	 * @param segments the number of integration segments
	 */

	public ExponentialIntegral(int order, NumericProperty segments) {
		super(new Segment(0, 1), segments); // [0, 1] - cosine domain
		setOrder(order);
	}
	
	/**
	 * Constructs an {@code ExponentialIntegral} of the specified order 
	 * with the default number of integration segments.
	 * @param order the order of exponential integral
	 */

	public ExponentialIntegral(int order) {
		this(order, derive(INTEGRATION_SEGMENTS, DEFAULT_INTEGRATION_SEGMENTS));
		setOrder(order);
	}

	/**
	 * Either calls the superclass method or, if the parameter is zero,
	 * uses the shortcut formula <math><i>E</i><sub>n</sub>(0) = 1/(n - 1)</math>.
	 */
	
	@Override
	public double integrate() {
		return t < EPS ? 1.0 / (order - 1.0) : super.integrate();
	}

	@Override
	public double integrand(double... vars) {
		final double mu = vars[0];
		return fastPowLoop(mu, order - 2) * exp(-t / mu);
	}

	protected double getParameter() {
		return t;
	}

	protected void setParameter(double t) {
		this.t = t;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

}