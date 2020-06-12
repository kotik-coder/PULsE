package pulse.io.readers;

import java.io.File;
import java.io.IOException;

public interface AbstractPopulator<T> extends AbstractHandler {

	public void populate(File f, T t) throws IOException;

}