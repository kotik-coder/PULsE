package pulse.io.readers;

import java.io.File;
import java.io.IOException;

/**
 * An {@code AbstractPopulator} provides the ability to add extra content to 
 * an object by modifying its internal structure to make it compliant with 
 * an external file. The difference to the {@code AbstractReader} is that the 
 * latter does not change the internal structure of an object. 
 *
 */

public interface AbstractPopulator<T> extends AbstractHandler {

	/**
	 * Tries to populate {@code t} from data contained in {@code f}.
	 * @param f a file presumably containing data that can be converted to the internal format of {@code t}.
	 * @param t a {@code T} object which can potentially be populated by {@code f}.
	 * @throws IOException if an exception occurs during processing {@code f}.
	 */
	
	public void populate(File f, T t) throws IOException;

}