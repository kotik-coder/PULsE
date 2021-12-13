package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;
import java.util.StringTokenizer;

import pulse.input.InterpolationDataset;
import pulse.ui.Messages;
import pulse.util.ImmutableDataEntry;

/**
 * A {@code DatasetReader} capable of reading {@code .tbl} files.
 * <p>
 * The format of these files is simply two tab-delimited numeric column. The
 * first column represents the 'keys', and the second -- the corresponding
 * 'values'.
 * </p>
 * <p>
 * Specific heat capacity and density at different temperatures can be read as
 * {@code ASCII} files with a .tbl suffix, where the first column is temperature
 * (in degrees Celsius) and the second column is the specific heat capacity (in
 * J kg<sup>-1</sup> K<sup>-1</sup>) or density (in kg m<sup>-3</sup>).
 * </p>
 * <p>
 * Below is an example of a valid {@code .tbl} file:
 * </p>
 *
 * <pre>
 * <code>
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
 * </code>
 * </pre>
 */
public class TBLReader implements DatasetReader {

    private static DatasetReader instance = new TBLReader();

    private TBLReader() {
        // intentionally blank
    }

    /**
     * @return a String equal to '{@code tbl}'
     */
    @Override
    public String getSupportedExtension() {
        return Messages.getString("TBLReader.0");
    }

    /**
     * As this class is built using a singleton pattern, only one instance
     * exists.
     *
     * @return the static instance of this class
     */
    public static DatasetReader getInstance() {
        return instance;
    }

    /**
     * Reads through a {@code file} with {@code .tbl extension}, converting each
     * row into an {@code ImmutableDataEntry<Double,Double>}, which is then
     * added to a newly created {@code InterpolationDataset}. Upon completion,
     * the {@code doInterpolation()} method of {@code InterpolationDataset} is
     * invoked.
     *
     * @see pulse.input.InterpolationDataset.doInterpolation()
     * @param file a {@code File} with {@code tbl} extension
     */
    @Override
    public InterpolationDataset read(File file) throws IOException {
        Objects.requireNonNull(file, Messages.getString("TBLReader.1"));

        if (!isExtensionSupported(file)) {
            throw new IllegalArgumentException("Extension not supported: " + file.getName());
        }

        var curve = new InterpolationDataset();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String delims = Messages.getString("TBLReader.2");
            StringTokenizer tokenizer;

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                tokenizer = new StringTokenizer(line);
                curve.add(new ImmutableDataEntry<>(parse(tokenizer, delims), parse(tokenizer, delims)));
            }
        }

        curve.doInterpolation();
        return curve;

    }

    private static Double parse(StringTokenizer tokenizer, String delims) {
        return Double.parseDouble(tokenizer.nextToken(delims));
    }

}
