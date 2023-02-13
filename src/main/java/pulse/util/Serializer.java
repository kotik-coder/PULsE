package pulse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.FILES_ONLY;
import javax.swing.filechooser.FileNameExtensionFilter;
import pulse.tasks.TaskManager;
import pulse.ui.frames.dialogs.ProgressDialog.ProgressWorker;

public class Serializer {

    private static final FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "Saved sessions (.pulse)", "pulse");

    private Serializer() {
        //
    }

    public static void serialize() throws IOException, FileNotFoundException, ClassNotFoundException {
        var fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(FILES_ONLY);
        fileChooser.setFileFilter(filter);
        File f = new File("./Saved/");
        if (!f.exists()) {
            f.mkdir();
        }
        fileChooser.setCurrentDirectory(f);

        int returnVal = fileChooser.showSaveDialog(null);

        if (returnVal == APPROVE_OPTION) {
            String ext = filter.getExtensions()[0];
            File fileToBeSaved;
            if (!fileChooser.getSelectedFile().getAbsolutePath().endsWith(ext)) {
                fileToBeSaved = new File(fileChooser.getSelectedFile() + ext);
            } else {
                fileToBeSaved = fileChooser.getSelectedFile();
            }

            ProgressWorker worker = () -> {
                try {
                    serialize(fileToBeSaved);
                } catch (IOException | ClassNotFoundException ex) {
                    Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, "Failed to save session", ex);
                    System.err.println("Failed to save session.");
                }
            };

            worker.work();
        }

    }

    public static void deserialize() throws FileNotFoundException {
        var fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(FILES_ONLY);
        fileChooser.setFileFilter(filter);
        File f = new File("./Saved/");
        if (f.exists()) {
            fileChooser.setCurrentDirectory(f);
        }

        int returnVal = fileChooser.showOpenDialog(null);

        if (returnVal == APPROVE_OPTION) {

            ProgressWorker worker = () -> {
                try {
                    deserialize(fileChooser.getSelectedFile());
                } catch (IOException | ClassNotFoundException ex) {
                    Logger.getLogger(Serializer.class.getName()).log(Level.SEVERE, "Failed to load session", ex);
                    System.err.println("Failed to load session.");
                }
            };

            worker.work();

        }

    }

    public static void serialize(File fname) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileOutputStream fileOutputStream = new FileOutputStream(fname);
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream)) {
            var instance = TaskManager.getManagerInstance();
            objectOutputStream.writeObject(instance);
        }
    }

    public static void deserialize(File fname) throws FileNotFoundException, IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(fname);
        TaskManager state;
        try (ObjectInputStream ois = new ObjectInputStream(fis)) {
            state = (TaskManager) ois.readObject();
        }
        //close stream
        state.initListeners();
        state.getTaskList().stream().forEach(t -> {
            t.initListeners();
            t.children().stream().forEach(c -> c.initListeners());
        }
        );
        TaskManager.assumeNewState(state);
        state.fireTaskSelected(state);
    }

}
