package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.ui.Messages;
import pulse.util.ReflexiveFinder;

/**
 * A {@code ReaderManager} is a single class that manages most input operations.
 * <p>
 * {@code ReaderManager} can dynamically access all available readers using
 * their {@code Reflexive} interfaces. It can also dynamically assign a file
 * with a recognised format to a specific reader. Note that MetaFileReader is
 * not covered by this class, as there is only one {@code Metadata} format which
 * is internal to {@code PULsE}.
 * </p>
 * <p>
 * This class heavily relies on the stream API from the Java SDK.
 * </p>
 * 
 * @see pulse.util.Reflexive
 * @see pulse.io.readers.CurveReader
 * @see pulse.io.readers.DatasetReader
 */

public class ReaderManager {

	@SuppressWarnings("rawtypes")
	private static List<AbstractReader> allReaders = allReaders();
	private static List<CurveReader> curveReaders = curveReaders();
	private static List<DatasetReader> datasetReaders = datasetReaders();

	private static List<String> allDataExtensions = supportedCurveExtensions();
	private static List<String> allDatasetExtensions = supportedDatasetExtensions();

	private ReaderManager() {
	}

	private static List<String> supportedCurveExtensions() {
		return ReaderManager.curveReaders().stream().map(reader -> reader.getSupportedExtension())
				.collect(Collectors.toList());
	}

	/**
	 * Returns a list of extensions recognised by the available
	 * {@code CurveReader}s.
	 * 
	 * @return a {@code List} of {@String} objects representing file extensions
	 */

	public static List<String> getCurveExtensions() {
		return allDataExtensions;
	}

	private static List<String> supportedDatasetExtensions() {
		return ReaderManager.datasetReaders().stream().map(reader -> reader.getSupportedExtension())
				.collect(Collectors.toList());
	}

	/**
	 * Returns a list of extensions recognised by the available
	 * {@code DatasetReader}s.
	 * 
	 * @return a {@code List} of {@String} objects representing file extensions
	 */

	public static List<String> getDatasetExtensions() {
		return allDatasetExtensions;
	}

	/**
	 * Finds all classes assignable from {@code AbstractReader} within the
	 * {@code pckgname} package.
	 * 
	 * @param pckgname the name of the package for the classes to be searched in
	 * @return a list of {@code AbstractReader}s in {@code pckgnamge}
	 */

	@SuppressWarnings("rawtypes")
	public static List<AbstractReader> allReaders(String pckgname) {
		return ReflexiveFinder.simpleInstances(pckgname).stream().filter(ref -> ref instanceof AbstractReader)
				.map(reflexive -> (AbstractReader) reflexive).collect(Collectors.toList());
	}

	/**
	 * Finds all classes assignable from {@code AbstractReader} within <b>this</b>
	 * package.
	 * 
	 * @return a list of {@code AbstractReader}s in this package
	 */

	@SuppressWarnings("rawtypes")
	public static List<AbstractReader> allReaders() {
		return allReaders(ReaderManager.class.getPackage().getName());
	}

	/**
	 * Finds all classes assignable from {@code CurveReader} within the
	 * {@code pckgname} package.
	 * 
	 * @param pckgname the name of the package to conduct search in.
	 * @return a list of {@code CurveReader}s in {@code pckgname}
	 */

	public static List<CurveReader> curveReaders(String pckgname) {
		if (allReaders == null)
			allReaders = allReaders(pckgname);

		return allReaders.stream().filter(reader -> reader instanceof CurveReader).map(r -> (CurveReader) r)
				.collect(Collectors.toList());
	}

	/**
	 * Finds all classes assignable from {@code CurveReader} within <b>this</b>
	 * package.
	 * 
	 * @return a list of {@code CurveReader}s in this package
	 */

	public static List<CurveReader> curveReaders() {
		return curveReaders(ReaderManager.class.getPackage().getName());
	}

	/**
	 * Finds all classes assignable from {@code DatasetReader} within the
	 * {@code pckgname} package.
	 * 
	 * @param pckgname the name of the package to conduct search in.
	 * @return a list of {@code DatasetReader}s in {@code pckgname}
	 */

