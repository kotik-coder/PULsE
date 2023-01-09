package pulse.problem.schemes;

import static java.lang.Math.pow;
import static java.lang.Math.rint;
import static java.lang.String.format;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;

import java.util.Set;

import pulse.problem.laser.DiscretePulse;
import pulse.problem.statements.Pulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;

/**
 * <p>
 * A {@code Grid} is used to partition the space and time domain of a
 * {@code Problem} to allow a numeric solution with a {@code DifferenceScheme}.
 * This specific class of grids is one-dimensional in space, meaning that it
 * only defines rules for partitioning the axial dimension in the laser flash
 * experiment.
 * </p>
 *
 */
public class Grid extends PropertyHolder {

    private double hx;
    private double tau;
    private double tauFactor;
    private int N;

    /**
     * Creates a {@code Grid} object with the specified {@code gridDensity} and
     * {@code timeFactor}.
     *
     * @param gridDensity a {@code NumericProperty} of the type
     * {@code GRID_DENSITY}
     * @param timeFactor a {@code NumericProperty} of the type
     * {@code TIME_FACTOR}
     * @see pulse.properties.NumericPropertyKeyword
     */
    public Grid(NumericProperty gridDensity, NumericProperty timeFactor) {
        this.N = (int) gridDensity.getValue();
        this.tauFactor = (double) timeFactor.getValue();
        hx = 1. / N;
        setTimeStep(tauFactor * pow(hx, 2));   
    }

    protected Grid() {
        // intentionally blank
    }

    /**
     * Creates a new {@code Grid} object with exactly the same parameters as
     * this one.
     *
     * @return a new {@code Grid} object replicating this {@code Grid}
     */
    public Grid copy() {
        return new Grid(getGridDensity(), getTimeFactor());
    }

    /**
     * The listed properties include {@code GRID_DENSITY} and
     * {@code TAU_FACTOR}.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(GRID_DENSITY);
        set.add(TAU_FACTOR);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        switch (type) {
            case TAU_FACTOR:
                setTimeFactor(property);
                break;
            case GRID_DENSITY:
                setGridDensity(property);
                break;
            default:
                break;
        }
    }

    /**
     * Retrieves the value of the <math><i>h<sub>x</sub></i></math> coordinate
     * step used in finite-difference calculation.
     *
     * @return a double, representing the {@code hx} value.
     */
    public final double getXStep() {
        return hx;
    }

    /**
     * Sets the value of the <math><i>h<sub>x</sub></i></math> coordinate step.
     *
     * @param hx a double, representing the new {@code hx} value.
     */
    public final void setXStep(double hx) {
        this.hx = hx;
    }

    /**
     * Retrieves the value of the &tau; time step used in finite-difference
     * calculation.
     *
     * @return a double, representing the {@code tau} value.
     */
    public final double getTimeStep() {
        return tau;
    }

    protected final void setTimeStep(double tau) {
        this.tau = tau;
        
    }

    /**
     * Retrieves the value of the &tau;-factor, or the time factor, used in
     * finite-difference calculation. This factor determines the proportionally
     * coefficient between &tau; and <math><i>h<sub>x</sub></i></math>.
     *
     * @return a NumericProperty of the {@code TAU_FACTOR} type, representing
     * the {@code tauFactor} value.
     */
    public final NumericProperty getTimeFactor() {
        return derive(TAU_FACTOR, tauFactor);
    }

    /**
     * Retrieves the value of the {@code gridDensity} used to calculate the
     * {@code hx} and {@code tau}.
     *
     * @return a NumericProperty of the {@code GRID_DENSITY} type, representing
     * the {@code gridDensity} value.
     */
    public final NumericProperty getGridDensity() {
        return derive(GRID_DENSITY, N);
    }

    protected final int getGridDensityValue() {
        return N;
    }

    protected void setGridDensityValue(int N) {
        this.N = N;
        hx = 1. / N;
    }

    /**
     * Sets the value of the {@code gridDensity}. Automatically recalculates the
     * {@code hx} value.
     *
     * @param gridDensity a NumericProperty of the {@code GRID_DENSITY} type
     */
    public void setGridDensity(NumericProperty gridDensity) {
        requireType(gridDensity, GRID_DENSITY);
        this.N = (int) gridDensity.getValue();
        hx = 1. / N;
        setTimeStep(tauFactor * pow(hx, 2));  
        firePropertyChanged(this, gridDensity);
    }

    /**
     * Sets the value of the {@code tauFactor}. Automatically recalculates the
     * {@code tau} value.
     *
     * @param timeFactor a NumericProperty of the {@code TAU_FACTOR} type
     */
    public void setTimeFactor(NumericProperty timeFactor) {
        requireType(timeFactor, TAU_FACTOR);
        this.tauFactor = (double) timeFactor.getValue();
        setTimeStep(tauFactor * pow(hx, 2));        
        firePropertyChanged(this, timeFactor);
    }

    /**
     * The dimensionless time on this {@code Grid}, which is the
     * {@code time/dimensionFactor} rounded up to a factor of the time step
     * {@code tau}.
     *
     * @param time the time
     * @param dimensionFactor a conversion factor with the dimension of time
     * @return a double representing the time on the finite grid
     */
    public final double gridTime(double time, double dimensionFactor) {
        return ( (int) (time / dimensionFactor / tau) ) * tau;
    }

    /**
     * The dimensionless axial distance on this {@code Grid}, which is the
     * {@code distance/lengthFactor} rounded up to a factor of the coordinate
     * step {@code hx}.
     *
     * @param distance the distance along the axial direction
     * @param lengthFactor a conversion factor with the dimension of length
     * @return a double representing the axial distance on the finite grid
     */
    public final double gridAxialDistance(double distance, double lengthFactor) {
        return rint((distance / lengthFactor) / hx) * hx;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("<html>").
        append(getClass().getSimpleName())
                .append(": <math><i>h<sub>x</sub></i>=")
                .append(format("%3.2e", hx))
                .append("; ").
        append("<i>&tau;</i>=")
                .append(format("%3.2e", tau))
                .append("; ");
        return sb.toString();
    }

}
