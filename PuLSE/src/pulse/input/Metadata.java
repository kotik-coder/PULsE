package pulse.input;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import pulse.problem.statements.Pulse.TemporalShape;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.util.Saveable;

import static pulse.properties.NumericPropertyKeyword.*;

/**
 * <p>{@code Metadata} is the information relating to a specific experiment, which is required
 * to accurately process the {@code ExperimentalData}. It is used to populate the 
 * associated {@code Problem}, {@code DifferenceScheme}, and the fitting range of the
 * {@code ExperimentalData}.</p>
 *
 */

public class Metadata extends PropertyHolder implements Reflexive, Saveable {
	
	private double testTemperature, sampleThickness, sampleDiameter, pulseWidth, spotDiameter, laserEnergy;
	private int detectorGain, detectorIris;
	private TemporalShape pulseShape = TemporalShape.RECTANGULAR;
	private int externalID;
	private String sampleName = "UnnamedSample";
	
	/**
	 * Creates a {@code Metadata} object with a pre-specified {@code externalId}
	 * @param externalId usually is the ID recorded by the experimental setup.
	 */
	
	public Metadata(int externalId) { 
		this.externalID = externalId;
	}
	
	/**
	 * Creates a {@code Metadata} with the specified parameters.
	 * @param temperature the NumericProperty with the type {@code NumericPropertyKeyword.TEST_TEMPERATURE}
	 * @param externalId an integer, specifying the external ID recorded by the experimental setup.
	 */
	
	public Metadata(NumericProperty temperature, int externalId) {
		setExternalID(externalId);
		setTestTemperature(temperature);
	}
	
	/**
	 * Retrieves the test temperature from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default test temperature value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.TEST_TEMPERATURE}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getTestTemperature() {
		NumericProperty defTest = NumericProperty.def(TEST_TEMPERATURE);
		if(NumericProperty.isValueSensible(defTest, testTemperature))
			return new NumericProperty(testTemperature, defTest);
		return null;
	}
	
	/**
	 * Sets the test temperature of this {@code Metadata} to {@code temperature}.
	 * <p> Checks the type of {@code temperature} corresponds to {@code NumericPropertyKeyword.TEST_TEMPERATURE}.
	 * @param temperature
	 */

	public void setTestTemperature(NumericProperty temperature) {
		if(temperature.getType() != NumericPropertyKeyword.TEST_TEMPERATURE)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + temperature.getType());
		
