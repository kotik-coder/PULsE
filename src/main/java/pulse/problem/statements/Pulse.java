package pulse.problem.statements;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
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
		super();
		pulseWidth = (double) def(PULSE_WIDTH).getValue();
		laserEnergy = (double) def(LASER_ENERGY).getValue();
		instanceDescriptor.setSelectedDescriptor(RectangularPulse.class.getSimpleName());
		initShape();
		instanceDescriptor.addListener(() -> {
			initShape();
			this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
		});
	}

	/**
	 * Copy constructor
	 * 
	 * @param p the pulse, parameters of which will be copied.
	 */

	public Pulse(Pulse p) {
		super();
		this.pulseShape = p.getPulseShape();
		this.pulseWidth = p.pulseWidth;
		this.laserEnergy = p.laserEnergy;
		instanceDescriptor.addListener(() -> {
			initShape();
			this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
		});
	}
	
	public Pulse copy() {
		return new Pulse(this);
	}
	
	public void initFrom(Pulse pulse) {
		this.pulseWidth = pulse.pulseWidth;
		this.laserEnergy = pulse.laserEnergy;
		this.pulseShape = pulse.pulseShape;
	}
	
	public double evaluateAt(final double time) {
		return pulseShape.evaluateAt(time);
	}

	private void initShape() {
		setPulseShape(instanceDescriptor.newInstance(PulseTemporalShape.class));
		parameterListChanged();
	}

	public NumericProperty getPulseWidth() {
		return derive(PULSE_WIDTH, pulseWidth);
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		requireType(pulseWidth, PULSE_WIDTH);
		this.pulseWidth = (double) pulseWidth.getValue();
		
		if(pulseWidth.compareTo(this.getPulseWidth()) != 0)
			firePropertyChanged(this, pulseWidth);
	}

	public NumericProperty getLaserEnergy() {
		return derive(LASER_ENERGY, laserEnergy);
	}

	public void setLaserEnergy(NumericProperty laserEnergy) {
		requireType(laserEnergy, LASER_ENERGY);
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
		list.add(def(PULSE_WIDTH));
		list.add(def(LASER_ENERGY));
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
		pulseShape.setParent(this);
	}

}