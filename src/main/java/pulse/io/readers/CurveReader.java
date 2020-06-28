package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;

/**
 * Basic interface for reading {@code ExperimentalData} (which extends
 * {@code HeatingCurve} -- hence the name).
 * <p>
 * In addition to providing the functionality of an {@code AbstractReader}, this
 * interface provides specific methods to read and sort multiple
 * {@code ExperimentalData} objects.
 * </p>
 */

public interface CurveReader extends AbstractReader<List<ExperimentalData>> {

	/**
	 * Basic operation for reading the {@code file} and translating its contents to
	 * a {@code List} of {@code ExperimentalData} objects.
	 * <p>
	 * 
	 * @param file a {@code File} which has <b>either</b> all information encoded in
	 *             its contents <b>or</b> provides {@code URI} links to other files,
	 *             each containing the necessary information.
	 * @return
	 *         <p>
	 *         a {@code List} of {@code ExperimentalData} objects associated with
	 *         this {@code file}. In case if {@code file} contains only one
	 *         {@code ExperimentalData}, i.e. if the data is only presented for one
	 *         heating curve taken at a specific temperature after a single laser
	 *         shot, the size of the {@code List} will be equal to unity.
	 *         </p>
	 * @throws IOException if something goes wrong with reading the file
	 */

        @Override
	public abstract List<ExperimentalData> read(File file) throws IOException;

	/**
	 * Sorts the {@code List} of {@code ExperimentalData} according to their
	 * external IDs (if any).
	 * 
	 * @param array an unsorted list of {@code ExperimentalData}
	 * @return the same list after sorting
	 * @see pulse.input.Metadata.getExternalID()
	 */

	public static List<ExperimentalData> sort(List<ExperimentalData> array) {
		Comparator<ExperimentalData> externalIdComparator = (ExperimentalData e1, ExperimentalData e2) -> Integer
				.valueOf(e1.getMetadata().getExternalID()).compareTo(Integer.valueOf(e2.getMetadata().getExternalID()));

		return array.stream().sorted(externalIdComparator).collect(Collectors.toList());
	}

}