package pulse.io.readers;

import java.io.File;
import java.io.IOException;

import pulse.problem.laser.NumericPulseData;

/**
 * A reader for importing numeric pulse data -- if available.
 *
 */
public interface PulseDataReader extends AbstractReader<NumericPulseData> {

    /**
     * Converts the ASCII file to a {@code NumericPulseData} object.
     */
    @Override
    public abstract NumericPulseData read(File file) throws IOException;

}
