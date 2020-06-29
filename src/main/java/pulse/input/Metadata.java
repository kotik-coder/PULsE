package pulse.input;

import static java.lang.System.lineSeparator;
import static pulse.problem.statements.Pulse.TemporalShape.RECTANGULAR;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.isValueSensible;
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

	private double testTemperature, sampleThickness, sampleDiameter, pulseWidth, spotDiameter, laserEnergy;
	private int detectorGain, detectorIris;
	private TemporalShape pulseShape = RECTANGULAR;
	private int externalID;
	private String sampleName = "UnnamedSample";

	/**
	 * Creates a {@code Metadata} object with a pre-specified {@code externalId}
	 * 
	 * @param externalId usually is the ID recorded by the experimental setup.
	 */

	public Metadata(int externalId) {
		this.externalID = externalId;
	}

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
		setTestTemperature(temperature);
	}

	/**
	 * Retrieves the test temperature from this {@code Metadata}. If the value is
	 * not deemed sensible, it will return the default test temperature value (as
	 * per the XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.TEST_TEMPERATURE}, the value of which
	 *         is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getTestTemperature() {
		var defTest = def(TEST_TEMPERATURE);
		if (isValueSensible(defTest, testTemperature))
			return new NumericProperty(testTemperature, defTest);
		return null;
	}

	/**
	 * Sets the test temperature of this {@code Metadata} to {@code temperature}.
	 * <p>
	 * Checks the type of {@code temperature} corresponds to
	 * {@code NumericPropertyKeyword.TEST_TEMPERATURE}.
	 * 
	 * @param temperature
	 */

	public void setTestTemperature(NumericProperty temperature) {
		if (temperature.getType() != TEST_TEMPERATURE)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + temperature.getType());

		this.testTemperature = (double) temperature.getValue();
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
	 * Retrieves the sample thickness from this {@code Metadata}. If the value is
	 * not deemed sensible, it will return the default thickness value (as per the
	 * XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.THICKNESS}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getSampleThickness() {
		var defThickness = def(THICKNESS);
		if (isValueSensible(defThickness, sampleThickness))
			return new NumericProperty(sampleThickness, defThickness);
		return defThickness;
	}

	/**
	 * Sets the sample thickness of this {@code Metadata} to {@code thickness}
	 * 
	 * @param thickness the value of the thickness
	 */

	public void setSampleThickness(NumericProperty thickness) {
		if (thickness.getType() != THICKNESS)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + thickness.getType());

		this.sampleThickness = (double) thickness.getValue();
	}

	/**
	 * Retrieves the sample diameter from this {@code Metadata}. If the value is not
	 * deemed sensible, it will return the default diameter value (as per the XML
	 * specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.DIAMETER}, the value of which is deemed
	 *         sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getSampleDiameter() {
		var defDiameter = def(DIAMETER);
		if (isValueSensible(defDiameter, sampleDiameter))
			return new NumericProperty(sampleDiameter, defDiameter);
		return defDiameter;
	}

	/**
	 * Sets the sample diameter of this {@code Metadata} to {@code diameter}
	 * 
	 * @param diameter a {@code NumericProperty} representing the diameter
	 */

	public void setSampleDiameter(NumericProperty diameter) {
		if (diameter.getType() != DIAMETER)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + diameter.getType());

		this.sampleDiameter = (double) diameter.getValue();
	}

	/**
	 * Retrieves the pulse width from this {@code Metadata}. If the value is not
	 * deemed sensible, it will return the default diameter value (as per the XML
	 * specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.PULSE_WIDTH}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getPulseWidth() {
		var defPulseWidth = def(PULSE_WIDTH);
		if (isValueSensible(defPulseWidth, pulseWidth))
			return new NumericProperty(pulseWidth, defPulseWidth);
		return defPulseWidth;
	}

	/**
	 * Sets the pulse width of this {@code Metadata} to {@code pulseWidth}
	 * 
	 * @param pulseWidth a {@code NumericProperty} representing the pulse width
	 */

	public void setPulseWidth(NumericProperty pulseWidth) {
		if (pulseWidth.getType() != PULSE_WIDTH)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + pulseWidth.getType());

		this.pulseWidth = (double) pulseWidth.getValue();
	}

	/**
	 * Retrieves the spot diameter from this {@code Metadata}. If the value is not
	 * deemed sensible, it will return the default spot diameter value (as per the
	 * XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.SPOT_DIAMETER}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getSpotDiameter() {
		var defSpotDiameter = def(SPOT_DIAMETER);
		if (isValueSensible(defSpotDiameter, spotDiameter))
			return new NumericProperty(spotDiameter, defSpotDiameter);
		return defSpotDiameter;
	}

	/**
	 * Sets the spot diameter of this {@code Metadata} to {@code spotDiameter}
	 * 
	 * @param spotDiameter a {@code NumericProperty} representing the spot diameter
	 */

	public void setSpotDiameter(NumericProperty spotDiameter) {
		if (spotDiameter.getType() != SPOT_DIAMETER)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + spotDiameter.getType());

		this.spotDiameter = (double) spotDiameter.getValue();
	}

	/**
	 * Retrieves the absorbed energy from this {@code Metadata}. If the value is not
	 * deemed sensible, it will return the default absorbed energy value (as per the
	 * XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.ABSORBED_ENERGY}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getAbsorbedEnergy() {
		var defAbsEnergy = def(LASER_ENERGY);
		if (isValueSensible(defAbsEnergy, laserEnergy))
			return new NumericProperty(laserEnergy, defAbsEnergy);
		return defAbsEnergy;
	}

	/**
	 * Sets the absorbed energy of this {@code Metadata} to {@code absorbedEnergy}
	 * 
	 * @param absorbedEnergy a {@code NumericProperty} representing the absorbed
	 *                       energy
	 */

	public void setAbsorbedEnergy(NumericProperty absorbedEnergy) {
		if (absorbedEnergy.getType() != LASER_ENERGY)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + absorbedEnergy.getType());

		this.laserEnergy = (double) absorbedEnergy.getValue();
	}

	/**
	 * Retrieves the detector gain from this {@code Metadata}. If the value is not
	 * deemed sensible, it will return the default detector gain value (as per the
	 * XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.DETECTOR_GAIN}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getDetectorGain() {
		var defDetectorGain = def(DETECTOR_GAIN);
		if (isValueSensible(defDetectorGain, detectorGain))
			return new NumericProperty(detectorGain, defDetectorGain);
		return defDetectorGain;
	}

	/**
	 * Sets the detector gain (amplification factor) of this {@code Metadata} to
	 * {@code detectorGain}
	 * 
	 * @param detectorGain a {@code NumericProperty} representing the detector gain
	 */

	public void setDetectorGain(NumericProperty detectorGain) {
		if (detectorGain.getType() != DETECTOR_GAIN)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + detectorGain.getType());

		this.detectorGain = ((Number) detectorGain.getValue()).intValue();
	}

	/**
	 * Retrieves the detector iris (aperture) from this {@code Metadata}. If the
	 * value is not deemed sensible, it will return the default detector iris value
	 * (as per the XML specification).
	 * 
	 * @return a {@code NumericProperty} of the type
	 *         {@code NumericPropertyKeyword.DETECTOR_IRIS}, the value of which is
	 *         deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number)
	 */

	public NumericProperty getDetectorIris() {
		var defIris = def(DETECTOR_IRIS);
		if (isValueSensible(defIris, detectorIris))
			return new NumericProperty(detectorIris, defIris);
		return defIris;
	}

	/**
	 * Sets the detector iris (aperture) of this {@code Metadata} to
	 * {@code detectorIris}
	 * 
	 * @param detectorIris a {@code NumericProperty} representing the detector iris
	 */

	public void setDetectorIris(NumericProperty detectorIris) {
		if (detectorIris.getType() != DETECTOR_IRIS)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + detectorIris.getType());

		this.detectorIris = ((Number) detectorIris.getValue()).intValue();
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
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case TEST_TEMPERATURE:
			setTestTemperature(property);
			break;
		case THICKNESS:
			setSampleThickness(property);
			break;
		case DIAMETER:
			setSampleDiameter(property);
			break;
		case PULSE_WIDTH:
			setPulseWidth(property);
			break;
		case SPOT_DIAMETER:
			setSpotDiameter(property);
			break;
		case LASER_ENERGY:
			setAbsorbedEnergy(property);
			break;
		case DETECTOR_GAIN:
			setDetectorGain(property);
			break;
		case DETECTOR_IRIS:
			setDetectorIris(property);
			break;
		default:
			throw new IllegalArgumentException("Illegal type passed to method: " + property);
		}
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
		sb.append("[Sample " + sampleName + " with external ID " + externalID + "]");
		sb.append(lineSeparator());
		sb.append(lineSeparator());

		var data = this.data();

		data.forEach(entry -> {
			sb.append(entry.toString());
			sb.append(lineSeparator());

		});

		return sb.toString();

	}

	@Override
	public Identifier identify() {
		return getParent() == null ? externalIdentifier(externalID) : super.identify();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (o instanceof Metadata) {

			var other = (Metadata) o;

			if (other.getExternalID() != this.getExternalID())
				return false;

			if (!sampleName.equals(other.getSampleName()))
				return false;

		}

		return false;

	}

}