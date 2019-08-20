package pulse.input;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pulse.input.Pulse.PulseShape;
import pulse.input.Messages;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.util.Saveable;

public class Metadata extends PropertyHolder implements Reflexive, Saveable {
	
	private double testTemperature, sampleThickness, sampleDiameter, pulseWidth, spotDiameter, absorbedEnergy;
	private int detectorGain, detectorIris;
	private PulseShape pulseShape = PulseShape.RECTANGULAR;
	private int externalID;
	private String sampleName = "UnnamedSample";
	
	public final static NumericProperty DETECTOR_GAIN = 
			new NumericProperty(NumericPropertyKeyword.DETECTOR_GAIN, 
					Messages.getString("Gain.Descriptor"), Messages.getString("Gain.Abbreviation"), 10, 1, 200, 10, 1, false); //$NON-NLS-1$
	public final static NumericProperty DETECTOR_IRIS = 
			new NumericProperty(NumericPropertyKeyword.DETECTOR_IRIS, 
					Messages.getString("Iris.Descriptor"), Messages.getString("Iris.Abbreviation"), 1, 1, 64, 1, 1, false); //$NON-NLS-1$
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Sample " + sampleName + " with external ID " + externalID + "]");
		sb.append(System.lineSeparator());
		
		List<Property> data = this.data();		
		
		data.forEach(entry -> 
		{					
			
			sb.append(entry.toString());
			sb.append(System.lineSeparator());
			
		});

		return sb.toString();
		
	}
	
	public Metadata(int externalId) { 
		this.externalID = externalId;
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>(9);
		list.add(NumericProperty.TEST_TEMPERATURE);
		list.add(NumericProperty.THICKNESS);
		list.add(NumericProperty.DIAMETER);
		list.add(NumericProperty.PULSE_WIDTH);
		list.add(NumericProperty.SPOT_DIAMETER);
		list.add(NumericProperty.ABSORBED_ENERGY);
		list.add(DETECTOR_GAIN);
		list.add(DETECTOR_IRIS);
		list.add(PulseShape.RECTANGULAR);		
		return list;
	}
	
	public Metadata(NumericProperty temperature) {
		this.testTemperature = (double) temperature.getValue();
	}
	
	public Metadata(NumericProperty temperature, int externalId) {
		setExternalID(externalId);
		setTestTemperature(temperature);
	}

	public NumericProperty getTestTemperature() {
		if(NumericProperty.isValueSensible(NumericProperty.TEST_TEMPERATURE, testTemperature))
			return new NumericProperty(testTemperature, NumericProperty.TEST_TEMPERATURE);
		return null;
	}

	public void setTestTemperature(NumericProperty temperature) {
		this.testTemperature = (double) temperature.getValue();					
	}

	public int getExternalID() {
		return externalID;
	}

	public void setExternalID(int externalId) {
		this.externalID = externalId;
	}

	public NumericProperty getSampleThickness() {
		if(NumericProperty.isValueSensible(NumericProperty.THICKNESS, sampleThickness))
			return new NumericProperty(sampleThickness, NumericProperty.THICKNESS); 
		return NumericProperty.THICKNESS;
	}

	public void setSampleThickness(NumericProperty thickness) {
		this.sampleThickness = (double)thickness.getValue();
	}

	public NumericProperty getSampleDiameter() {
		if(NumericProperty.isValueSensible(NumericProperty.DIAMETER, sampleDiameter))
			return new NumericProperty(sampleDiameter, NumericProperty.DIAMETER);
		return NumericProperty.DIAMETER;
	}

	public void setSampleDiameter(NumericProperty diameter) {
		this.sampleDiameter = (double) diameter.getValue();
	}

	public NumericProperty getPulseWidth() {
		if(NumericProperty.isValueSensible(NumericProperty.PULSE_WIDTH, pulseWidth))
			return new NumericProperty(pulseWidth, NumericProperty.PULSE_WIDTH);
		return NumericProperty.PULSE_WIDTH;
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double) pulseWidth.getValue();
	}

	public NumericProperty getSpotDiameter() {
		if(NumericProperty.isValueSensible(NumericProperty.SPOT_DIAMETER, spotDiameter))
			return new NumericProperty(spotDiameter, NumericProperty.SPOT_DIAMETER);
		return NumericProperty.SPOT_DIAMETER;
	}

	public void setSpotDiameter(NumericProperty spotDiameter) {
		this.spotDiameter = (double) spotDiameter.getValue();
	}

	public NumericProperty getAbsorbedEnergy() {
		if(NumericProperty.isValueSensible(NumericProperty.ABSORBED_ENERGY, absorbedEnergy))
			return new NumericProperty(absorbedEnergy, NumericProperty.ABSORBED_ENERGY);
		return NumericProperty.ABSORBED_ENERGY;
	}

	public void setAbsorbedEnergy(NumericProperty absorbedEnergy) {
		this.absorbedEnergy = (double) absorbedEnergy.getValue();
	}

	public NumericProperty getDetectorGain() {
		if(NumericProperty.isValueSensible(DETECTOR_GAIN, detectorGain))
			return new NumericProperty(detectorGain, DETECTOR_GAIN);
		return DETECTOR_GAIN;
	}

	public void setDetectorGain(NumericProperty detectorGain) {
		this.detectorGain = ((Number) detectorGain.getValue()).intValue();
	}

	public NumericProperty getDetectorIris() {
		if(NumericProperty.isValueSensible(DETECTOR_IRIS, detectorIris))
			return new NumericProperty(detectorIris, DETECTOR_IRIS);
		return DETECTOR_IRIS;
	}

	public void setDetectorIris(NumericProperty detectorIris) {
		this.detectorIris = (int) detectorIris.getValue();
	}

	public PulseShape getPulseShape() {
		return pulseShape;
	}

	public void setPulseShape(PulseShape pulseShape) {
		this.pulseShape = pulseShape;
	}

	public String getSampleName() {
		return sampleName;
	}

	public void setSampleName(String sampleName) {
		this.sampleName = sampleName;
	}
	
	@Override
	public void printData(FileOutputStream fos) {
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
		case ABSORBED_ENERGY 	: setAbsorbedEnergy(property); 	break;
		case DETECTOR_GAIN 		: setDetectorGain(property); 	break;
		case DETECTOR_IRIS 		: setDetectorIris(property); 	break;
		}
	}
		
}
