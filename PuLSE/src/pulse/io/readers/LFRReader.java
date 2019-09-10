package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class LFRReader implements CurveReader {
	
	private static CurveReader instance = new LFRReader();

	private final static double CONVERSION_TO_KELVIN = 273;
	
	private LFRReader() {}

	@Override
	public String getSupportedExtension() {
		return Messages.getString("LFRReader.0"); //$NON-NLS-1$
	}

	@Override 
	public List<ExperimentalData> read(File file) throws IOException {
		if(file == null)
			throw new NullPointerException(Messages.getString("LFRReader.1"));				 //$NON-NLS-1$
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		final String directory = file.getAbsoluteFile().getParent();
		String delims		  = Messages.getString("LFRReader.2"); //$NON-NLS-1$
		String stringSplitter = Messages.getString("LFRReader.3");
		StringTokenizer tokenizer;

		//skip two first lines
		reader.readLine();
		reader.readLine();
		
		List<String> fileNames = new LinkedList<String>();
		HashMap<String,Metadata> fileTempMap = new HashMap<String,Metadata>();
		String tmp;

		for(String line = reader.readLine(); line != null; line = reader.readLine()) {
			tokenizer = new StringTokenizer(line);
			int id = Integer.parseInt(tokenizer.nextToken(delims)); //id
			
			tmp = tokenizer.nextToken(delims);
			tmp = tmp.split(stringSplitter)[0]; //write file names without extension
			
			fileNames.add(tmp); //fileName
		
			tokenizer.nextToken(delims); //sample id
			NumericProperty temperature = NumericProperty.derive(NumericPropertyKeyword.TEST_TEMPERATURE, 
					Double.parseDouble(tokenizer.nextToken()) + CONVERSION_TO_KELVIN); //test temperature
			
			fileTempMap.put( tmp, new Metadata(temperature, id) ); //assign metadata object with external id and temperature
			
		}
		
		reader.close();
		
		List<ExperimentalData> curves = new LinkedList<ExperimentalData>();
		
		String[] nameAndExtension;
		String toReplace = Messages.getString("LFRReader.5");
		
		for(File f : new File(directory).listFiles()) {
			
			nameAndExtension = f.getName().split(stringSplitter); //$NON-NLS-1$
			nameAndExtension[0] = nameAndExtension[0].replaceAll(toReplace, Messages.getString(" ")); //$NON-NLS-1$ //$NON-NLS-2$
			
			for(String name : fileNames) {
				if(nameAndExtension[0].equalsIgnoreCase(name.replaceAll(toReplace, Messages.getString(" ")))) //$NON-NLS-1$ //$NON-NLS-2$
				{ //add only those curves listed in the master file
					curves.add( readSingleCurve(f, fileTempMap.get(name)) );
					break;
				}
			}
			
		}	
		
		return CurveReader.sort( curves );	
				
	}
	
	public ExperimentalData readSingleCurve(File file, Metadata metadata) throws IOException {
		if(file == null)
			throw new NullPointerException(Messages.getString("LFRReader.9"));				 //$NON-NLS-1$
		
		ExperimentalData curve = new ExperimentalData();
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String delims = Messages.getString("LFRReader.10"); //$NON-NLS-1$
		StringTokenizer tokenizer;
		
		curve.setMetadata(metadata);
		curve.clear();
		reader.readLine(); //skip first line
		
		double time, temp;
		
		for(String line = reader.readLine(); line != null; line = reader.readLine()) {
			tokenizer = new StringTokenizer(line);
			
			time = Double.parseDouble(tokenizer.nextToken(delims))*1E-3;
			temp = Double.parseDouble(tokenizer.nextToken(delims));
			
			curve.add(time, temp);
			
		}
		
		reader.close();
	
		return curve;	
				
	}
	
	public static CurveReader getInstance() {
		return instance;
	}

}