package pulse.io.readers;

import java.io.File;
import java.io.IOException;

import pulse.input.InterpolationDataset;

/**
 * An {@code AbstractReader} for reading tabular datasets and enhancing them
 * with an interpolation algorithm.
 *
 */

public interface DatasetReader extends AbstractReader<InterpolationDataset> {

	/**
	 * Creates an {@code InterpolationDataset} using the dataset stored in the
	 * {@code file}.
	 * 
	 * @param file a file with a supported extension containing the information
	 *             needed to create an {@code InterpolationDataset}.
	 * @return an {@code InterpolationDataset}, which not only stores the
	 *         information contained in {@code file}, but also provides means of
	 *         interpolation.
	 * @throws IOException if something goes wrong with reading the {@code file}
	 */

        @Override
	public abstract InterpolationDataset read(File file) throws IOException;

}
