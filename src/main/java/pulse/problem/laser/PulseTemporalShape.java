package pulse.problem.laser;

import pulse.input.ExperimentalData;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An abstract time-dependent pulse shape. Declares the abstract method to
 * calculate the laser power function at a given moment of time. This generally
 * utilises a discrete pulse width. By default, uses a midpoint-rule numeric
 * integrator to calculate the pulse integral.
 *
 */
public abstract class PulseTemporalShape extends PropertyHolder implements Reflexive {

    private double width;

    public PulseTemporalShape() {
        //intentionlly blank
    }

    public PulseTemporalShape(PulseTemporalShape another) {
        this.width = another.width;
    }

    /**
     * This evaluates the dimensionless, discretised pulse function on a
     * {@code grid} needed to evaluate the heat source in the difference scheme.
     *
     * @param time the dimensionless time (a multiplier of {@code tau}), at
     * which calculation should be performed
     * @return a double value, representing the pulse function at {@code time}
     */
    public abstract double evaluateAt(double time);

    /**
     * Stores the pulse width from {@code pulse} and initialises area
     * integration.
     *
     * @param data
     * @param pulse the discrete pulse containing the pulse width
     */
    public void init(ExperimentalData data, DiscretePulse pulse) {
        width = pulse.getDiscreteWidth();
    }

    public abstract PulseTemporalShape copy();

    @Override
    public String getPrefix() {
        return "Pulse temporal shape";
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public double getPulseWidth() {
        return width;
    }

    public void setPulseWidth(double width) {
        this.width = width;
    }

    public abstract int getRequiredDiscretisation();

}
