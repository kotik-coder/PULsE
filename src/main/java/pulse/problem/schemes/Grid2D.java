package pulse.problem.schemes;

import static java.lang.Math.pow;
import static java.lang.Math.rint;

import pulse.problem.laser.DiscretePulse;
import pulse.problem.laser.DiscretePulse2D;
import pulse.properties.NumericProperty;

/**
 * <p>
 * A {@code Grid2D} is used to partition the space and time domain of a
 * {@code Problem2D} to allow a numeric solution with a
 * {@code DifferenceScheme}. This type of grid is two-dimensional in space,
 * meaning that it defines rules for partitioning of both the axial and radial
 * dimensions for interpreting the laser flash experiments.
 * </p>
 */
public class Grid2D extends Grid {

    private static final long serialVersionUID = 564113358979595637L;
    private double hy;

    protected Grid2D() {
        super();
    }

    /**
     * Creates a {@code Grid2D} where the radial and axial spatial steps are
     * equal to the inverse {@code gridDensity}. Otherwise, calls the superclass
     * constructor.
     *
     * @param gridDensity the grid density
     * @param timeFactor the {@code &tau;<sub>F</sub>} factor
     */
    public Grid2D(NumericProperty gridDensity, NumericProperty timeFactor) {
        super(gridDensity, timeFactor);
        hy = 1.0 / getGridDensityValue();
    }

    @Override
    public Grid2D copy() {
        return new Grid2D(getGridDensity(), getTimeFactor());
    }

    @Override
    public void setTimeFactor(NumericProperty timeFactor) {
        super.setTimeFactor(timeFactor);
        setTimeStep((double) timeFactor.getValue() * (pow(getXStep(), 2) + pow(hy, 2)));
    }

    /**
     * Calls the {@code adjustTo} method from superclass, then adjusts the
     * {@code gridDensity} of the {@code grid} if
     * {@code discretePulseSpot < (Grid2D)grid.hy}.
     *
     * @param pulse the discrete puls representation
     */
    public void adjustStepSize(DiscretePulse pulse) {
        var pulse2d = (DiscretePulse2D) pulse;
        double pulseSpotSize = pulse2d.getDiscretePulseSpot();

        if (hy > pulseSpotSize) {
            final int INCREMENT = 5;
            final int newN = getGridDensityValue() + INCREMENT;
            setGridDensityValue(newN);
            adjustStepSize(pulse);
        }

    }

    @Override
    protected void setGridDensityValue(int N) {
        super.setGridDensityValue(N);
        hy = 1. / N;
    }

    /**
     * Sets the value of the {@code gridDensity}. Automatically recalculates the
     * {@code hx} an {@code hy} values.
     */
    @Override
    public void setGridDensity(NumericProperty gridDensity) {
        super.setGridDensity(gridDensity);
        hy = getXStep();
    }

    /**
     * The dimensionless radial distance on this {@code Grid2D}, which is the
     * {@code radial/lengthFactor} rounded up to a factor of the coordinate step
     * {@code hy}.
     *
     * @param radial the distance along the radial direction
     * @param lengthFactor a factor which has the dimension of length
     * @return a double representing the radial distance on the finite grid
     */
    public double gridRadialDistance(double radial, double lengthFactor) {
        return rint((radial / lengthFactor) / hy) * hy;
    }

    public double getYStep() {
        return hy;
    }

}
