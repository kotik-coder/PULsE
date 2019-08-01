package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import pulse.input.Metadata;
import pulse.input.Pulse.PulseShape;
import pulse.properties.NumericProperty;
import pulse.properties.Property;

public class MetaFileReader implements AbstractReader {	
	
	private static MetaFileReader instance = new MetaFileReader();
	
	private final static double CELSIUS_TO_KELVIN = 273;
	
	private MetaFileReader() {}

	public static MetaFileReader getInstance() {
		return instance;
	}
	
	public void populateMetadata(File file, Metadata met) throws IOException {
			if(file == null)
				throw new NullPointerException(Messages.getString("MetaFileReader.1")); //$NON-NLS-1$			
			
			BufferedReader reader = new BufferedReader(new FileReader(file));

			Map<Integer,String> metaFormat = new HashMap<Integer,String>();
			metaFormat.put(0, "ID"); //id must always be the first entry of a row
			
			List<String> tokens = new LinkedList<String>();
							
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				
				StringTokenizer st = new StringTokenizer(line);
				
				tokens.clear();
				for( ; st.hasMoreTokens() ; )
					tokens.add(st.nextToken());
				int size = tokens.size();
				
				if(size < 1)
					continue;				
				
				if(size == 2) {
					
					Object[][] val = new Object[][]{ {tokens.get(0), tokens.get(1) } };
								
					switch( tokens.get(0) ) {					
					case "Sample" : 
						met.setSampleName(tokens.get(1));
						break;
					default :
						try {
							translate(val, met);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							System.err.println("Error changing property in Metadata object. Details below.");
							e.printStackTrace();
						}
					break;
					
					}
					
				} else {
					
					if(tokens.get(0).equalsIgnoreCase( metaFormat.get(0) )) {
						
						for(int i = 1; i < size; i++) 
							metaFormat.put(i, tokens.get(i));
						
					} 
					
					else {						
						
						if( Math.abs( Integer.valueOf(tokens.get(0)) - met.getExternalID() ) > 0.5 ) 							
							continue;												
												
						Object[][] values = new Object[size-1][2];
						
						for(int i = 1; i < size; i++) {
							
							values[i-1][0] = metaFormat.get(i);
							values[i-1][1] = tokens.get(i);
							
						}
						
						try {
							translate(values, met);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							System.err.println("Error changing property in Metadata object. Details below.");
							e.printStackTrace();
						}
						break;
						
					}
					
				}																
				
			}		
				
			reader.close();						
					
	}	
	
	private void translate(Object[][] data, Metadata met) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Object[][] metaData = met.data();
		double tmp, val; 
		Property property;
		NumericProperty numProperty;
		
		for(int i = 0; i < metaData.length; i++) {
			
			for(int j = 0; j < data.length; j++) {
				
				if(! (metaData[i][1] instanceof Property) )
					continue;
				
				property = (Property) metaData[i][1];
				
				if( property.getSimpleName().equals( data[j][0] ) )  {
				
					if(property instanceof NumericProperty)  {								
								
							tmp = Double.valueOf( (String) data[j][1] );
							if(property.getSimpleName().equals(
									NumericProperty.DEFAULT_T.getSimpleName())
									)
								tmp += CELSIUS_TO_KELVIN;
							numProperty = (NumericProperty) property;
							val = tmp / (numProperty.getDimensionFactor() ).doubleValue();
								
							if(! numProperty.isValueSensible(val))
								continue;
								
							numProperty.setValue( val );
							met.updateProperty( property );										
							
					}
					
					else {
						
						if(property instanceof PulseShape)							
							met.updateProperty( PulseShape.valueOf( (String) data[j][1]) );
						
						
					}
					
				} 				
				
			}
			
		}
		
	}
	
	@Override
	public String getSupportedExtension() {
		return Messages.getString("MetaFileReader.0"); //$NON-NLS-1$
	}

}
