package pulse.input;

import static java.lang.System.lineSeparator;
import static pulse.problem.statements.Pulse.TemporalShape.RECTANGULAR;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericPropertyKeyword.DETECTOR_GAIN;
import static pulse.properties.NumericPropertyKeyword.DETECTOR_IRIS;
import static pulse.properties.NumericPropertyKeyword.DIAMETER;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
import static pulse.tasks.Identifier.externalIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import pulse.problem.statements.Pulse.TemporalShape;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.Identifier;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * <p>
 * {@code Metadata} is the information relating to a specific experiment, which
 * is required to accurately process the {@code ExperimentalData}. It is used to
 * populate the associated {@code Problem}, {@code DifferenceScheme}, and the
 * fitting range of the {@code ExperimentalData}.
 * </p>
 *
 */

public class Metadata extends PropertyHolder implements Reflexive {

	private Set<NumericProperty> data;
	private int externalID;
	
	private TemporalShape pulseShape = RECTANGULAR;
	private String sampleName = "UnnamedSample";

	/**
	 * Creates a {@code Metadata} with the specified parameters.
	 * 
	 * @param temperature the NumericProperty with the type
	 *                    {@code NumericPropertyKeyword.TEST_TEMPERATURE}
	 * @param externalId  an integer, specifying the external ID recorded by the
	 *                    experimental setup.
	 */

	public Metadata(NumericProperty temperature, int externalId) {
		setExternalID(externalId);
		data = new TreeSet<NumericProperty>();
		set(TEST_TEMPERATURE, temperature);
	}
	
	/**
	 * Gets the external ID usually originating from the original exported
	 * experimental files. Note this is not a {@code NumericProperty}
	 * 
	 * @return an integer, representing the external ID
	 */

	public int getExternalID() {
		return externalID;
	}

	/**
	 * Sets the external ID in this {@code Metadata} to {@code externalId}
	 * 
	 * @param externalId the value of the external ID
	 */

	public void setExternalID(int externalId) {
		this.externalID = externalId;
	}

	/**
	 * Retrieves the pulse shape recorded in this {@code Metadata}
	 * 
	 * @return a {@code PulseShape} object
	 */

	public TemporalShape getPulseShape() {
		return pulseShape;
	}

	/**
	 * Sets the pulse shape recorded in this {@code Metadata}
	 * 
	 * @param pulseShape a {@code PulseShape} object
	 */

	public void setPulseShape(TemporalShape pulseShape) {
		this.pulseShape = pulseShape;
	}

	/**
	 * Retrieves the sample name. This name is used to create directories when
	 * exporting the data and also to fill the legend when plotting.
	 * 
	 * @return a string representing the sample name
	 */

	public String getSampleName() {
		return sampleName;
	}

	/**
	 * Sets the sample name
	 * 
	 * @param sampleName a string representing the sample name
	 */

	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	
	@Override
	public NumericProperty numericProperty(NumericPropertyKeyword key) {
		var optional = data.stream().filter(p -> p.getType() == key).findFirst();
		return optional.isPresent() ? optional.get() : null;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		
		if(type != property.getType() || !isListedParameter(property))
			throw new IllegalArgumentException("Illegal type: " + type);
		
		var optional = numericProperty(type);
		
		if( optional != null )
			optional.setValue( (Number) property.getValue());
		else 
			data.add(property);
		
		firePropertyChanged(this, property);
		
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>(9);
		list.add(def(TEST_TEMPERATURE));
		list.add(def(THICKNESS));
		list.add(def(DIAMETER));
		list.add(def(PULSE_WIDTH));
		list.add(def(SPOT_DIAMETER));
		list.add(def(LASER_ENERGY));
		list.add(def(DETECTOR_GAIN));
		list.add(def(DETECTOR_IRIS));
		list.add(RECTANGULAR);
		return list;
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append(sampleName + " [" + externalID + "]");
		sb.append(lineSeparator());
		sb.append(lineSeparator());

		data.forEach(entry -> {
			sb.append(entry.toString());
			sb.append(lineSeparator());
		});
		
		sb.append(pulseShape.toString());

		return sb.toString();

	}
	
	@Override
	public List<Property> data() {
		var list = new ArrayList<Property>();
		list.add(pulseShape);
		list.addAll(data);
		return list;
	}

	@Override
	public Identifier identify() {
		return getParent() == null ? externalIdentifier(externalID) : super.identify();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (! (o instanceof Metadata) )
			return false;

		var other = (Metadata) o;

		if (other.getExternalID() != this.getExternalID())
			return false;

		if (!sampleName.equals(other.getSampleName()))
			return false;
			
		return this.data().containsAll(other.data());

	}

}