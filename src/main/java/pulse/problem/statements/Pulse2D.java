package pulse.problem.statements;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

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
		sb.append(" ; ");
		sb.append(getSpotDiameter());
		return sb.toString();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(SPOT_DIAMETER));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == SPOT_DIAMETER)
			setSpotDiameter(property);
		else
			super.set(type, property);
	}

}