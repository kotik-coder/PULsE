package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import pulse.problem.laser.NumericPulseData;
import pulse.ui.Messages;

/**
 * Reads numeric pulse data generated by the Proteus LFA Analysis export tool.
 * The data must have a decimal point separator and should follow, in general,
 * the same rules set by the NetzschPulseCSVReader
 *
 * @see pulse.io.reader.NetzschCSVReader
 *
 */
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

    /**
     * This performs a basic check, finding the shot ID, which is then passed to
     * a new {@code NumericPulseData} object.The latter is populated using the
     * time-power sequence stored in this file.If the {@value PULSE} keyword is
     * not found, the method will display an error.
     *
     * @param file
     * @throws java.io.IOException
     * @see pulse.io.readers.NetzschCSVReader.read()
     * @return a new {@code NumericPulseData} object encapsulating the contents
     * of {@code file}
     */
    @Override
    public NumericPulseData read(File file) throws IOException {
        Objects.requireNonNull(file, Messages.getString("DATReader.1"));

        NumericPulseData data = null;

        ((NetzschCSVReader) NetzschCSVReader.getInstance())
                .setDefaultLocale(); //always start with a default locale

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            int shotId = NetzschCSVReader.determineShotID(reader, file);
            data = new NumericPulseData(shotId);

            var pulseLabel = NetzschCSVReader.findLineByLabel(reader, PULSE, false);

            if (pulseLabel == null) {
                System.err.println("Skipping " + file.getName());
                return null;
            }

            reader.readLine();
            NetzschCSVReader.populate(data, reader);

        } catch (ParseException ex) {
            Logger.getLogger(NetzschPulseCSVReader.class.getName()).log(Level.SEVERE, null, ex);
        }

        return data;

    }

    /**
     * As this class uses the singleton pattern, only one instance is created
     * using an empty no-argument constructor.
     *
     * @return the single instance of this class.
     */
    public static PulseDataReader getInstance() {
        return instance;
    }

}