		this.testTemperature = (double) temperature.getValue();					
	}
	
	/**
	 * Gets the external ID usually originating from the original exported experimental files.
	 * Note this is not a {@code NumericProperty}  
	 * @return an integer, representing the external ID
	 */

	public int getExternalID() {
		return externalID;
	}
	
	/**
	 * Sets the external ID in this {@code Metadata} to {@code externalId}
	 * @param externalId the value of the external ID
	 */

	public void setExternalID(int externalId) {
		this.externalID = externalId;
	}
	
	/**
	 * Retrieves the sample thickness from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default thickness value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.THICKNESS}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getSampleThickness() {
		NumericProperty defThickness = NumericProperty.def(THICKNESS);
		if(NumericProperty.isValueSensible(defThickness, sampleThickness))
			return new NumericProperty(sampleThickness, defThickness); 
		return defThickness;
	}
	
	/**
	 * Sets the sample thickness of this {@code Metadata} to {@code thickness}
	 * @param thickness the value of the thickness
	 */

	public void setSampleThickness(NumericProperty thickness) {
		if(thickness.getType() != NumericPropertyKeyword.THICKNESS)
				throw new IllegalArgumentException("Wrong type of NumericProperty: " + thickness.getType());
			
		this.sampleThickness = (double)thickness.getValue();
	}
	
	/**
	 * Retrieves the sample diameter from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default diameter value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.DIAMETER}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getSampleDiameter() {
		NumericProperty defDiameter = NumericProperty.def(DIAMETER);
		if(NumericProperty.isValueSensible(defDiameter, sampleDiameter))
			return new NumericProperty(sampleDiameter, defDiameter);
		return defDiameter;
	}
	
	/**
	 * Sets the sample diameter of this {@code Metadata} to {@code diameter}
	 * @param diameter a {@code NumericProperty} representing the diameter
	 */

	public void setSampleDiameter(NumericProperty diameter) {
		if(diameter.getType() != NumericPropertyKeyword.DIAMETER)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + diameter.getType());		

		this.sampleDiameter = (double) diameter.getValue();
	}
	
	/**
	 * Retrieves the pulse width from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default diameter value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.PULSE_WIDTH}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getPulseWidth() {
		NumericProperty defPulseWidth = NumericProperty.def(PULSE_WIDTH);
		if(NumericProperty.isValueSensible(defPulseWidth, pulseWidth))
			return new NumericProperty(pulseWidth, defPulseWidth);
		return defPulseWidth;
	}

	/**
	 * Sets the pulse width of this {@code Metadata} to {@code pulseWidth}
	 * @param pulseWidth a {@code NumericProperty} representing the pulse width
	 */
	
	public void setPulseWidth(NumericProperty pulseWidth) {
		if(pulseWidth.getType() != NumericPropertyKeyword.PULSE_WIDTH)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + pulseWidth.getType());		
		
		this.pulseWidth = (double) pulseWidth.getValue();
	}
	
	/**
	 * Retrieves the spot diameter from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default spot diameter value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.SPOT_DIAMETER}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getSpotDiameter() {
		NumericProperty defSpotDiameter = NumericProperty.def(SPOT_DIAMETER);
		if(NumericProperty.isValueSensible(defSpotDiameter, spotDiameter))
			return new NumericProperty(spotDiameter, defSpotDiameter);
		return defSpotDiameter;
	}
	
	/**
	 * Sets the spot diameter of this {@code Metadata} to {@code spotDiameter}
	 * @param spotDiameter a {@code NumericProperty} representing the spot diameter
	 */

	public void setSpotDiameter(NumericProperty spotDiameter) {
		if(spotDiameter.getType() != NumericPropertyKeyword.SPOT_DIAMETER)
			throw new IllegalArgumentException("Wrong type of NumericProperty: " + spotDiameter.getType());		
		
		this.spotDiameter = (double) spotDiameter.getValue();
	}
	
	/**
	 * Retrieves the absorbed energy from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default absorbed energy value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.ABSORBED_ENERGY}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getAbsorbedEnergy() {
		NumericProperty defAbsEnergy = NumericProperty.def(LASER_ENERGY);
		if(NumericProperty.isValueSensible(defAbsEnergy, laserEnergy))
			return new NumericProperty(laserEnergy, defAbsEnergy);
		return defAbsEnergy;
	}
	
	/**
	 * Sets the absorbed energy of this {@code Metadata} to {@code absorbedEnergy}
	 * @param absorbedEnergy a {@code NumericProperty} representing the absorbed energy
	 */

	public void setAbsorbedEnergy(NumericProperty absorbedEnergy) {
		if(absorbedEnergy.getType() != NumericPropertyKeyword.LASER_ENERGY)
			throw new IllegalArgumentException("Wrong type of NumericProperty: "
		+ absorbedEnergy.getType());		
		
		this.laserEnergy = (double) absorbedEnergy.getValue();
	}
	
	/**
	 * Retrieves the detector gain from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default detector gain value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.DETECTOR_GAIN}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getDetectorGain() {
		NumericProperty defDetectorGain = NumericProperty.def(DETECTOR_GAIN);
		if(NumericProperty.isValueSensible(defDetectorGain, detectorGain))
			return new NumericProperty(detectorGain, defDetectorGain);
		return defDetectorGain;
	}
	
	/**
	 * Sets the detector gain (amplification factor) of this {@code Metadata} to {@code detectorGain}
	 * @param detectorGain a {@code NumericProperty} representing the detector gain 
	 */

	public void setDetectorGain(NumericProperty detectorGain) {
		if(detectorGain.getType() != NumericPropertyKeyword.DETECTOR_GAIN)
			throw new IllegalArgumentException("Wrong type of NumericProperty: "
		+ detectorGain.getType());	
		
		this.detectorGain = ((Number) detectorGain.getValue()).intValue();
	}
	
	/**
	 * Retrieves the detector iris (aperture) from this {@code Metadata}. 
	 * If the value is not deemed sensible, it will return the default detector iris value (as per the XML specification).
	 * @return a {@code NumericProperty} of the type {@code NumericPropertyKeyword.DETECTOR_IRIS}, the value of which is deemed sensible.
	 * @see pulse.properties.NumericProperty.isValueSensible(NumericProperty,Number) 
	 */

	public NumericProperty getDetectorIris() {
		NumericProperty defIris = NumericProperty.def(DETECTOR_IRIS);
		if(NumericProperty.isValueSensible(defIris, detectorIris))
			return new NumericProperty(detectorIris, defIris);
		return defIris;
	}
	
	/**
	 * Sets the detector iris (aperture) of this {@code Metadata} to {@code detectorIris}
	 * @param detectorIris a {@code NumericProperty} representing the detector iris 
	 */

	public void setDetectorIris(NumericProperty detectorIris) {
		if(detectorIris.getType() != NumericPropertyKeyword.DETECTOR_IRIS)
			throw new IllegalArgumentException("Wrong type of NumericProperty: "
		+ detectorIris.getType());
		
		this.detectorIris = ((Number)detectorIris.getValue()).intValue();
	}
	
	/**
	 * Retrieves the pulse shape recorded in this {@code Metadata}
	 * @return a {@code PulseShape} object
	 */

	public TemporalShape getPulseShape() {
		return pulseShape;
	}
	
	/**
	 * Sets the pulse shape recorded in this {@code Metadata}
	 * @param pulseShape a {@code PulseShape} object
	 */

	public void setPulseShape(TemporalShape pulseShape) {
		this.pulseShape = pulseShape;
	}
	
	/**
	 * Retrieves the sample name. This name is used to create directories when
	 * exporting the data and also to fill the legend when plotting.
	 * @return a string representing the sample name
	 */

	public String getSampleName() {
		return sampleName;
	}
	
	/**
	 * Sets the sample name
	 * @param sampleName a string representing the sample name
	 */

	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	
	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		printHTML(fos);
	}
	
	private void printHTML(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		
		stream.print("<table>"); //$NON-NLS-1$
		stream.print("<tr>"); //$NON-NLS-1$
	
		final String METADATA_LABEL = "Metadata"; //$NON-NLS-1$
		final String VALUE_LABEL= "Value"; //$NON-NLS-1$
	
		stream.print("<html>");
       	stream.print("<td>"); stream.print(METADATA_LABEL + "\t"); stream.print("</td>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
       	stream.print("<td>");
       	stream.print(VALUE_LABEL + "\t"); 
       	stream.print("</td>"); 
       	
        stream.print("</tr>"); //$NON-NLS-1$

        stream.println(" "); //$NON-NLS-1$

        List<Property> data = data();
        
        data.forEach(entry -> {
        	stream.print("<tr>"); //$NON-NLS-1$
            
    		stream.print("<td>"); //$NON-NLS-1$
            stream.print(entry.getDescriptor(false)); //$NON-NLS-1$
            stream.print("</td><td>"); //$NON-NLS-1$
            stream.print(entry.formattedValue()); //$NON-NLS-1$
            //possible error typecast property -> object
            stream.print("</td>"); //$NON-NLS-1$
        
            stream.println("</tr>"); //$NON-NLS-1$
        });
        
        stream.print("</table>"); //$NON-NLS-1$
		stream.print("</html>");
        stream.close();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case TEST_TEMPERATURE 	: setTestTemperature(property); break;
		case THICKNESS 			: setSampleThickness(property); break;
		case DIAMETER 			: setSampleDiameter(property);	break; 
		case PULSE_WIDTH 		: setPulseWidth(property);		break;
		case SPOT_DIAMETER 		: setSpotDiameter(property); 	break;
		case LASER_ENERGY 	: setAbsorbedEnergy(property); 	break;
		case DETECTOR_GAIN 		: setDetectorGain(property); 	break;
		case DETECTOR_IRIS 		: setDetectorIris(property); 	break;
		default: 
			throw new IllegalArgumentException("Illegal type passed to method: " + property);			
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>(9);
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(THICKNESS));
		list.add(NumericProperty.def(DIAMETER));
		list.add(NumericProperty.def(PULSE_WIDTH));
		list.add(NumericProperty.def(SPOT_DIAMETER));
		list.add(NumericProperty.def(LASER_ENERGY));
		list.add(NumericProperty.def(DETECTOR_GAIN));
		list.add(NumericProperty.def(DETECTOR_IRIS));
		list.add(TemporalShape.RECTANGULAR);		
		return list;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Sample " + sampleName + " with external ID " + externalID + "]");
		sb.append(System.lineSeparator());
		sb.append(System.lineSeparator());
		
		List<Property> data = this.data();		
		
		data.forEach(entry -> 
		{								
			sb.append(entry.toString());
			sb.append(System.lineSeparator());
			
		});

		return sb.toString();
		
	}
		
}