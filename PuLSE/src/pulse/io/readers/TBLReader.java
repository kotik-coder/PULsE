package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import pulse.input.PropertyCurve;

public class TBLReader implements PropertyTableReader {
	
	private static PropertyTableReader instance = new TBLReader();
	
	private TBLReader() {}

	@Override
	public String getSupportedExtension() {
		return Messages.getString("TBLReader.0"); //$NON-NLS-1$
	}

	public static PropertyTableReader getInstance() {
		return instance;
	}
	
	@Override
	public PropertyCurve read(File file) throws IOException {
			if(file == null)
				throw new NullPointerException(Messages.getString("TBLReader.1")); //$NON-NLS-1$
			
			PropertyCurve curve = new PropertyCurve();
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String delims = Messages.getString("TBLReader.2"); //$NON-NLS-1$
			StringTokenizer tokenizer;
			
			curve.setMinTemperature(1e7);
			curve.setMaxTemperature(-1);
			double temp;
			
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				tokenizer = new StringTokenizer(line);
				temp = Double.parseDouble(tokenizer.nextToken(delims));
				curve.addTemperature(temp);
				if(temp < curve.getMinTemperature())
					curve.setMinTemperature(temp);
				else if(temp > curve.getMaxTemperature())
					curve.setMaxTemperature(temp);
				curve.addProperty(Double.parseDouble(tokenizer.nextToken(delims)));
			}
			
			reader.close();
			
			return curve;
					
	}


}
