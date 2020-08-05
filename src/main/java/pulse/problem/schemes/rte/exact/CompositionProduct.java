package pulse.problem.schemes.rte.exact;

import pulse.math.AbstractIntegrator;
import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;
import pulse.problem.schemes.rte.BlackbodySpectrum;

/**
 * A class for evaluating the definite integral <math>&#8747;<sub>a</sub><sup>b</sup> <i>f</i>(<i>x</i>) <i>E</i><sub>n</sub> (&alpha; +
 * &beta; <i>x</i>) d<i>x</i>}</math>. This integral appears as a result of analytically integrating the radiative transfer equation for an 
 * absorbing-emitting medium. The number <i>n</i> is the order of this integral, and &alpha; and &beta; are the coefficients.
 *
 */

public abstract class CompositionProduct extends AbstractIntegrator {

	private double alpha;
	private double beta;
	private int order;

	private FunctionWithInterpolation expIntegral;
	private BlackbodySpectrum blackbody;

	/**
	 * Constructs the composition product with the specified integration bounds.
	 * @param bounds integration bounds
	 */
	
	public CompositionProduct(Segment bounds) {
		super(bounds);
	}
	
	/**
	 * Evaluates the integrand <math><i>f</i>(<i>x</i>) <i>E</i><sub>n</sub> (&alpha; +
	 * &beta; <i>x</i>) d<i>x</i>}</math>.
	 */
	
	@Override
	public double integrand(double... vars) {
		return blackbody.powerAt(vars[0]) * expIntegral.valueAt(alpha + beta * vars[0]);
	}

	public BlackbodySpectrum getEmissionFunction() {
		return blackbody;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.blackbody = emissionFunction;
	}

	public double getBeta() {
		return beta;
	}

	public double getAlpha() {
		return alpha;
	}

	public void setCoefficients(double alpha, double beta) {
		this.alpha = alpha;
		this.beta = beta;
	}
	
	public int getOrder() {
		return order;
	}

	/**
	 * Sets the integration order <i>n</i>. Updates the exponential integral associated with this
	 * {@code CompositionProduct} upon completion.
	 * @param order an integer in the range from 1 to 4 inclusively
	 */
	
	public void setOrder(int order) {
		this.order = order;
		expIntegral = ExponentialIntegrals.get(order);
	}

	@Override
	public String getPrefix() {
		return "Composition Product Integrator";
	}

}