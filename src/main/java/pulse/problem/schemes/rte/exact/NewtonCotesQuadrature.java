package pulse.problem.schemes.rte.exact;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_CUTOFF;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;

import java.util.List;
import java.util.Set;

import pulse.math.FixedIntervalIntegrator;
import pulse.math.MidpointIntegrator;
import pulse.math.Segment;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
import pulse.properties.Property;

/**
 * A class for evaluating the composition product using a simple Newton-Cotes
 * quadrature with a cutoff.
 *
 */
public class NewtonCotesQuadrature extends CompositionProduct {

    private final static int DEFAULT_SEGMENTS = 64;
    private final static double DEFAULT_CUTOFF = 16.0;
    private FixedIntervalIntegrator integrator;
    private double cutoff;

    /**
     * Constructs a default {@code NewtonCotesQuadrature} with integration
     * bounds spanning from 0 to 1.
     */
    public NewtonCotesQuadrature() {
        this(new Segment(0, 1));
    }

    /**
     * Constructs a default {@code NewtonCotesQuadrature} whose integration
     * bounds are specified by the argument.
     *
     * @param bounds the integration bounds
     */
    public NewtonCotesQuadrature(Segment bounds) {
        this(bounds, derive(INTEGRATION_SEGMENTS, DEFAULT_SEGMENTS));
    }

    /**
     * Constructs a custom {@code NewtonCotesQuadrature} with specified
     * integration bounds and number of integration segments. The underlying
     * integration scheme by default is a {@code SimpsonIntegrator}.
     *
     * @param bounds the integration bounds
     * @param segments the number of integration segments. The higher this
     * number, the higher is the accuracy.
     * @see pulse.math.SimpsonIntegrator
     */
    public NewtonCotesQuadrature(Segment bounds, NumericProperty segments) {
        super(bounds);
        setCutoff(derive(INTEGRATION_CUTOFF, DEFAULT_CUTOFF));
        CompositionProduct reference = this;
        integrator = new MidpointIntegrator(new Segment(0.0, 1.0), segments) {

            @Override
            public double integrand(double... vars) {
                return reference.integrand(vars);
            }

            @Override
            public String toString() {
                return getDescriptor() + " ; " + getIntegrationSegments();
            }

            @Override
            public String getDescriptor() {
                return "Midpoint Integrator";
            }

        };
        integrator.setParent(this);
    }

    /**
     * Uses the Newton-Cotes integrator (by default, the Simpson's rule) to
     * evaluate the composition product.
     */
    @Override
    public double integrate() {
        integrator.setBounds(truncatedBounds());
        return integrator.integrate();
    }

    /**
     * This will retrieve the Newton-Cotes integrator, which by default is the
     * Simpson integrator.
     *
     * @return the integrator
     */
    public FixedIntervalIntegrator getIntegrator() {
        return integrator;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + cutoff + " ; " + integrator.getIntegrationSegments();
    }

    public NumericProperty getCutoff() {
        return derive(INTEGRATION_CUTOFF, cutoff);
    }

    public void setCutoff(NumericProperty cutoff) {
        requireType(cutoff, INTEGRATION_CUTOFF);
        this.cutoff = (double) cutoff.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == INTEGRATION_CUTOFF) {
            setCutoff(property);
            firePropertyChanged(this, property);
        }
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(INTEGRATION_CUTOFF);
        set.add(INTEGRATION_SEGMENTS);
        return set;
    }

    @Override
    public boolean ignoreSiblings() {
        return true;
    }

    private Segment truncatedBounds() {
        final double min = getBounds().getMinimum();
        final double max = getBounds().getMaximum();

        double bound = (cutoff - getAlpha()) / getBeta();

        double a = 0.5 - getBeta() / 2; // beta usually takes values of 1 or -1, so a is either 0 or 1
        double b = 1. - a; // either 1 or 0

        return new Segment(max(bound, min) * a + min * b, max * a + min(bound, max) * b);
    }

}
