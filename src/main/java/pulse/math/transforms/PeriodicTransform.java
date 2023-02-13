package pulse.math.transforms;

import pulse.math.Segment;

public class PeriodicTransform extends BoundedParameterTransform {

    private static final long serialVersionUID = 4564881912462997982L;

    /**
     * Only the upper bound of the argument is used.
     *
     * @param bounds the {@code bounda.getMaximum()} is used in the transforms
     */
    public PeriodicTransform(Segment bounds) {
        super(bounds);
    }

    /**
     * @param a
     * @see pulse.math.MathUtils.atanh()
     * @see pulse.math.Segment.getBounds()
     */
    @Override
    public double transform(double a) {
        double max = getBounds().getMaximum();
        double min = getBounds().getMinimum();
        double len = max - min;

        return a > max ? transform(a - len) : (a < min ? transform(a + len) : a);
    }

    /**
     * @see pulse.math.MathUtils.tanh()
     * @see pulse.math.Segment.getBounds()
     */
    @Override
    public double inverse(double t) {
        return t;
    }
}
