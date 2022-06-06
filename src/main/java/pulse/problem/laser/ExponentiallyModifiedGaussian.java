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

import java.util.Set;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

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
   
    private final static int MIN_POINTS = 10;

    /**
     * Creates an exponentially modified Gaussian with the default parameter
     * values.
     */
    public ExponentiallyModifiedGaussian() {
        mu = (double) def(SKEW_MU).getValue();
        lambda = (double) def(SKEW_LAMBDA).getValue();
        sigma = (double) def(SKEW_SIGMA).getValue();
    }

    public ExponentiallyModifiedGaussian(ExponentiallyModifiedGaussian another) {
        super(another);
        this.mu = another.mu;
        this.sigma = another.sigma;
        this.lambda = another.lambda;
    }

    /**
     * Evaluates the laser power function. The error function is calculated
     * using the ApacheCommonsMath library tools.
     *
     * @see <a href="Wikipedia page">https://tinyurl.com/ExpModifiedGaussian</a>
     * @param time is measured from the 'start' of laser pulse
     */
    @Override
    public double evaluateAt(double time) {
        final var reducedTime = time / getPulseWidth();

        final double lambdaHalf = 0.5 * lambda;
        final double sigmaSq = sigma * sigma;

        return lambdaHalf * exp(lambdaHalf * (2.0 * mu + lambda * sigmaSq - 2.0 * reducedTime))
                * erfc((mu + lambda * sigmaSq - reducedTime) / (sqrt(2) * sigma));

    }

    /**
     * @see pulse.properties.NumericPropertyKeyword.SKEW_MU
     * @see pulse.properties.NumericPropertyKeyword.SKEW_LAMBDA
     * @see pulse.properties.NumericPropertyKeyword.SKEW_SIGMA
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(SKEW_MU);
        set.add(SKEW_LAMBDA);
        set.add(SKEW_SIGMA);
        return set;
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

    @Override
    public int getRequiredDiscretisation() {
        return MIN_POINTS;
    }

}