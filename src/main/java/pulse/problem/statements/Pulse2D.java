package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class Pulse2D extends Pulse {

	protected double spotDiameter;

	public Pulse2D() {
		super();
		spotDiameter = (double) NumericProperty.def(SPOT_DIAMETER).getValue();
	}

	/**
	 * Creates a {@code Pulse} with default values of pulse width and laser spot
	 * diameter (as per XML specification), but with custom {@code PulseShape}.
	 * 
	 * @param pform the pulse shape
	 */

	public Pulse2D(TemporalShape pform) {
		super(pform);
		spotDiameter = (double) NumericProperty.def(SPOT_DIAMETER).getValue();
	}

	/**
	 * Copy constructor
	 * 
	 * @param p the pulse, parameters of which will be copied.
	 */

	public Pulse2D(Pulse p) {
		super(p);
		if (p instanceof Pulse2D)
			this.spotDiameter = ((Pulse2D) p).spotDiameter;
		else
			spotDiameter = (double) NumericProperty.def(SPOT_DIAMETER).getValue();
	}

	public NumericProperty getSpotDiameter() {
		return NumericProperty.derive(SPOT_DIAMETER, spotDiameter);
	}

	public void setSpotDiameter(NumericProperty spotDiameter) {
		this.spotDiameter = (double) spotDiameter.getValue();
		notifyListeners(this, spotDiameter);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(super.toString());
		sb.append(String.format("%3.2f", spotDiameter * 1E3));
		return sb.toString();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(SPOT_DIAMETER));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case SPOT_DIAMETER:
			setSpotDiameter(property);
			break;
		default:
			break;
		}

		notifyListeners(this, property);

	}

}