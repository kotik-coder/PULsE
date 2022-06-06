package pulse.problem.laser;

import java.util.Objects;
import pulse.input.ExperimentalData;
import pulse.math.MidpointIntegrator;
import pulse.math.Segment;
import pulse.problem.schemes.Grid;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.tasks.SearchTask;

/**
 * A {@code DiscretePulse} is an object that acts as a medium between the
 * physical {@code Pulse} and the respective {@code DifferenceScheme} used to
 * process the solution of a {@code Problem}.
 *
 * @see pulse.problem.statements.Pulse
 */
public class DiscretePulse {

    private final Grid grid;
    private final Pulse pulse;
    
    private double widthOnGrid;
    private double timeConversionFactor;
    private double invTotalEnergy; //normalisation factor

    /**
     * This number shows how small the actual pulse may be compared to the
     * half-time. If the pulse is shorter than
     * <i>t</i><sub>c</sub>/{@value WIDTH_TOLERANCE_FACTOR}, it will be replaced
     * by a rectangular pulse with the width equal to
     * <i>t</i><sub>c</sub>/{@value WIDTH_TOLERANCE_FACTOR}. Here
     * <i>t</i><sub>c</sub>
     * is the time factor defined in the {@code Problem} class.
     */
    public final static int WIDTH_TOLERANCE_FACTOR = 1000;

    /**
     * This creates a one-dimensional discrete pulse on a {@code grid}.
     * <p>
     * The dimensional factor is taken from the {@code problem}, while the
     * discrete pulse width (a multiplier of the {@code grid} parameter
     * {@code tau} is calculated using the {@code gridTime} method.
     * </p>
     *
     * @param problem the problem, used to extract the dimensional time factor
     * @param grid a grid used to discretise the {@code pulse}
     */
    public DiscretePulse(Problem problem, Grid grid) {
        this.grid = grid;
        timeConversionFactor = problem.getProperties().timeFactor();
        this.pulse = problem.getPulse();

        Object ancestor
                = Objects.requireNonNull(problem.specificAncestor(SearchTask.class),
                        "Problem has not been assigned to a SearchTask");

        ExperimentalData data = ((SearchTask) ancestor).getExperimentalCurve();
        init(data);
        
        pulse.addListener(e -> {
            timeConversionFactor = problem.getProperties().timeFactor();
            init(data);
        });

        grid.addListener(e
                -> init(data)
        );
        
    }
    
    private void init(ExperimentalData data) {
        widthOnGrid = 0;
        recalculate();
        pulse.getPulseShape().init(data, this);
        normalise();
    }

    /**
     * Uses the {@code PulseTemporalShape} of the {@code Pulse} object to
     * calculate the laser power at the specified moment of {@code time}.
     *
     * @param time the time argument
     * @return the laser power at the specified moment of {@code time}
     */
    public double laserPowerAt(double time) {
        return invTotalEnergy * pulse.getPulseShape().evaluateAt(time);
    }

    /**
     * Recalculates the {@code discretePulseWidth} by calling {@code gridTime}
     * on the physical pulse width and {@code timeFactor}.
     *
     * @see pulse.problem.schemes.Grid.gridTime(double,double)
     */
    public final void recalculate() {
        final double nominalWidth  = ((Number) pulse.getPulseWidth().getValue()).doubleValue();
        final double resolvedWidth = timeConversionFactor / WIDTH_TOLERANCE_FACTOR;

        final double EPS = 1E-10;
        
        /**
         * The pulse is too short, which makes calculations too expensive. Can
         * we replace it with a rectangular pulse shape instead?
         */
        
        if (nominalWidth < resolvedWidth - EPS && widthOnGrid < EPS) {
            //change shape to rectangular
            var shape = new RectangularPulse();
            pulse.setPulseShape(shape);            
            //change pulse width
            setDiscreteWidth(resolvedWidth);
            shape.init(null, this);
        } else if(nominalWidth > resolvedWidth + EPS) {
            setDiscreteWidth(nominalWidth);
        } 
        
    }
    
    /**
     * Calculates the total pulse energy using a numerical integrator. The 
     * normalisation factor is then equal to the inverse total energy.
     */

    public final void normalise() {
        invTotalEnergy = 1.0;
        var pulseShape = pulse.getPulseShape();

        var integrator = new MidpointIntegrator(new Segment(0, widthOnGrid)) {

            @Override
            public double integrand(double... vars) {
                return pulseShape.evaluateAt(vars[0]);
            }

        };

        invTotalEnergy = 1.0 / integrator.integrate();

    }

    /**
     * Gets the discrete dimensionless pulse width, which is a multiplier of the current
     * grid timestep. The pulse width is converted to the dimensionless pulse width by 
     * dividing the real value by <i>l</i><sup>2</sup>/a.
     *
     * @return the dimensionless pulse width mapped to the grid.
     */
    public double getDiscreteWidth() {
        return widthOnGrid;
    }
    
    private void setDiscreteWidth(double width) {
        widthOnGrid = grid.gridTime(width, timeConversionFactor);
        grid.adjustTimeStep(this);
    }

    /**
     * Gets the physical {@code Pulse}
     *
     * @return the {@code Pulse} object
     */
    public Pulse getPulse() {
        return pulse;
    }

    /**
     * Gets the {@code Grid} object used to construct this {@code DiscretePulse}
     *
     * @return the {@code Grid} object.
     */
    public Grid getGrid() {
        return grid;
    }
    
    /**
     * Gets the dimensional factor required to convert real time variable into
     * a dimensional variable, defined in the {@code Problem} class
     * @return the conversion factor
     */
    
    public double getConversionFactor() {
        return timeConversionFactor;
    }
    
    /**
     * Gets the minimal resolved pulse width defined by the {@code WIDTH_TOLERANCE_FACTOR}
     * and the characteristic time given by the {@code getConversionFactor}.
     * @return 
     */
    
    public double resolvedPulseWidth() {
        return timeConversionFactor / WIDTH_TOLERANCE_FACTOR;
    }

}