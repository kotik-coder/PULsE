package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import pulse.ui.Messages;
import pulse.ui.Version;
import pulse.util.ReflexiveFinder;

/**
 * A {@code ReaderManager} is a single class that manages most input operations.
 * <p>
 * {@code ReaderManager} can dynamically access all available readers using
 * their {@code Reflexive} interfaces. It can also dynamically assign a file
 * with a recognised format to a specific reader. Note that MetaFilePopulator is
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

    private static List<String> allDataExtensions = supportedExtensions(ReaderManager.curveReaders());
    private static List<String> allPulseExtensions = supportedExtensions(ReaderManager.pulseReaders());
    private static List<String> allDatasetExtensions = supportedExtensions(ReaderManager.datasetReaders());

    private ReaderManager() {
        // intentionally blank
    }

    private static List<String> supportedExtensions(List<? extends AbstractReader<?>> readers) {
        return readers.stream().map(reader -> reader.getSupportedExtension()).collect(Collectors.toList());
    }

    /**
     * Returns a list of extensions recognised by the available
     * {@code CurveReader}s.
     *
     * @return a {@code List} of {
     * @String} objects representing file extensions
     */
    public static List<String> getCurveExtensions() {
        return allDataExtensions;
    }

    public static List<String> getPulseExtensions() {
        return allPulseExtensions;
    }

    /**
     * Returns a list of extensions recognised by the available
     * {@code DatasetReader}s.
     *
     * @return a {@code List} of {
     * @String} objects representing file extensions
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
    private static List<AbstractReader> allReaders(String pckgname) {
        return ReflexiveFinder.simpleInstances(pckgname).stream().filter(ref -> ref instanceof AbstractReader)
                .map(reflexive -> (AbstractReader) reflexive).collect(Collectors.toList());
    }

    /**
     * Finds all classes assignable from {@code AbstractReader} within
     * <b>this</b>
     * package.
     *
     * @return a list of {@code AbstractReader}s in this package
     */
    @SuppressWarnings("rawtypes")
    private static List<AbstractReader> allReaders() {
        return allReaders(ReaderManager.class.getPackage().getName());
    }

    /**
     * Finds all classes assignable from {@code CurveReader} within the
     * {@code pckgname} package.
     *
     * @param pckgname the name of the package to conduct search in.
     * @return a list of {@code CurveReader}s in {@code pckgname}
     */
    public static List<CurveReader> findCurveReaders(String pckgname) {
        return allReaders.stream().filter(reader -> reader instanceof CurveReader).map(r -> (CurveReader) r)
                .collect(Collectors.toList());
    }

    public static List<PulseDataReader> findPulseReaders(String pckgname) {
        return allReaders.stream().filter(reader -> reader instanceof PulseDataReader).map(r -> (PulseDataReader) r)
                .collect(Collectors.toList());
    }

    /**
     * Finds all classes assignable from {@code CurveReader} within <b>this</b>
     * package.
     *
     * @return a list of {@code CurveReader}s in this package
     */
    public static List<CurveReader> curveReaders() {
        return findCurveReaders(ReaderManager.class.getPackage().getName());
    }

    public static List<PulseDataReader> pulseReaders() {
        return findPulseReaders(ReaderManager.class.getPackage().getName());
    }

    /**
     * Finds all classes assignable from {@code DatasetReader} within the
     * {@code pckgname} package.
     *
     * @param pckgname the name of the package to conduct search in.
     * @return a list of {@code DatasetReader}s in {@code pckgname}
     */
    public static List<DatasetReader> findDatasetReaders(String pckgname) {
        return allReaders.stream().filter(reader -> reader instanceof DatasetReader).map(r -> (DatasetReader) r)
                .collect(Collectors.toList());
    }

    /**
     * Finds all classes assignable from {@code DatasetReader} within
     * <b>this</b>
     * package.
     *
     * @return a list of {@code DatasetReader}s in this package
     */
    public static List<DatasetReader> datasetReaders() {
        return findDatasetReaders(ReaderManager.class.getPackage().getName());
    }

    /**
     * Attempts to find a {@code DatasetReader} for processing {@code file}.
     *
     * @param <T>
     * @param readers
     * @param file the target file supposedly containing data for an
     * {@code InterpolationDataset}.
     * @return an {@code InterpolationDataset} extracted from {
     * @file} using the first available {@code DatasetReader} from the list
     * @throws IllegalArgumentException if the file has an unsupported extension
     */
    public static <T> T read(List<? extends AbstractReader<T>> readers, File file) {
        Objects.requireNonNull(readers);

        var optional = readers.stream()
                .filter(reader -> AbstractHandler.extensionsMatch(file, reader.getSupportedExtension())).findFirst();

        if (!optional.isPresent()) {
            throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName());
        }

        T result = null;

        try {
            result = optional.get().read(file);
        } catch (IOException e) {
            System.err.println("Error reading " + file + " with reader: " + optional.get());
            e.printStackTrace();
        }

        return result;

    }

    /**
     * Obtains a set of files in {@code directory} and attemps to convert each
     * file to {@code T} using {@code readers}.
     *
     * @param <T> a type recognised by {@code readers}
     * @param readers a list of {@code AbstractReader}s capable of processing
     * {@code T}
     * @param directory a directory
     * @return the set of converted {@code T} objects
     * @throws IllegalArgumentException if second argument is not a directory
     */
    public static <T> Set<T> readDirectory(List<AbstractReader<T>> readers, File directory)
            throws IllegalArgumentException {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Not a directory: " + directory);
        }

        var es = Executors.newSingleThreadExecutor();

        var callableList = new ArrayList<Callable<T>>();

        for (File f : directory.listFiles()) {
            Callable<T> callable = () -> read(readers, f);
            callableList.add(callable);
        }

        Set<T> result = new HashSet<>();

        try {
            List<Future<T>> futures = es.invokeAll(callableList);

            for (Future<T> f : futures) {
                result.add(f.get());
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(ReaderManager.class.getName()).log(Level.SEVERE,
                    "Reading interrupted when loading files from " + directory.toString(), ex);
        } catch (ExecutionException ex) {
            Logger.getLogger(ReaderManager.class.getName()).log(Level.SEVERE,
                    "Error executing read operation using concurrency", ex);
        }

        return result;
    }

    /**
     * This method is specifically introduced to handle multiple files in a
     * resources folder enclosed within the {@code jar} archive. A list of files
     * is required to be included in the same location, which is scanned and
     * each entry is added to a temporary list of names. A combination of these
     * names with the relative {@code location} allows reading separate files
     * and collating the result in a unique {@code Set}.
     *
     * @param <T> a type recognised by the {@code reader}
     * @param reader the reader specifically targetted at {@code T}
     * @param location the relative location of files
     * @param listName the name of the list-file
     * @return a unique {@code Set} of {@code T}
     */
    public static <T> Set<T> load(AbstractReader<T> reader, String location, String listName) {

        var stream = ReaderManager.class.getResourceAsStream(location + listName);
        var names = new ArrayList<String>();

        try (Scanner s = new Scanner(stream)) {
            while (s.hasNext()) {
                names.add(s.next());
            }
        }

        return names.stream().map(name -> readSpecific(reader, location, name)).map(obj -> (T) obj)
                .collect(Collectors.toSet());

    }

    private static <T> T readSpecific(AbstractReader<T> reader, String location, String name) {
        T result = null;
        try {
            var f = File.createTempFile(name, ".tmp");
            f.deleteOnExit();
            FileUtils.copyInputStreamToFile(ReaderManager.class.getResourceAsStream(location + name), f);
            result = reader.read(f);
        } catch (IOException e) {
            System.err.println("Unable to read: " + name);
            e.printStackTrace();
        }
        return result;
    }

    public static Version readVersion() {
        var versionInfoFile = Version.class.getResource("/Version.txt");
        String versionLabel = "";
        long date = 0;
        try {
            date = versionInfoFile.openConnection().getLastModified();
        } catch (IOException e1) {
            System.err.println("Could not connect to local version file!");
            e1.printStackTrace();
        }
        try {
            versionLabel = IOUtils.toString(versionInfoFile, "UTF-8");
        } catch (IOException e) {
            System.err.println("Could not read current version!");
            e.printStackTrace();
        }
        return new Version(versionLabel, date);
    }

}
