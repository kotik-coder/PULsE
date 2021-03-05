package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import pulse.problem.laser.NumericPulseData;

public interface PulseDataReader extends AbstractReader<NumericPulseData> {

	@Override
	public abstract NumericPulseData read(File file) throws IOException;

}