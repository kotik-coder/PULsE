package pulse.input;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pulse.input.Pulse.PulseShape;
import pulse.ui.Messages;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.util.Saveable;

import static pulse.properties.NumericPropertyKeyword.*;

public class Metadata extends PropertyHolder implements Reflexive, Saveable {
	
	private double testTemperature, sampleThickness, sampleDiameter, pulseWidth, spotDiameter, absorbedEnergy;
	private int detectorGain, detectorIris;
	private PulseShape pulseShape = PulseShape.RECTANGULAR;
	private int externalID;
	private String sampleName = "UnnamedSample";

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
	
	public Metadata(int externalId) { 
		this.externalID = externalId;
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>(9);
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(THICKNESS));
		list.add(NumericProperty.def(DIAMETER));
		list.add(NumericProperty.def(PULSE_WIDTH));
		list.add(NumericProperty.def(SPOT_DIAMETER));
		list.add(NumericProperty.def(ABSORBED_ENERGY));
		list.add(NumericProperty.def(DETECTOR_GAIN));
		list.add(NumericProperty.def(DETECTOR_IRIS));
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
		NumericProperty defTest = NumericProperty.def(TEST_TEMPERATURE);
		if(NumericProperty.isValueSensible(defTest, testTemperature))
			return new NumericProperty(testTemperature, defTest);
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
		NumericProperty defThickness = NumericProperty.def(THICKNESS);
		if(NumericProperty.isValueSensible(defThickness, sampleThickness))
			return new NumericProperty(sampleThickness, defThickness); 
		return defThickness;
	}

	public void setSampleThickness(NumericProperty thickness) {
		this.sampleThickness = (double)thickness.getValue();
	}

	public NumericProperty getSampleDiameter() {
		NumericProperty defDiameter = NumericProperty.def(DIAMETER);
		if(NumericProperty.isValueSensible(defDiameter, sampleDiameter))
			return new NumericProperty(sampleDiameter, defDiameter);
		return defDiameter;
	}

	public void setSampleDiameter(NumericProperty diameter) {
		this.sampleDiameter = (double) diameter.getValue();
	}

	public NumericProperty getPulseWidth() {
		NumericProperty defPulseWidth = NumericProperty.def(PULSE_WIDTH);
		if(NumericProperty.isValueSensible(defPulseWidth, pulseWidth))
			return new NumericProperty(pulseWidth, defPulseWidth);
		return defPulseWidth;
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double) pulseWidth.getValue();
	}

	public NumericProperty getSpotDiameter() {
		NumericProperty defSpotDiameter = NumericProperty.def(SPOT_DIAMETER);
		if(NumericProperty.isValueSensible(defSpotDiameter, spotDiameter))
			return new NumericProperty(spotDiameter, defSpotDiameter);
		return defSpotDiameter;
	}

	public void setSpotDiameter(NumericProperty spotDiameter) {
		this.spotDiameter = (double) spotDiameter.getValue();
	}

	public NumericProperty getAbsorbedEnergy() {
		NumericProperty defAbsEnergy = NumericProperty.def(ABSORBED_ENERGY);
		if(NumericProperty.isValueSensible(defAbsEnergy, absorbedEnergy))
			return new NumericProperty(absorbedEnergy, defAbsEnergy);
		return defAbsEnergy;
	}

	public void setAbsorbedEnergy(NumericProperty absorbedEnergy) {
		this.absorbedEnergy = (double) absorbedEnergy.getValue();
	}

	public NumericProperty getDetectorGain() {
		NumericProperty defDetectorGain = NumericProperty.def(DETECTOR_GAIN);
		if(NumericProperty.isValueSensible(defDetectorGain, detectorGain))
			return new NumericProperty(detectorGain, defDetectorGain);
		return defDetectorGain;
	}

	public void setDetectorGain(NumericProperty detectorGain) {
		this.detectorGain = ((Number) detectorGain.getValue()).intValue();
	}

	public NumericProperty getDetectorIris() {
		NumericProperty defIris = NumericProperty.def(DETECTOR_IRIS);
		if(NumericProperty.isValueSensible(defIris, detectorIris))
			return new NumericProperty(detectorIris, defIris);
		return defIris;
	}

	public void setDetectorIris(NumericProperty detectorIris) {
		this.detectorIris = ((Number)detectorIris.getValue()).intValue();
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
		case ABSORBED_ENERGY 	: setAbsorbedEnergy(property); 	break;
		case DETECTOR_GAIN 		: setDetectorGain(property); 	break;
		case DETECTOR_IRIS 		: setDetectorIris(property); 	break;
		}
	}
		
}
