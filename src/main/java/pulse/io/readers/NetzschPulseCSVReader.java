package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

import pulse.problem.laser.NumericPulseData;
import pulse.ui.Messages;

public class NetzschPulseCSVReader implements PulseDataReader {

	private static PulseDataReader instance = new NetzschPulseCSVReader();
	
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
		
		NumericPulseData data;
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			
			int shotId = NetzschCSVReader.determineShotID(reader, file);
			data = new NumericPulseData(shotId);
						
			var pulseLabel = NetzschCSVReader.findLineByLabel(reader, PULSE, NetzschCSVReader.delims);
			
			if(pulseLabel == null) {
				System.err.println("Skipping " + file.getName());
				return null;
			}
			
			reader.readLine();
			NetzschCSVReader.populate(data, reader);
			
		}

		return data;

	}
	
	/*
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
	*/

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