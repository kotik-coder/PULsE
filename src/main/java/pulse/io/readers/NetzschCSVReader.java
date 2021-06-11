package pulse.io.readers;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import pulse.AbstractData;
import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.input.Range;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * Reads the .CSV files exported from Proteus LFA Analysis software. To load Proteus measurements in PULsE,
 * the detector signal needs to be imported first, followed by the pulse data. 
 * <p>
 * Note that by default the decimal separator is assumed to be a point (".").
 * </p>
 */

public class NetzschCSVReader implements CurveReader {

	private static NetzschCSVReader instance = new NetzschCSVReader();
	
	private final static double TO_KELVIN = 273;
	protected final static double TO_SECONDS = 1E-3;
	private final static double TO_METRES = 1E-3;
	
	private final static String SAMPLE_TEMPERATURE = "Sample_temperature";
	private final static String SHOT_DATA = "Shot_data";
	private final static String DETECTOR = "DETECTOR";
	private final static String THICKNESS = "Thickness_RT";
	private final static String DIAMETER = "Diameter";
	
	/**
	 * Note comma is included as a delimiter character here.
	 */
	
	public final static String delims = "[#();,/°Cx%^]+";
	
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
	 * Reads {@code file}, assuming that it contains data generated by Proteus with the detector signal.
	 * <p>
	 * This will throw an {@code IllegalArgumentException} if the first entry in this file does not contain the 
	 * {@value SHOT_DATA} string. If this is found, then an ID is extracted from the file, which will then be used
	 * to associate a pulse with the newly create {@code ExperimentalData} (this requires another reader. 
	 * When the ID is identified, the file is searched for the keywords {@value THICKNESS} and {@value SAMPLE_TEMPERATURE} to 
	 * determine the sample thickness and baseline temperature of the shot. Then the method proceeds to search for the {@code DETECTOR} keyword,
	 * marking the beginning of the experimental time-signal sequence. If, for example, the file only contains 
	 * the pulse data, the method will return an empty list and print an error message in the log, saying that the file
	 * was skipped. Otherwise, the time-signal sequence will be read, taking care to convert the time (in milliseconds
	 * by default) to second (used by default in PULsE). 
	 * </p>
	 * @return a list containing either zero elements, if the procedure failed, or one element, corresponding to 
	 * the stored shot data.
	 * 
	 */

	@Override
	public List<ExperimentalData> read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("DATReader.1"));

		ExperimentalData curve = new ExperimentalData();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			
			int shotId = determineShotID(reader, file);
			
			var tempTokens = findLineByLabel(reader, THICKNESS, delims).split(delims);
			final double thickness = Double.parseDouble( tempTokens[tempTokens.length - 1] ) * TO_METRES;
			
			tempTokens = findLineByLabel(reader, DIAMETER, delims).split(delims);
			final double diameter = Double.parseDouble( tempTokens[tempTokens.length - 1] ) * TO_METRES;
			
			tempTokens = findLineByLabel(reader, SAMPLE_TEMPERATURE, delims).split(delims);
			final double sampleTemperature = Double.parseDouble( tempTokens[tempTokens.length - 1] ) + TO_KELVIN;
			
			/*
			 * Finds the detector keyword.
			 */
			
			var detectorLabel = findLineByLabel(reader, DETECTOR, delims);
			
			if(detectorLabel == null) {
				System.err.println("Skipping " + file.getName());
				return new ArrayList<>();
			}
			
			reader.readLine();			
			populate(curve, reader);
			
			var met = new Metadata(derive(TEST_TEMPERATURE, sampleTemperature), shotId);
			met.set(NumericPropertyKeyword.THICKNESS, derive(NumericPropertyKeyword.THICKNESS, thickness));
			met.set(NumericPropertyKeyword.DIAMETER, derive(NumericPropertyKeyword.DIAMETER, diameter));
			met.set(NumericPropertyKeyword.FOV_OUTER, derive(NumericPropertyKeyword.FOV_OUTER, 0.85*diameter));
			met.set(NumericPropertyKeyword.SPOT_DIAMETER, derive(NumericPropertyKeyword.SPOT_DIAMETER, 0.94*diameter));
			
			curve.setMetadata(met);
			curve.setRange(new Range(curve.getTimeSequence()));
			
		}

		return new ArrayList<>(Arrays.asList(curve));

	}
	
	protected static void populate(AbstractData data, BufferedReader reader) throws IOException {
		double time;
		double power;
		String[] tokens;
		
		for (String line = reader.readLine(); line != null && !line.trim().isEmpty(); line = reader.readLine()) {
			tokens = line.split(delims);
			
			time = Double.parseDouble(tokens[0]) * NetzschCSVReader.TO_SECONDS;
			power = Double.parseDouble(tokens[1]);
			data.addPoint(time, power);
		}
		
	}
	
	protected static int determineShotID(BufferedReader reader, File file) throws IOException {
		String[] shotID = reader.readLine().split(delims);
		
		int shotId = -1;
		
		//check if first entry makes sense
		if(!shotID[shotID.length - 2].equalsIgnoreCase(SHOT_DATA))
			throw new IllegalArgumentException(file.getName() 
					+ " is not a recognised Netzch CSV file. First entry is: " + shotID[shotID.length - 2]);
		else 
			shotId = Integer.parseInt(shotID[shotID.length - 1]);
		
		return shotId;
		
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
	
	protected static String findLineByLabel(BufferedReader reader, String label, String delims) throws IOException {
		
		String line = "";
		String[] tokens;
		
		//find keyword
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