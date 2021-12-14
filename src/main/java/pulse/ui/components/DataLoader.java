package pulse.ui.components;

import static pulse.io.readers.ReaderManager.datasetReaders;
import static pulse.io.readers.ReaderManager.pulseReaders;
import static pulse.io.readers.ReaderManager.read;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.StandartType;
import pulse.io.readers.MetaFilePopulator;
import pulse.io.readers.ReaderManager;
import pulse.problem.laser.NumericPulse;
import pulse.problem.laser.NumericPulseData;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.ui.Messages;
import pulse.ui.frames.dialogs.ProgressDialog;

/**
 * Manages loading the experimental time-temperature profiles, metadata files
 * and {@code InterpolationDataset}s. Tracks the load progress using a
 * {@code ProgressDialog}.
 *
 */
public class DataLoader {

    private static File dir;
    private static ProgressDialog progressFrame = new ProgressDialog();

    static {
        TaskManager.getManagerInstance().addTaskRepositoryListener(e -> {
            if (e.getState() == TaskRepositoryEvent.State.TASK_ADDED) {
                progressFrame.incrementProgress();
            }
        });
        progressFrame.setLocationRelativeTo(null);
        progressFrame.setAlwaysOnTop(true);
    }

    private DataLoader() {
        // intentionally blank
    }

    /**
     * Initiates a user dialog to load experimental time-temperature profiles.
     * Multiple selection is possible. When the user finalises selection, the
     * {@code TaskManager} will start generating tasks using the files selected
     * by the user as input. The tracker progress bar is reset and made visible.
     */
    public static void loadDataDialog() {
        var files = userInput(Messages.getString("TaskControlFrame.ExtensionDescriptor"),
                ReaderManager.getCurveExtensions());

        var instance = TaskManager.getManagerInstance();

        if (files != null) {
            progressFrame.trackProgress(files.size());
            instance.generateTasks(files);
        }

    }

    /**
     * Asks the user to select a single file containing the metadata, with the
     * extension given by the {@code MetaFilePopulator} class. If a valid
     * selection is made and the task list is not empty, proceeds to populating
     * each task's metadata object using the information contained in the
     * selected file. If the task has a problem assigned to it, sets the
     * parameters of that problem to match the loaded {@code Metadata}.
     * Throughout the process, progress is monitored in a separate dialog with a
     * {@code JProgressBar}. Upon finishing, the data range will be checked to
     * determine if truncation is needed.
     *
     * @see truncateDataDialog
     */
    public static void loadMetadataDialog() {
        var handler = MetaFilePopulator.getInstance();
        var file = userInputSingle(Messages.getString("TaskControlFrame.ExtensionDescriptor"),
                handler.getSupportedExtension());

        var instance = TaskManager.getManagerInstance();

        if (instance.numberOfTasks() < 1 || file == null) {
            return; // invalid input received, do nothing
        }
        progressFrame.trackProgress(instance.numberOfTasks() + 1);

        // attempt to fill metadata and problem
        for (SearchTask task : instance.getTaskList()) {
            var data = task.getExperimentalCurve();

            try {
                handler.populate(file, data.getMetadata());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(progressFrame, Messages.getString("TaskControlFrame.LoadError"),
                        Messages.getString("TaskControlFrame.IOError"), JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

            var p = task.getCurrentCalculation().getProblem();
            if (p != null) {
                p.retrieveData(data);
            }
            progressFrame.incrementProgress();

        }

        progressFrame.incrementProgress();

        // select first of the generated task
        instance.selectFirstTask();

    }

    public static void loadPulseDialog() {
        var files = userInput(Messages.getString("TaskControlFrame.ExtensionDescriptor"),
                ReaderManager.getPulseExtensions());

        if (files != null) {

            var manager = TaskManager.getManagerInstance();

            progressFrame.trackProgress(files.size());

            Runnable loader = () -> {

                final var pool = Executors.newCachedThreadPool();

                files.stream().map(f -> read(pulseReaders(), f))
                        .filter(pulseData -> (pulseData != null))
                        .forEach(pulseData -> {

                            var task = manager.getTask(pulseData.getExternalID());

                            if (task != null) {
                                pool.submit(() -> {
                                    var metadata = task.getExperimentalCurve().getMetadata();
                                    metadata.setPulseData(pulseData);
                                    metadata.getPulseDescriptor()
                                            .setSelectedDescriptor(
                                                    NumericPulse.class.getSimpleName());
                                });
                            }

                        });

            };
            
            Executors.newSingleThreadExecutor().submit(loader);

        }

    }

    /**
     * Uses the {@code ReaderManager} to create an {@code InterpolationDataset}
     * from data stored in {@code f} and updates the associated properties of
     * each task.
     *
     * @param f a {@code File} containing a property specified by the
     * {@code type}
     * @param type the type of the loaded data
     * @throws IOException if file cannot be read
     * @see pulse.tasks.TaskManager.evaluate()
     */
    public static void load(StandartType type, File f) throws IOException {
        Objects.requireNonNull(f);
        InterpolationDataset.setDataset(read(datasetReaders(), f), type);
        TaskManager.getManagerInstance().evaluate();
    }

    private static List<File> userInput(String descriptor, List<String> extensions) {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setCurrentDirectory(directory());
        fileChooser.setMultiSelectionEnabled(true);

        String[] extArray = extensions.toArray(new String[extensions.size()]);
        fileChooser.setFileFilter(new FileNameExtensionFilter(descriptor, extArray));

        boolean approve = fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
        dir = fileChooser.getCurrentDirectory();

        return approve ? Arrays.asList(fileChooser.getSelectedFiles()) : null;
    }

    private static File userInputSingle(String descriptor, List<String> extensions) {
        JFileChooser fileChooser = new JFileChooser();

        fileChooser.setCurrentDirectory(directory());
        fileChooser.setMultiSelectionEnabled(false);

        String[] extArray = extensions.toArray(new String[extensions.size()]);
        fileChooser.setFileFilter(new FileNameExtensionFilter(descriptor, extArray));

        boolean approve = fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION;
        dir = fileChooser.getCurrentDirectory();

        return approve ? fileChooser.getSelectedFile() : null;
    }

    private static File userInputSingle(String descriptor, String... extensions) {
        return userInputSingle(descriptor, Arrays.asList(extensions));
    }

    private static File directory() {
        if (dir != null) {
            return dir;
        } else
			try {
            return new File(DataLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            System.err.println("Cannot determine current working directory.");
            e.printStackTrace();
        }
        return null;
    }

}
