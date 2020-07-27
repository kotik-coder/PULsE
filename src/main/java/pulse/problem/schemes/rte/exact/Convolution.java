package pulse.problem.schemes.rte.exact;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_CUTOFF;

import java.util.List;

import pulse.math.AbstractIntegrator;
import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * A class for evaluating the definite integral
 * $\int_a^b{f(x) E_n (\alpha + \beta x) dx}$.
 *
 */

public abstract class Convolution extends AbstractIntegrator {

	private double alpha;
	private double beta;
	private double cutoff;
	private int order;

	private FunctionWithInterpolation expIntegral;
	private BlackbodySpectrum blackbody;

	public Convolution(Segment bounds) {
		super(bounds);
	}

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

	public void setOrder(int order) {
		if (order < 1 || order > 4)
			throw new IllegalArgumentException("Unsupported integration order: " + order);
		this.order = order;
		expIntegral = ExponentialIntegrals.get(order);
	}

	public NumericProperty getCutoff() {
		return derive(INTEGRATION_CUTOFF, cutoff);
	}

	public void setCutoff(NumericProperty cutoff) {
		requireType(cutoff, INTEGRATION_CUTOFF);
		this.cutoff = (double)cutoff.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == INTEGRATION_CUTOFF) {
			setCutoff(property);
			firePropertyChanged(this, property);
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(INTEGRATION_CUTOFF));
		return list;
	}
	
	@Override
	public String getPrefix() {
		return "Convolution Integrator";
	}
	
}