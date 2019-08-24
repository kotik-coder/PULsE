package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import pulse.ui.Messages;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import pulse.input.Metadata;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.DataEntry;

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
					
					List<DataEntry<String,String>> val = new ArrayList<DataEntry<String,String>>();
					DataEntry<String,String> entry = new DataEntry<String,String>( tokens.get(0), tokens.get(1) );
					val.add(entry);
								
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
												
						List<DataEntry<String,String>> values = new ArrayList<DataEntry<String,String>>(size);						
												
						for(int i = 1; i < size; i++) 						
							values.add(new DataEntry<String,String>(metaFormat.get(i), tokens.get(i)));													
						
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
	
	private void translate(List<DataEntry<String,String>> data, Metadata met) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

		double tmp;		
		
		for(NumericProperty metaEntry : met.numericData()) {

			inner: for(DataEntry<String,String> dataEntry : data) {			
				
				if(!metaEntry.getType().toString().
				equalsIgnoreCase(dataEntry.getKey()))
					continue;

				tmp = Double.valueOf( dataEntry.getValue() );
				if(metaEntry.getType() == NumericPropertyKeyword.TEST_TEMPERATURE)
					tmp += CELSIUS_TO_KELVIN;
							
				tmp /= (metaEntry.getDimensionFactor() ).doubleValue();
								
				if( NumericProperty.isValueSensible(metaEntry, tmp)) {																
					try {
						met.updateProperty( instance, new NumericProperty(tmp, metaEntry) );
						break inner;
					} catch (IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
							
				}																	
				
			}
			
		}
					
		for(EnumProperty genericEntry : met.enumData())  
		{			
			inner: for(DataEntry<String,String> dataEntry : data) {
				
						if(dataEntry.getKey().equals(genericEntry.getClass().getSimpleName())) {
		
						try {
							met.updateProperty( instance, genericEntry.evaluate(dataEntry.getValue()) );
						}
						catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							//TODO Auto-generated catch block
							e.printStackTrace();
						}	
						
						break inner;
						
					}
				
				}
		
			}				
								
	}
	
	@Override
	public String getSupportedExtension() {
		return Messages.getString("MetaFileReader.0"); //$NON-NLS-1$
	}

}
