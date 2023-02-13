package pulse.problem.laser;

import static java.lang.Math.signum;

import pulse.problem.schemes.Grid2D;
import pulse.problem.statements.ClassicalProblem2D;
import pulse.problem.statements.Pulse2D;
import pulse.problem.statements.model.ExtendedThermalProperties;

/**
 * The discrete pulse on a {@code Grid2D}.
 * <p>
 * The main parameters are the {@code discretePulseWidth} (defined in the
 * superclass) and {@code discretePulseSpot}, which is the discretised version
 * of the spot diameter of the respective {@code Pulse} object.
 * </p>
 *
 */
public class DiscretePulse2D extends DiscretePulse {

    private static final long serialVersionUID = 6203222036852037146L;
    private double discretePulseSpot;
    private double sampleRadius;
    private double normFactor;

    /**
     * This had to be decreased for the 2d pulses.
     */
    private final static int WIDTH_TOLERANCE_FACTOR = 1000;

    /**
     * The constructor for {@code DiscretePulse2D}.
     * <p>
     * Calls the constructor of the superclass, after which calculates the
     * {@code discretePulseSpot} using the {@code gridRadialDistance} method of
     * this class. The dimension factor is defined as the sample diameter.
     * </p>
     *
     * @param problem a two-dimensional problem
     * @param grid the two-dimensional grid
     */
    public DiscretePulse2D(ClassicalProblem2D problem, Grid2D grid) {
        super(problem, grid);
        var properties = (ExtendedThermalProperties) problem.getProperties();
        calcPulseSpot(properties);
        properties.addListener(e -> calcPulseSpot(properties));
    }

    /**
     * This calculates the dimensionless, discretised pulse function at a
     * dimensionless radial coordinate {@code coord}.
     * <p>
     * It uses a Heaviside function to determine whether the {@code radialCoord}
     * lies within the {@code 0 <= radialCoord <= discretePulseSpot} interval.
     * It uses the {@code time} parameter to determine the discrete pulse
     * function using {@code evaluateAt(time)}. </p>
     *
     * @param time the time for calculation
     * @param radialCoord - the radial coordinate [length dimension]
     * @return the pulse function at {@code time} and {@code coord}, or 0 if
     * {@code coord > spotDiameter}.
     * @see pulse.problem.laser.PulseTemporalShape.laserPowerAt(double)
     */
    public double evaluateAt(double time, double radialCoord) {
        return laserPowerAt(time)
                * (0.5 + 0.5 * signum(discretePulseSpot - radialCoord));
    }

    /**
     * Calculates the laser power at a give moment in time. The total laser
     * energy is normalised over a beam partially illuminating the sample
     * surface.
     *
     * @param time a moment in time (in dimensionless units)
     * @return the laser power in arbitrary units
     */
    @Override
    public double laserPowerAt(double time) {
        return normFactor * super.laserPowerAt(time);
    }

    private void calcPulseSpot(ExtendedThermalProperties properties) {
        sampleRadius = (double) properties.getSampleDiameter().getValue() / 2.0;
        evalPulseSpot();
    }

    /**
     * Calculates the {@code discretePulseSpot} using the
     * {@code gridRadialDistance} method.
     *
     * @see pulse.problem.schemes.Grid2D.gridRadialDistance(double,double)
     */
    public final void evalPulseSpot() {
        var pulse = (Pulse2D) getPhysicalPulse();
        var grid2d = (Grid2D) getGrid();
        final double spotRadius = (double) pulse.getSpotDiameter().getValue() / 2.0;
        discretePulseSpot = grid2d.gridRadialDistance(spotRadius, sampleRadius);
        grid2d.adjustStepSize(this);
        normFactor = sampleRadius * sampleRadius / spotRadius / spotRadius;
    }

    public final double getDiscretePulseSpot() {
        return discretePulseSpot;
    }

    public final double getRadialConversionFactor() {
        return sampleRadius;
    }

    /**
     * A smaller tolerance factor is set for 2D calculations
     */
    @Override
    public int getWidthToleranceFactor() {
        return WIDTH_TOLERANCE_FACTOR;
    }

}
