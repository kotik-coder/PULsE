package pulse.io.readers;

import java.io.File;
import java.io.IOException;

/**
 * Basic interface for readers in {@code PULsE}.
 * <p>
 * The only functionality this interfaces offers is to check if a certain files
 * conforms to the extension requirement. All file readers used in {@code PULsE}
 * should implement this interface.
 * </p>
 */

public interface AbstractReader<T> extends AbstractHandler {

	public T read(File f) throws IOException;

}