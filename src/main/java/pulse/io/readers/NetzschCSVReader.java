package pulse.io.readers;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.input.Range;
import pulse.ui.Messages;

public class NetzschCSVReader implements CurveReader {

	private static CurveReader instance = new NetzschCSVReader();
	private final static double TO_KELVIN = 273;
	private final static double TO_SECONDS = 1E-3;
	
	private final static String SAMPLE_TEMPERATURE = "Sample_temperature";
	private final static String SHOT_DATA = "Shot_data";
	private final static String DETECTOR = "DETECTOR";
	
	private NetzschCSVReader() {
		// intentionally blank
	}

	/**
	 * @return The supported extension ({@code .csv}).
	 */

	@Override
	public String getSupportedExtension() {
		return Messages.getString("NetzschCSVReader.0");
	}

	/**
	 * <p>
	 * This will return a single {@code ExperimentalData}, which stores all the
	 * information available in the {@code file}, wrapped in a {@code List} object
	 * with the size of unity. In addition to the time-temperature data loaded
	 * directly into the {@code ExperimentalData} lists, a {@code Metadata} object
	 * will be created for the {@code ExperimentalData} and will store the test
	 * temperature declared in {@code file}.
	 * 
	 * @param file a '{@code .dat}' file, which conforms to the respective format.
	 * @return a single {@code ExperimentalData} wrapped in a {@code List} with the
	 *         size of unity.
	 */

	@Override
	public List<ExperimentalData> read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("DATReader.1"));

		ExperimentalData curve = new ExperimentalData();
		
		String delims = "[#();/°Cx%^]+";

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			
			String[] shotID = reader.readLine().split(delims);
			
			int shotId = -1;
			
			//check if first entry makes sense
			if(!shotID[shotID.length - 2].equalsIgnoreCase(SHOT_DATA))
				throw new IllegalArgumentException(file.getName() + " is not a recognised Netzch CSV file. First entry is: " + shotID[shotID.length - 2]);
			else 
				shotId = Integer.parseInt(shotID[shotID.length - 1]);
			
			String[] tempTokens = findLineByLabel(reader, SAMPLE_TEMPERATURE, delims).split(delims);
			double sampleTemperature = parseDoubleWithComma( tempTokens[tempTokens.length - 1] ) + TO_KELVIN;
			
			var met = new Metadata(derive(TEST_TEMPERATURE, sampleTemperature), shotId);
			curve.setMetadata(met);
			
			double time;
			double temp;
			
			var detectorLabel = findLineByLabel(reader, DETECTOR, delims);
			
			if(detectorLabel == null) {
				System.err.println("Skipping " + file.getName());
				return new ArrayList<>();
			}
			
			reader.readLine();
			
			String[] tokens;
			
			for (String line = reader.readLine(); line != null && !line.trim().isEmpty(); line = reader.readLine()) {
				tokens = line.split(delims);
				
				time = parseDoubleWithComma(tokens[0]) * TO_SECONDS;
				temp = parseDoubleWithComma(tokens[1]);
				curve.addPoint(time, temp);
			}
			curve.setRange(new Range(curve.getTimeSequence()));
			
		}

		return new ArrayList<>(Arrays.asList(curve));

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

	public static CurveReader getInstance() {
		return instance;
	}

}