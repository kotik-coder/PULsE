package pulse.ui;

import static java.awt.EventQueue.invokeLater;
import static java.awt.SplashScreen.getSplashScreen;
import static java.lang.System.err;
import static java.lang.System.setErr;
import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_WEEK_DATE;
import static java.util.Objects.requireNonNull;
import static pulse.ui.frames.TaskControlFrame.getInstance;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

import javax.swing.UIManager;

import com.alee.laf.WebLookAndFeel;
import com.alee.skin.dark.WebDarkSkin;

/**
 * <p>
 * This is the main class used to launch {@code PULsE} and start the GUI. In
 * addition to providing the launcher methods, it also provides some
 * functionality for accessing the System CPU and memory usage, as well as the
 * number of available threads that can be used in calculation.
 * </p>
 *
 */

public class Launcher {
	
	private PrintStream errStream;

	private Launcher() {
		arrangeErrorOutput();
	}

	/**
	 * Launches the application and creates a GUI.
	 */

	public static void main(String[] args) {
		new Launcher();
		splashScreen();

		WebLookAndFeel.install( WebDarkSkin.class);
		try {
		    UIManager.setLookAndFeel( new WebLookAndFeel() );
		} catch( Exception ex ) {
		    System.err.println( "Failed to initialize LaF" );
		}
		
		/* Create and display the form */
		invokeLater(() -> {
			getInstance().setLocationRelativeTo(null);
			getInstance().setVisible(true);
		});
	}

	private static void splashScreen() {
		var splash = getSplashScreen();
		if (splash == null)
			err.println("SplashScreen.getSplashScreen() returned null");
		else {
			var g = splash.createGraphics();
			requireNonNull(g, "splash.createGraphics() returned null");
		}
	}
	
	private void arrangeErrorOutput() {
		try {
			setErr( new PrintStream(new File("ErrorLog_" + now().format(ISO_WEEK_DATE) + ".log")) );
		} catch (FileNotFoundException e) {
			System.err.println("Unable to set up error stream");
			e.printStackTrace();
		}
	}
	
	@Override
	public void finalize() {
		errStream.close();
	}

}