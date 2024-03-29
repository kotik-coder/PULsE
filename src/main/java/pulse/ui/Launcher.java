package pulse.ui;

import static java.awt.EventQueue.invokeLater;
import static java.awt.SplashScreen.getSplashScreen;
import static java.lang.System.err;
import static java.lang.System.setErr;
import static java.time.LocalDateTime.now;
import static java.util.Objects.requireNonNull;
import static pulse.ui.frames.TaskControlFrame.getInstance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.FlatDarkLaf;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * This is the main class used to launch {@code PULsE} and start the GUI. In
 * addition to providing the launcher methods, it also redirects the System.err
 * stream to an external file. An empty log file is deleted upon program exit
 * via a shutdown hook.
 * </p>
 *
 */
public class Launcher {

    private PrintStream errStream;
    private File errorLog;
    private final static boolean DEBUG = false;

    private static final File LOCK = new File("pulse.lock");

    private Launcher() {
        if (!DEBUG) {
            arrangeErrorOutput();
        }
        arrangeMessages();
    }

    /**
     * Launches the application and creates a GUI.
     */
    public static void main(String[] args) {
        new Launcher();

        if (!LOCK.exists()) {

            try {
                LOCK.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(Launcher.class.getName()).log(Level.SEVERE, "Unable to create lock file", ex);
            }

            LOCK.deleteOnExit();

            splashScreen();

            //WebLookAndFeel.install(WebDarkSkin.class);
            FlatDarkLaf.setup();
            try {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } catch (Exception ex) {
                System.err.println("Failed to initialize LaF");
            }

            var newVersion = Version.getCurrentVersion().checkNewVersion();

            /* Create and display the form */
            invokeLater(() -> {
                getInstance().setLocationRelativeTo(null);
                getInstance().setVisible(true);

                if (newVersion != null) {
                    JOptionPane.showMessageDialog(null, "<html>A new version of this software is available: "
                            + newVersion.toString() + "<br>Please visit the PULsE website for more details.</html>");
                }

            });

        } else {
            System.out.println(Messages.getString("msg.running"));
        }

    }

    private static void splashScreen() {
        var splash = getSplashScreen();
        if (splash == null) {
            err.println("SplashScreen.getSplashScreen() returned null");
        } else {
            var g = splash.createGraphics();
            requireNonNull(g, "splash.createGraphics() returned null");
        }
    }

    private void arrangeErrorOutput() {
        String path = Launcher.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String decodedPath = "";
        //
        try {
            decodedPath = URLDecoder.decode(path, "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            System.err.println("Unsupported UTF-8 encoding. Details below.");
            e1.printStackTrace();
        }
        //
        try {
            var dir = new File(decodedPath).getParent();
            errorLog = new File(dir + File.separator + "ErrorLog_" + now() + ".log");
            setErr(new PrintStream(errorLog));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to set up error stream");
            e.printStackTrace();
        }

        createShutdownHook();

    }

    private void arrangeMessages() {
        System.setOut(new PrintStream(System.out) {

            @Override
            public void println(String str) {
                JOptionPane.showMessageDialog(null, Messages.getString("TextWrap.0")
                        + str + Messages.getString("TextWrap.1"));
            }

        });
    }

    private void createShutdownHook() {

        /*
		 * Delete log file on program exit if empty
         */
        Runnable r = () -> {
            if (errorLog != null && errorLog.exists() && errorLog.length() < 1) {
                errorLog.delete();
            }
            //delete lock explicitly on abnormal termination
            if (LOCK.exists()) {
                LOCK.delete();
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(r));

    }

    @Override
    public void finalize() {
        errStream.close();
    }

}
