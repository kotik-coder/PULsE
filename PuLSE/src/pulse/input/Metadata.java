package pulse.input;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import pulse.input.Pulse.PulseShape;
import pulse.input.Messages;
import pulse.properties.NumericProperty;
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
	
	public final static NumericProperty DEFAULT_DETECTOR_GAIN = new NumericProperty(Messages.getString("Metadata.6"), 10, 1, 200, 10, 1, false); //$NON-NLS-1$
	public final static NumericProperty DEFAULT_DETECTOR_IRIS = new NumericProperty(Messages.getString("Metadata.7"), 1, 1, 64, 1, 1, false); //$NON-NLS-1$
	
	private final static String EXPORT_EXTENSION = ".html";
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[Sample " + sampleName + " with external ID " + externalID + "]");
		sb.append(System.lineSeparator());
		
		Object[][] data = this.data();		
		
		for(int i = 0; i < data.length; i++) {
			sb.append( 
					data[i][1].toString() 
							);
			if(i+1 < data.length)
				sb.append(";");
			else
				sb.append(".");
			sb.append(System.lineSeparator());
		}
		
		return sb.toString();
		
	}
	
	public Metadata(int externalId) { 
		this.externalID = externalId; 
	}
	
	/**
	 * COMMENTED LINE _ DELETE!
	 */
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(9);
		map.put(NumericProperty.DEFAULT_T.getSimpleName(), Messages.getString("Problem.0")); //$NON-NLS-1$
		map.put(NumericProperty.DEFAULT_THICKNESS.getSimpleName(), Messages.getString("Problem.3")); //$NON-NLS-1$
		map.put(NumericProperty.DEFAULT_DIAMETER.getSimpleName(), Messages.getString("SecondDimensionData.2")); //$NON-NLS-1$
		map.put(NumericProperty.DEFAULT_PULSE_WIDTH.getSimpleName(), Messages.getString("Pulse.5")); //$NON-NLS-1$
		map.put(NumericProperty.DEFAULT_SPOT_DIAMETER.getSimpleName(), Messages.getString("Pulse.6")); //$NON-NLS-1$
		map.put(NumericProperty.DEFAULT_QABS.getSimpleName(), Messages.getString("NonlinearProblem.0")); //$NON-NLS-1$
		map.put(DEFAULT_DETECTOR_GAIN.getSimpleName(), Messages.getString("Metadata.4")); //$NON-NLS-1$
		map.put(DEFAULT_DETECTOR_IRIS.getSimpleName(), Messages.getString("Metadata.5")); //$NON-NLS-1$
		map.put(PulseShape.RECTANGULAR.getSimpleName(), Messages.getString("Pulse.4")); //$NON-NLS-1$
		return map;
	}
	
	public Metadata(NumericProperty temperature) {
		this.testTemperature = (double) temperature.getValue();
	}
	
	public Metadata(NumericProperty temperature, int externalId) {
		setExternalID(externalId);
		setTestTemperature(temperature);
	}

	public NumericProperty getTestTemperature() {
		if(NumericProperty.DEFAULT_T.isValueSensible(testTemperature))
			return new NumericProperty(testTemperature, NumericProperty.DEFAULT_T);
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
		if(NumericProperty.DEFAULT_THICKNESS.isValueSensible(sampleThickness))
			return new NumericProperty(sampleThickness, NumericProperty.DEFAULT_THICKNESS); 
		return NumericProperty.DEFAULT_THICKNESS;
	}

	public void setSampleThickness(NumericProperty thickness) {
		this.sampleThickness = (double)thickness.getValue();
	}

	public NumericProperty getSampleDiameter() {
		if(NumericProperty.DEFAULT_DIAMETER.isValueSensible(sampleDiameter))
			return new NumericProperty(sampleDiameter, NumericProperty.DEFAULT_DIAMETER);
		return NumericProperty.DEFAULT_DIAMETER;
	}

	public void setSampleDiameter(NumericProperty diameter) {
		this.sampleDiameter = (double) diameter.getValue();
	}

	public NumericProperty getPulseWidth() {
		if(NumericProperty.DEFAULT_PULSE_WIDTH.isValueSensible(pulseWidth))
			return new NumericProperty(pulseWidth, NumericProperty.DEFAULT_PULSE_WIDTH);
		return NumericProperty.DEFAULT_PULSE_WIDTH;
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double) pulseWidth.getValue();
	}

	public NumericProperty getSpotDiameter() {
		if(NumericProperty.DEFAULT_SPOT_DIAMETER.isValueSensible(spotDiameter))
			return new NumericProperty(spotDiameter, NumericProperty.DEFAULT_SPOT_DIAMETER);
		return NumericProperty.DEFAULT_SPOT_DIAMETER;
	}

	public void setSpotDiameter(NumericProperty spotDiameter) {
		this.spotDiameter = (double) spotDiameter.getValue();
	}

	public NumericProperty getAbsorbedEnergy() {
		if(NumericProperty.DEFAULT_QABS.isValueSensible(absorbedEnergy))
			return new NumericProperty(absorbedEnergy, NumericProperty.DEFAULT_QABS);
		return NumericProperty.DEFAULT_QABS;
	}

	public void setAbsorbedEnergy(NumericProperty absorbedEnergy) {
		this.absorbedEnergy = (double) absorbedEnergy.getValue();
	}

	public NumericProperty getDetectorGain() {
		if(DEFAULT_DETECTOR_GAIN.isValueSensible(detectorGain))
			return new NumericProperty(detectorGain, DEFAULT_DETECTOR_GAIN);
		return DEFAULT_DETECTOR_GAIN;
	}

	public void setDetectorGain(NumericProperty detectorGain) {
		this.detectorGain = (int) detectorGain.getValue();
	}

	public NumericProperty getDetectorIris() {
		if(DEFAULT_DETECTOR_IRIS.isValueSensible(detectorIris))
			return new NumericProperty(detectorIris, DEFAULT_DETECTOR_IRIS);
		return DEFAULT_DETECTOR_IRIS;
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
		
       	stream.print("<td>"); stream.print(METADATA_LABEL + "\t"); stream.print("</td>"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
       	stream.print("<td>");
       	stream.print(VALUE_LABEL + "\t"); 
       	stream.print("</td>"); 
       	
        stream.print("</tr>"); //$NON-NLS-1$

        stream.println(" "); //$NON-NLS-1$

        Object[][] data = data();
        
        for (int i = 0; i < data.length; i++) {
        	stream.print("<tr>"); //$NON-NLS-1$
            
        		stream.print("<td>"); //$NON-NLS-1$
                stream.print(data[i][0]); //$NON-NLS-1$
                stream.print("</td><td>"); //$NON-NLS-1$
                stream.print( ( (Property) data[i][1]).formattedValue()); //$NON-NLS-1$
                stream.print("</td>"); //$NON-NLS-1$
            
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
        stream.print("</table>"); //$NON-NLS-1$
        stream.close();
        
	}
		
}
