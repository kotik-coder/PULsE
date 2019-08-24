package pulse.io.readers;

import java.io.File;
import java.io.IOException;

import pulse.input.InterpolationDataset;

public interface PropertyTableReader extends AbstractReader {
	
	public abstract InterpolationDataset read(File file) throws IOException;

}