	public static List<DatasetReader> findDatasetReaders(String pckgname) {
		if (allReaders == null)
			allReaders = allReaders(pckgname);

		return allReaders.stream().filter(reader -> reader instanceof DatasetReader).map(r -> (DatasetReader) r)
				.collect(Collectors.toList());
	}

	/**
	 * Finds all classes assignable from {@code DatasetReader} within <b>this</b>
	 * package.
	 * 
	 * @return a list of {@code DatasetReader}s in this package
	 */

	public static List<DatasetReader> datasetReaders() {
		return findDatasetReaders(ReaderManager.class.getPackage().getName());
	}

	/**
	 * Attempts to find a {@code CurveReader} for processing {@code file}.
	 * 
	 * @param file the target file supposedly containing experimental data
	 * @return a {@code List} of {@code ExperimentalData} extracted from
	 *         {@file} using the first available {@code CurveReader} from the list
	 * @throws IOException              if the reader has been found, but an error
	 *                                  occurred when reading the file
	 * @throws IllegalArgumentException if the file has an unsupported extension
	 */

	public static List<ExperimentalData> extract(File file) throws IOException, IllegalArgumentException {
		if (curveReaders == null)
			curveReaders = curveReaders();

		Optional<CurveReader> optional = curveReaders.stream()
				.filter(reader -> AbstractHandler.extensionsMatch(file, reader.getSupportedExtension())).findFirst();

		if (!optional.isPresent())
			throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName());

		var reader = optional.get();
		return reader.read(file);
	}

	/**
	 * Attempts to find a {@code DatasetReader} for processing {@code file}.
	 * 
	 * @param file the target file supposedly containing data for an
	 *             {@code InterpolationDataset}.
	 * @return an {@code InterpolationDataset} extracted from {@file} using the
	 *         first available {@code DatasetReader} from the list
	 * @throws IOException              if the reader has been found, but an error
	 *                                  occurred when reading the file
	 * @throws IllegalArgumentException if the file has an unsupported extension
	 */

	public static InterpolationDataset readDataset(File file) throws IOException {
		if (datasetReaders == null)
			datasetReaders = datasetReaders();

		Optional<DatasetReader> optional = datasetReaders.stream()
				.filter(reader -> AbstractHandler.extensionsMatch(file, reader.getSupportedExtension())).findFirst();

		if (!optional.isPresent())
			throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName());

		return optional.get().read(file);
	}

	@SuppressWarnings("rawtypes")
	public static Object read(File file) throws IOException {
		if (allReaders == null)
			allReaders = allReaders();

		Optional<AbstractReader> optional = allReaders.stream()
				.filter(reader -> AbstractHandler.extensionsMatch(file, reader.getSupportedExtension())).findFirst();

		if (!optional.isPresent())
			throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName());

		return optional.get().read(file);

	}

	public static List<Object> readDirectory(File file) throws IllegalArgumentException {
		if (!file.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + file);

		var list = new ArrayList<Object>();

		for (File f : file.listFiles())
			try {
				list.add(read(f));
			} catch (IOException e) {
				System.err.println("Failed to read file: " + f);
				e.printStackTrace();
			}

		return list;

	}
	
	@SuppressWarnings("unchecked")
	public static <T> Set<T> load(AbstractReader<?> reader, String location, String listName) {
		
		InputStream stream = ReaderManager.class.getResourceAsStream(location + listName);

		java.util.ArrayList<java.lang.String> names;
            try (Scanner s = new Scanner(stream)) {
                names = new ArrayList<String>();
                while(s.hasNext())
                    names.add(s.next());
            }
		
		return names.stream().map(name -> readSpecific(reader, location, name) ).map(obj -> (T)obj).collect(Collectors.toSet());
		
	}
	
	private static Object readSpecific(AbstractReader<?> reader, String location, String name) {
		Object result = null;
		try {
			var f = File.createTempFile(name, ".tmp");
			f.deleteOnExit();
			FileUtils.copyInputStreamToFile( ReaderManager.class.getResourceAsStream(location + name) , f);
			result = reader.read(f);
		} catch(IOException e) {
			System.err.println("Unable to read: " + name);
			e.printStackTrace();
		}
		return result;
	}

}