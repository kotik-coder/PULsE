package pulse.problem.laser;

import static java.lang.Math.exp;
import static java.lang.Math.sqrt;
import static org.apache.commons.math3.special.Erf.erfc;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.SKEW_LAMBDA;
import static pulse.properties.NumericPropertyKeyword.SKEW_MU;
import static pulse.properties.NumericPropertyKeyword.SKEW_SIGMA;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * Represents the exponentially modified Gaussian function, which is given by
 * three independent parameters (&mu;, &sigma; and &lambda;).
 * 
 * @see <a href="https://tinyurl.com/ExpModifiedGaussian">Wikipedia page</a>
 *
 */

public class ExponentiallyModifiedGaussian extends PulseTemporalShape {

	private double mu;
	private double sigma;
	private double lambda;
	private double norm;

	/**
	 * Creates an exponentially modified Gaussian with the default parameter values.
	 */

	public ExponentiallyModifiedGaussian() {
		mu = (double) def(SKEW_MU).getValue();
		lambda = (double) def(SKEW_LAMBDA).getValue();
		sigma = (double) def(SKEW_SIGMA).getValue();
		norm = 1.0;
	}
	
	public ExponentiallyModifiedGaussian(ExponentiallyModifiedGaussian another) {
		super(another);
		this.mu = another.mu;
		this.sigma = another.sigma;
		this.lambda = another.lambda;
		this.norm = another.norm;
	}

	/**
	 * This calls the superclass {@code init method} and sets the normalisation
	 * factor to <math>1/&#8747;&Phi;(Fo)<i>d<i>Fo</math>.
	 */

	@Override
	public void init(ExperimentalData data, DiscretePulse pulse) {
		super.init(data, pulse);
		norm = 1.0 / area(); // calculates the area. The normalisation factor is then set to the inverse of
								// the area.
	}

	/**
	 * Evaluates the laser power function. The error function is calculated using
	 * the ApacheCommonsMath library tools.
	 * 
	 * @see <a href="Wikipedia page">https://tinyurl.com/ExpModifiedGaussian</a>
	 * @param time is measured from the 'start' of laser pulse
	 */

	@Override
	public double evaluateAt(double time) {
		final var reducedTime = time / getPulseWidth();

		final double lambdaHalf = 0.5 * lambda;
		final double sigmaSq = sigma * sigma;

		return norm * lambdaHalf * exp(lambdaHalf * (2.0 * mu + lambda * sigmaSq - 2.0 * reducedTime))
				* erfc((mu + lambda * sigmaSq - reducedTime) / (sqrt(2) * sigma));

	}
	
	/**
	 * @see pulse.properties.NumericPropertyKeyword.SKEW_MU
	 * @see pulse.properties.NumericPropertyKeyword.SKEW_LAMBDA
	 * @see pulse.properties.NumericPropertyKeyword.SKEW_SIGMA
	 */

	@Override
	public List<Property> listedTypes() {
		var list = super.listedTypes();
		list.add(def(SKEW_MU));
		list.add(def(SKEW_LAMBDA));
		list.add(def(SKEW_SIGMA));
		return list;
	}

	/**
	 * @return the &mu; parameter
	 */

	public NumericProperty getMu() {
		return derive(SKEW_MU, mu);
	}

	/**
	 * @return the &sigma; parameter
	 */

	public NumericProperty getSigma() {
		return derive(SKEW_SIGMA, sigma);
	}

	/**
	 * @return the &lambda; parameter
	 */

	public NumericProperty getLambda() {
		return derive(SKEW_LAMBDA, lambda);
	}

	/**
	 * Sets the {@code SKEW_LAMBDA} parameter
	 * 
	 * @param p the &lambda; parameter
	 */

	public void setLambda(NumericProperty p) {
		requireType(p, SKEW_LAMBDA);
		this.lambda = (double) p.getValue();
	}

	/**
	 * Sets the {@code SKEW_MU} parameter
	 * 
	 * @param p the &mu; parameter
	 */

	public void setMu(NumericProperty p) {
		requireType(p, SKEW_MU);
		this.mu = (double) p.getValue();
	}

	/**
	 * Sets the {@code SKEW_SIGMA} parameter
	 * 
	 * @param p the &sigma; parameter
	 */

	public void setSigma(NumericProperty p) {
		requireType(p, SKEW_SIGMA);
		this.sigma = (double) p.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case SKEW_MU:
			setMu(property);
			break;
		case SKEW_LAMBDA:
			setLambda(property);
			break;
		case SKEW_SIGMA:
			setSigma(property);
			break;
		default:
			break;
		}
		firePropertyChanged(this, property);
	}

	@Override
	public PulseTemporalShape copy() {
		return new ExponentiallyModifiedGaussian(this);
	}

}