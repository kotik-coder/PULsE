package pulse.io.readers;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.isValueSensible;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.findAny;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringTokenizer;

import pulse.input.Metadata;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.ImmutableDataEntry;
import pulse.util.InstanceDescriptor;

/**
 * An {@code AbstractPopulator} capable of handling metafiles.
 * <p>
 * Metafiles are ASCII files storing various experimental parameters for
 * different instances of {@code ExperimentalData}. The {@code Metadata (.met)}
 * file should be formatted to include a header of arbitrary length, which
 * defines global parameters, and a table where a series of metaproperties is
 * defined for each laser shot.
 * </p>
 * <p>
 * Metadata for each shot should be recorded during the experiment in a tab
 * delimited ASCII format, with a {@code .met} file suffix. Constant data should
 * be recorded in tab-separated pairs at the top of the file, such as
 * {@code Sample_Name}, {@code Thickness} (of the sample, in mm),
 * {@code Diameter} (of the sample, in mm), {@code Spot_Diameter} (diameter of
 * laser spot, in mm), {@code TemporalShape} (e.g. {@code TrapezoidalPulse},
 * {@code RectangularPulse}) and {@code Detector_Iris}. Two line breaks below, a
 * tab-delimited table with headers for variables should contain variable data
 * for each shot. These variables should include ID (which should relate to the
 * final number of the file name for each shot), Test_Temperature (in deg. C),
 * Pulse_Width (the time width of the laser pulse, in ms), {@code Laser_Energy}
 * (the energy transmitted by the laser, in J), and Detector_Gain (gain of the
 * detector). If any of the “constants” listed above are variable, then they
 * should be included in the variable table, and vice versa.
 * </p>
 * The full list of keywords for the {@code .met} files are listed in the
 * {@code NumericPropertyKeyword} enum.
 *
 * <p>
 * An example content of a valid {@code .met} file is provided below.
 * </p>
 *
 * <pre>
 * <code>
 * Thickness	2.034
 * Diameter	9.88
 * Spot_Diameter	10.0
 *
 * Test_Temperature	Pulse_Width	Spot_Diameter	Laser_Energy	Detector_Gain	TemporalShape	Detector_Iris
 * 200	200	5	2	31.81	50	TrapezoidalPulse	1
 * 201	196	5	2	31.81	100	TrapezoidalPulse	1
 * 202	198	5	2	31.81	100	TrapezoidalPulse	1
 * 203	199	5	2	31.81	50	TrapezoidalPulse	1
 * 204	199	5	2	31.81	50	TrapezoidalPulse	1
 * 205	199	5	2	31.81	50	TrapezoidalPulse	1
 * 206	200	5	2	31.81	50	TrapezoidalPulse	1
 * 207	200	5	2	31.81	50	TrapezoidalPulse	1
 * 208	400	5	2	31.81	50	TrapezoidalPulse	1
 * 209	400	5	2	31.81	20	TrapezoidalPulse	1
 * 210	400	5	2	31.81	10	TrapezoidalPulse	1
 * </code>
 * </pre>
 *
 * @see pulse.properties.NumericPropertyKeyword
 * @see pulse.problem.laser.PulseTemporalShape
 */
public class MetaFilePopulator implements AbstractPopulator<Metadata> {

    private static MetaFilePopulator instance = new MetaFilePopulator();
    private final static double TO_KELVIN = 273;

    private MetaFilePopulator() {
        // intentionally blank
    }

    /**
     * Gets the single instance of this class.
     *
     * @return a static instance of {@code MetaFilePopulator}.
     */
    public static MetaFilePopulator getInstance() {
        return instance;
    }

    @Override
    public void populate(File file, Metadata met) throws IOException {
        Objects.requireNonNull(file, Messages.getString("MetaFileReader.1")); //$NON-NLS-1$
        Map<Integer, String> metaFormat = new HashMap<>();
        metaFormat.put(0, "ID"); // id must always be the first entry in the current row

        List<String> tokens = new LinkedList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {

                tokens.clear();
                for (StringTokenizer st = new StringTokenizer(line); st.hasMoreTokens();) {
                    tokens.add(st.nextToken());
                }

                int size = tokens.size();

                if (size == 2) {
                    processPair(tokens, met);
                } else if (size > 2) {

                    if (tokens.get(0).equalsIgnoreCase(metaFormat.get(0))) {

                        for (int i = 1; i < size; i++) {
                            metaFormat.put(i, tokens.get(i));
                        }

                    } else if (Integer.compare(Integer.valueOf(tokens.get(0)), met.getExternalID()) == 0) {

                        processList(tokens, met, metaFormat);

                    }

                }

            }

        }
    }

    private void processPair(List<String> tokens, Metadata met) {
        List<ImmutableDataEntry<String, String>> val = new ArrayList<>();
        var entry = new ImmutableDataEntry<>(tokens.get(0), tokens.get(1));
        val.add(entry);

        try {
            translate(val, met);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            System.err.println("Error changing property in Metadata object. Details below.");
            e.printStackTrace();
        }
    }

    private void processList(List<String> tokens, Metadata met, Map<Integer, String> metaFormat) {
        int size = tokens.size();
        List<ImmutableDataEntry<String, String>> values = new ArrayList<>(size);

        for (int i = 1; i < size; i++) {
            values.add(new ImmutableDataEntry<>(metaFormat.get(i), tokens.get(i)));
        }

        try {
            translate(values, met);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            System.err.println("Error changing property in Metadata object. Details below.");
            e.printStackTrace();
        }
    }

    private void translate(List<ImmutableDataEntry<String, String>> data, Metadata met)
            throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        for (var dataEntry : data) {

            var optional = findAny(dataEntry.getKey());

            // numeric properties
            if (optional.isPresent()) {
                var key = optional.get();

                double value = Double.valueOf(dataEntry.getValue());
                if (key == TEST_TEMPERATURE) {
                    value += TO_KELVIN;
                }

                var proto = def(key);
                value /= proto.getDimensionFactor().doubleValue();

                if (isValueSensible(proto, value)) {
                    proto.setValue(value);
                    met.set(key, proto);
                }

            } // generic properties
            else {

                for (Property genericEntry : met.genericProperties()) {

                    if (genericEntry instanceof InstanceDescriptor
                            || dataEntry.getKey().equalsIgnoreCase(genericEntry.getClass().getSimpleName())) {

                        if (genericEntry.attemptUpdate(dataEntry.getValue())) {
                            met.updateProperty(instance, genericEntry);
                        }

                    }

                }

            }

        }

    }

    /**
     * @return {@code .met}, an internal PULsE meta-file format.
     */
    @Override
    public String getSupportedExtension() {
        return Messages.getString("MetaFileReader.0"); //$NON-NLS-1$
    }

}
