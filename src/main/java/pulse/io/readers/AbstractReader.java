package pulse.io.readers;

import java.io.File;
import java.io.IOException;

/**
 * Basic interface for readers in {@code PULsE}.
 * <p>
 * Provides the {@code read} capability, which handles an external file and
 * attempts to read its content to translate it into the {@code T} internal
 * structure. The internal structure of {@code T} formed by a set of properties,
 * accessibles, and property holders, is assumed to be immutable. The size of
 * lists, arrays and containers may (and usually will) change as a result of
 * using the reader.
 * </p>
 */

public interface AbstractReader<T> extends AbstractHandler {

	/**
	 * Reads {@code f} to translate its contents to one of the immutable structures
	 * of {@code T}. Usually this involves reading arrays and collections and
	 * pasting their data into existing structural elements. This does not change
	 * the internal structure of the object.
	 * 
	 * @param f a file which has readable content
	 * @return a {@code T} object created by reading all information from {@code f}.
	 * @throws IOException
	 */

	public T read(File f) throws IOException;

}