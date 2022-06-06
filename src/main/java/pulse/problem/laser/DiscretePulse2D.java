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

    private double discretePulseSpot;
    private double radialFactor;

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
        init(properties);
        
        properties.addListener(e -> init(properties) );
    }
    
    /**
     * This calculates the dimensionless, discretised pulse function at a
     * dimensionless radial coordinate {@code coord}.
     * <p>
     * It uses a Heaviside function to determine whether the {@code radialCoord}
     * lies within the {@code 0 <= radialCoord <= discretePulseSpot} interval.
     * It uses the {@code time} parameter to determine the discrete pulse
     * function using {@code evaluateAt(time)}.
     *
     * @param time the time for calculation
     * @param radialCoord - the radial coordinate [length dimension]
     * @return the pulse function at {@code time} and {@code coord}, or 0 if
     * {@code coord > spotDiameter}.
     * @see pulse.problem.laser.PulseTemporalShape.laserPowerAt(double)
     */
    public double evaluateAt(double time, double radialCoord) {
        return laserPowerAt(time) * (0.5 + 0.5 * signum(discretePulseSpot - radialCoord));
    }
    
    private void init(ExtendedThermalProperties properties) {
        radialFactor    = (double) properties.getSampleDiameter().getValue() / 2.0;
        evalPulseSpot();
    }

    /**
     * Calculates the {@code discretePulseSpot} using the {@code gridRadialDistance} method.
     *
     * @see pulse.problem.schemes.Grid2D.gridRadialDistance(double,double)
     */
    public final void evalPulseSpot() {
        var pulse = (Pulse2D) getPulse();
        var grid2d = (Grid2D) getGrid();
        final double radius = (double) pulse.getSpotDiameter().getValue() / 2.0;       
        discretePulseSpot = grid2d.gridRadialDistance(radius, radialFactor);
        grid2d.adjustStepSize(this);
    }

    public final double getDiscretePulseSpot() {
        return discretePulseSpot;
    }
    
    public final double getRadialConversionFactor() {
        return radialFactor;
    }

}
