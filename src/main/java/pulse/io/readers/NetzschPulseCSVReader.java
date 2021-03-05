package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Objects;

import pulse.problem.laser.NumericPulseData;
import pulse.ui.Messages;

public class NetzschPulseCSVReader implements PulseDataReader {

	private static PulseDataReader instance = new NetzschPulseCSVReader();
	private final static double TO_SECONDS = 1E-3;
	
	private final static String SHOT_DATA = "Shot_data";
	private final static String PULSE = "Laser_pulse_data";
	
	private NetzschPulseCSVReader() {
		// intentionally blank
	}

	/**
	 * @return The supported extension ({@code .csv}).
	 */

	@Override
	public String getSupportedExtension() {
		return Messages.getString("NetzschCSVReader.0");
	}

	@Override
	public NumericPulseData read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("DATReader.1"));
		
		String delims = "[#();/°Cx%^]+";

		NumericPulseData data;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			
			String[] shotID = reader.readLine().split(delims);
			
			int shotId = -1;
			
			//check if first entry makes sense
			if(!shotID[shotID.length - 2].equalsIgnoreCase(SHOT_DATA))
				throw new IllegalArgumentException(file.getName() + " is not a recognised Netzch CSV file. First entry is: " + shotID[shotID.length - 2]);
			else 
				shotId = Integer.parseInt(shotID[shotID.length - 1]);
			
			data = new NumericPulseData(shotId);
			
			double time;
			double power;
			
			var pulseLabel = findLineByLabel(reader, PULSE, delims);
			
			if(pulseLabel == null) {
				System.err.println("Skipping " + file.getName());
				return null;
			}
			
			reader.readLine();
			
			String[] tokens;
			
			for (String line = reader.readLine(); line != null && !line.trim().isEmpty(); line = reader.readLine()) {
				tokens = line.split(delims);
				
				time = parseDoubleWithComma(tokens[0]) * TO_SECONDS;
				power = parseDoubleWithComma(tokens[1]);
				data.addPoint(time, power);
			}
			
		}

		return data;

	}
	
	private double parseDoubleWithComma(String s) {
		var format = NumberFormat.getInstance(Locale.GERMANY);
		try {
			return format.parse(s).doubleValue();
		} catch (ParseException e) {
			System.out.println("Couldn't parse double from: " + s);
			e.printStackTrace();
		}
		return Double.NaN;
	}
	
	private String findLineByLabel(BufferedReader reader, String label, String delims) throws IOException {
		
		String line = "";
		String[] tokens;
		
		//find sample temperature
		outer : for(line = reader.readLine(); line != null; line = reader.readLine()) {
			
			tokens = line.split(delims);
			
			for(String token : tokens) { 			
				if(token.equalsIgnoreCase(label))
					break outer;
			}
			
		}
		
		return line;
		
	}

	/**
	 * As this class uses the singleton pattern, only one instance is created using
	 * an empty no-argument constructor.
	 * 
	 * @return the single instance of this class.
	 */

	public static PulseDataReader getInstance() {
		return instance;
	}
	
}