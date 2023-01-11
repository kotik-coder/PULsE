package pulse.problem.statements;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.Set;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class Pulse2D extends Pulse {

    private double spotDiameter;

    /**
     * Creates a {@code Pulse} with default values of pulse width and laser spot
     * diameter (as per XML specification).
     *
     */
    public Pulse2D() {
        super();
        spotDiameter = (double) def(SPOT_DIAMETER).getValue();
    }

    /**
     * Copy constructor
     *
     * @param p the pulse, parameters of which will be copied.
     */
    public Pulse2D(Pulse p) {
        super(p);
        this.spotDiameter = p instanceof Pulse2D ? ((Pulse2D) p).spotDiameter : (double) def(SPOT_DIAMETER).getValue();
    }

    @Override
    public void initFrom(Pulse pulse) {
        super.initFrom(pulse);
        if (pulse instanceof Pulse2D) {
            this.spotDiameter = ((Pulse2D) pulse).spotDiameter;
        }
    }

    @Override
    public Pulse copy() {
        return new Pulse2D(this);
    }

    public NumericProperty getSpotDiameter() {
        return derive(SPOT_DIAMETER, spotDiameter);
    }

    public void setSpotDiameter(NumericProperty spotDiameter) {
        requireType(spotDiameter, SPOT_DIAMETER);
        this.spotDiameter = (double) spotDiameter.getValue();
        firePropertyChanged(this, spotDiameter);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(String.format("%n %-25s", getSpotDiameter()));
        return sb.toString();
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(SPOT_DIAMETER);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == SPOT_DIAMETER) {
            setSpotDiameter(property);
        } else {
            super.set(type, property);
        }
    }

}
