package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.properties.NumericProperty;

public class DATReader implements CurveReader {
	
	private static CurveReader instance = new DATReader();
	
	private final static double CONVERSION_TO_KELVIN = 273.15;
	
	private DATReader() {}

	@Override
	public String getSupportedExtension() {
		return Messages.getString("DATReader.0"); //$NON-NLS-1$
	}

	@Override
	public List<ExperimentalData> read(File file) throws IOException {
		if(file == null)
			throw new NullPointerException(Messages.getString("DATReader.1"));				 //$NON-NLS-1$
		
		ExperimentalData curve = new ExperimentalData();

		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		double T = Double.parseDouble(reader.readLine()) + CONVERSION_TO_KELVIN;
		Metadata met = new Metadata(-1);
		met.setTestTemperature( new NumericProperty(T, NumericProperty.DEFAULT_T) );
		curve.setMetadata(met);
		
		double time, temp;	

		String delims = Messages.getString("DATReader.2"); //$NON-NLS-1$
		StringTokenizer tokenizer;
		
		for(String line = reader.readLine(); line != null; line = reader.readLine()) {
			tokenizer = new StringTokenizer(line, delims);
			time = Double.parseDouble(tokenizer.nextToken());
			temp = Double.parseDouble(tokenizer.nextToken());
			curve.add(time, temp);
			tokenizer = null;
		}
		
		curve.updateCount();
		
		reader.close();
		
		return new ArrayList<ExperimentalData>(Arrays.asList(curve));		
				
	}
	
	public static CurveReader getInstance() {
		return instance;
	}

}
