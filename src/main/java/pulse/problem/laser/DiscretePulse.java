package pulse.problem.laser;

import java.io.Serializable;
import java.util.Objects;
import pulse.input.ExperimentalData;
import pulse.math.MidpointIntegrator;
import pulse.math.Segment;
import pulse.problem.schemes.Grid;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import pulse.tasks.SearchTask;
import pulse.util.PropertyHolderListener;

/**
 * A {@code DiscretePulse} is an object that acts as a medium between the
 * physical {@code Pulse} and the respective {@code DifferenceScheme} used to
 * process the solution of a {@code Problem}.
 *
 * @see pulse.problem.statements.Pulse
 */
public class DiscretePulse implements Serializable {

    private static final long serialVersionUID = 5826506918603729615L;
    private final Grid grid;
    private final Pulse pulse;
    private final ExperimentalData data;

    private double widthOnGrid;
    private double characteristicTime;
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
    private final static int WIDTH_TOLERANCE_FACTOR = 10000;

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
        characteristicTime = problem.getProperties().characteristicTime();
        this.pulse = problem.getPulse();

        Object ancestor
                = Objects.requireNonNull(problem.specificAncestor(SearchTask.class),
                        "Problem has not been assigned to a SearchTask");

        data = (ExperimentalData) (((SearchTask) ancestor).getInput());
        init();

        PropertyHolderListener phl = e -> {
            characteristicTime = problem.getProperties().characteristicTime();
            widthOnGrid = 0;
            init();
        };

        pulse.addListener(e -> {
            widthOnGrid = 0;
            init();
        });
        problem.addListener(phl);

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
    public final void init() {
        final double nominalWidth = ((Number) pulse.getPulseWidth().getValue()).doubleValue();
        final double resolvedWidth = resolvedPulseWidthSeconds();

        final double EPS = 1E-10;

        double oldValue = widthOnGrid;
        this.widthOnGrid = pulseWidthGrid();

        /**
         * The pulse is too short, which makes calculations too expensive. Can
         * we replace it with a rectangular pulse shape instead?
         */
        if (nominalWidth < resolvedWidth - EPS && oldValue < EPS) {
            //change shape to rectangular
            var shape = new RectangularPulse();
            pulse.setPulseShape(shape);
            shape.init(null, this);
        } else {
            pulse.getPulseShape().init(data, this);
        }

        invTotalEnergy = 1.0 / totalEnergy();
    }

    /**
     * Optimises the {@code Grid} parameters so that the timestep is
     * sufficiently small to enable accurate pulse correction.
     * <p>
     * This can change the {@code tauFactor} and {@code tau} variables in the
     * {@code Grid} object if {@code discretePulseWidth/(M - 1) < grid.tau},
     * where M is the required number of pulse calculations.
     * </p>
     *
     * @see PulseTemporalShape.getRequiredDiscretisation()
     */
    public double pulseWidthGrid() {
        //minimum number of points for pulse calculation
        int reqPoints = pulse.getPulseShape().getRequiredDiscretisation();
        //physical pulse width in time units
        double experimentalWidth = (double) pulse.getPulseWidth().getValue();

        //minimum resolved pulse width in time units for that specific problem
        double resolvedWidth = resolvedPulseWidthSeconds();

        double pWidth = Math.max(experimentalWidth, resolvedWidth);

        final double EPS = 1E-10;

        double newTau = pWidth / characteristicTime / reqPoints;

        double result = 0;

        if (newTau < grid.getTimeStep() - EPS) {
            double newTauFactor = (double) grid.getTimeFactor().getValue() / 2.0;
            grid.setTimeFactor(derive(TAU_FACTOR, newTauFactor));
            result = pulseWidthGrid();
        } else {
            result = grid.gridTime(pWidth, characteristicTime);
        }

        return result;
    }

    /**
     * Calculates the total pulse energy using a numerical integrator.The
     * normalisation factor is then equal to the inverse total energy.
     *
     * @return the total pulse energy, assuming sample area fully covered by the
     * beam
     */
    public final double totalEnergy() {
        var pulseShape = pulse.getPulseShape();

        var integrator = new MidpointIntegrator(new Segment(0, widthOnGrid)) {

            @Override
            public double integrand(double... vars) {
                return pulseShape.evaluateAt(vars[0]);
            }

        };

        return integrator.integrate();
    }

    /**
     * Gets the discrete dimensionless pulse width, which is a multiplier of the
     * current grid timestep. The pulse width is converted to the dimensionless
     * pulse width by dividing the real value by <i>l</i><sup>2</sup>/a.
     *
     * @return the dimensionless pulse width mapped to the grid.
     */
    public double getDiscreteWidth() {
        return widthOnGrid;
    }

    /**
     * Gets the physical {@code Pulse}
     *
     * @return the {@code Pulse} object
     */
    public Pulse getPhysicalPulse() {
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
     * Gets the dimensional factor required to convert real time variable into a
     * dimensional variable, defined in the {@code Problem} class
     *
     * @return the conversion factor
     */
    public double getCharacteristicTime() {
        return characteristicTime;
    }

    /**
     * Gets the minimal resolved pulse width defined by the
     * {@code WIDTH_TOLERANCE_FACTOR} and the characteristic time given by the
     * {@code getConversionFactor}.
     *
     * @return
     */
    public double resolvedPulseWidthSeconds() {
        return characteristicTime / getWidthToleranceFactor();
    }

    /**
     * Assuming a characteristic time is divided by the return value of this
     * method and is set to the minimal resolved pulse width, shows how small a
     * pulse width can be to enable finite pulse correction.
     *
     * @return the smallest fraction of a characteristic time resolved as a
     * finite pulse.
     */
    public int getWidthToleranceFactor() {
        return WIDTH_TOLERANCE_FACTOR;
    }

}
