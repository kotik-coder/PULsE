package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import pulse.input.InterpolationDataset;
import pulse.ui.Messages;
import pulse.util.DataEntry;

/**
 * A {@code DatasetReader} capable of reading {@code .tbl} files.
 * <p> The format of these files is simply two tab-delimited numeric column. The first column represents the
 * 'keys', and the second -- the corresponding 'values'. </p>
 * <p>Specific heat capacity and density at different temperatures can be read as 
 * ASCII files with a .tbl suffix, where the first column is temperature 
 * (in degrees Celsius) and the second column is the specific heat capacity 
 * (in J kg<sup>-1</sup> K<sup>-1</sup>) or density (in kg m<sup>-3</sup>).
 * </p>
 * <p>Below is an example of a valid {@code .tbl} file:</p>
 * <pre><code>
 * -273	11000.00
 * 0	10959.84
 * 50	10943.82
 * 100	10927.80
 * 150	10911.77
 * 200	10895.73
 * 250	10879.67
 * 300	10863.56
 * 350	10847.41
 * 400	10831.21
 * 450	10814.93
 * 500	10798.58
 * 550	10782.14
 </code></pre>
 */

public class TBLReader implements DatasetReader {
	
	private static DatasetReader instance = new TBLReader();
	
	private TBLReader() {}

	/**
	 * @return a String equal to '{@code tbl}'
	 */
	
	@Override
	public String getSupportedExtension() {
		return Messages.getString("TBLReader.0");
	}
	
	/**
	 * As this class is built using a singleton pattern, only one instance exists.
	 * @return the static instance of this class
	 */

	public static DatasetReader getInstance() {
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
