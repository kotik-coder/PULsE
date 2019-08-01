package pulse.io.readers;

import java.io.File;
import java.io.IOException;

import pulse.input.PropertyCurve;

public interface PropertyTableReader extends AbstractReader {
	
	public abstract PropertyCurve read(File file) throws IOException;

}
