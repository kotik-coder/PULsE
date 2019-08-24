package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import pulse.input.InterpolationDataset;
import pulse.ui.Messages;
import pulse.util.DataEntry;

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
	public InterpolationDataset read(File file) throws IOException {
			if(file == null)
				throw new NullPointerException(Messages.getString("TBLReader.1")); //$NON-NLS-1$
			
			InterpolationDataset curve = new InterpolationDataset();
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			String delims = Messages.getString("TBLReader.2"); //$NON-NLS-1$
			StringTokenizer tokenizer;
			
			for(String line = reader.readLine(); line != null; line = reader.readLine()) {
				tokenizer = new StringTokenizer(line);
				curve.add(
						new DataEntry<Double,Double>(
						Double.parseDouble(tokenizer.nextToken(delims)), 
						Double.parseDouble(tokenizer.nextToken(delims)))
						);
			}
			
			reader.close();
			
			return curve;
					
	}


}
