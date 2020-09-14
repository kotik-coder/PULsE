package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.laser.PulseTemporalShape;
import pulse.problem.laser.RectangularPulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;

/**
 * A {@code Pulse} stores the parameters of the laser pulse, but does not
 * provide the calculation facilities.
 * 
 * @see pulse.problem.laser.DiscretePulse
 * 
 */

public class Pulse extends PropertyHolder {

	private double pulseWidth;
	private double laserEnergy;

	private PulseTemporalShape pulseShape;

	private InstanceDescriptor<? extends PulseTemporalShape> instanceDescriptor = new InstanceDescriptor<PulseTemporalShape>(
			"Pulse Shape Selector", PulseTemporalShape.class);

	/**
	 * Creates a {@code Pulse} with default values of pulse width and laser spot
	 * diameter (as per XML specification) and with a default pulse temporal shape (rectangular).
	 * 
	 */

	public Pulse() {
		pulseWidth = (double) NumericProperty.def(PULSE_WIDTH).getValue();
		laserEnergy = (double) NumericProperty.def(LASER_ENERGY).getValue();
		instanceDescriptor.setSelectedDescriptor(RectangularPulse.class.getSimpleName());
		initShape();
		instanceDescriptor.addListener(() -> initShape());
	}

	/**
	 * Copy constructor
	 * 
	 * @param p the pulse, parameters of which will be copied.
	 */

	public Pulse(Pulse p) {
		this.pulseShape = p.getPulseShape();
		this.pulseWidth = p.pulseWidth;
		this.laserEnergy = p.laserEnergy;
		instanceDescriptor.addListener(() -> initShape());
	}

	private void initShape() {
		setPulseShape(instanceDescriptor.newInstance(PulseTemporalShape.class));
		parameterListChanged();
	}

	public NumericProperty getPulseWidth() {
		return NumericProperty.derive(PULSE_WIDTH, pulseWidth);
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double) pulseWidth.getValue();
		firePropertyChanged(this, pulseWidth);
	}

	public NumericProperty getLaserEnergy() {
		return NumericProperty.derive(LASER_ENERGY, laserEnergy);
	}

	public void setLaserEnergy(NumericProperty laserEnergy) {
		this.laserEnergy = (double) laserEnergy.getValue();
		firePropertyChanged(this, laserEnergy);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getPulseShape());
		sb.append(" ; ");
		sb.append(getPulseWidth());
		sb.append(" ; ");
		sb.append(getLaserEnergy());
		return sb.toString();
	}

	/**
	 * The listed parameters for {@code Pulse} are:
	 * <code>PulseShape, PULSE_WIDTH, SPOT_DIAMETER</code>.
	 */

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(NumericProperty.def(PULSE_WIDTH));
		list.add(NumericProperty.def(LASER_ENERGY));
		list.add(instanceDescriptor);
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if(type == PULSE_WIDTH) 
			setPulseWidth(property);
		else if(type == LASER_ENERGY)
			setLaserEnergy(property);
	}

	public InstanceDescriptor<? extends PulseTemporalShape> getPulseDescriptor() {
		return instanceDescriptor;
	}

	public void setPulseDescriptor(InstanceDescriptor<? extends PulseTemporalShape> shapeDescriptor) {
		this.instanceDescriptor = shapeDescriptor;
		//TODO
		initShape();
	}

	public PulseTemporalShape getPulseShape() {
		return pulseShape;
	}

	public void setPulseShape(PulseTemporalShape pulseShape) {
		this.pulseShape = pulseShape;
	}

}