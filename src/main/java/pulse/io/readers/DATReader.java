package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.input.Range;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * A specific implementation of {@code CurveReader} used to read {@code .dat}
 * files.
 * <p>
 * This file format has been previously used to export data obtained with the
 * Kvant laser flash analyser. Files with a {@code .dat} extension should consist of a
 * one-line header listing the test temperature; second and subsequent lines should be made of two or three
 * tab-delimited data columns. The first column represents time
 * <math><i>t</i></math> [seconds], and the second column represents the rise of
 * the absolute temperature of the sample over the ambient temperature
 * <math><i>T</i>-<i>T</i><sub>0</sub></math>. Any other columns are ignored by
 * this reader. The temperature rise corresponds to an absolute scale, according
 * to NIST recommendations.
 * </p>
 */

public class DATReader implements CurveReader {

	private static CurveReader instance = new DATReader();
	private final static double CONVERSION_TO_KELVIN = 273.15;

	private DATReader() {
		//intentionally blank
	}

	/**
	 * @return a {@code String} equal to {.dat}
	 */

	@Override
	public String getSupportedExtension() {
		return Messages.getString("DATReader.0"); //$NON-NLS-1$
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

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                double T = Double.parseDouble(reader.readLine()) + CONVERSION_TO_KELVIN;
                Metadata met = new Metadata( NumericProperty.derive(NumericPropertyKeyword.TEST_TEMPERATURE, T), -1);
                curve.setMetadata(met);
                double time, temp;
                String delims = Messages.getString("DATReader.2"); //$NON-NLS-1$
                StringTokenizer tokenizer;
                for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                    tokenizer = new StringTokenizer(line, delims);
                    time = Double.parseDouble(tokenizer.nextToken());
                    temp = Double.parseDouble(tokenizer.nextToken());
                    curve.addPoint(time, temp);
                }
                curve.setRange(new Range(curve.getTimeSequence()));
            }

		return new ArrayList<>(Arrays.asList(curve));

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