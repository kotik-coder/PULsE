package pulse.ui;

import java.awt.EventQueue;
import java.awt.Image;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.swing.ImageIcon;

import pulse.ui.charts.Chart;
import pulse.ui.components.IconCheckBox;
import pulse.ui.frames.ProblemStatementFrame;
import pulse.ui.frames.SearchOptionsFrame;
import pulse.ui.frames.TaskControlFrame;

/**
 * <p>This is the main class used to launch {@code PULsE} and start the GUI. 
 * In addition to providing the launcher methods, it also provides some functionality
 * for accessing the System CPU and memory usage, as well as the number of available
 * threads that can be used in calculation.</p>
 *
 */

public class Launcher {

	private static TaskControlFrame controlFrame;
	private static ProblemStatementFrame directFrame;
	private static SearchOptionsFrame searchOptionsFrame; 
	
	private Launcher() { }
	
	/**
	 * Launches the application and creates a GUI.
	 */
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Chart.setup();
					TaskControlFrame.getInstance().setLocationRelativeTo(null);
					TaskControlFrame.getInstance().setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	/**
	 * Invoked when the respective frame component needs to be shown on screen.
	 */

	public static void showProblemStatementFrame() {
		if(directFrame == null) {
			directFrame = 
				new ProblemStatementFrame();
			directFrame.setLocationRelativeTo(controlFrame);
		}
		else
			directFrame.update();
		directFrame.setVisible(true);			
	}
	
	/**
	 * Invoked when the respective frame component needs to be shown on screen.
	 */
	
	public static void showSearchOptionsFrame() {
		if(searchOptionsFrame == null) {
			searchOptionsFrame = 
				new SearchOptionsFrame(  );
			searchOptionsFrame.setLocationRelativeTo(controlFrame);
		} else
			searchOptionsFrame.update();
		searchOptionsFrame.setVisible(true);
	}
	
	/**
	 * <p>This will calculate the ratio {@code totalMemory/maxMemory} using the standard {@code Runtime}.
	 * Note this memory usage depends on heap allocation for the JVM.</p>
	 * @return a value depicting the memory usage
	 */
	
	public static double getMemoryUsage() {
		double totalMemory = Runtime.getRuntime().totalMemory();
		double maxMemory = Runtime.getRuntime().maxMemory();
		return ( totalMemory/maxMemory * 100 );
	}
	
	/**
	 * <p>This will calculate the CPU load for the machine running {@code PULsE}.
	 * Note this is rather code-intensive, so it is recommende to use only at certain
	 * time intervals.</p>
	 * @return a value depicting the CPU usage
	 */
	
	public static double CPUUsage() {

	    MBeanServer mbs    = ManagementFactory.getPlatformMBeanServer();
	    ObjectName name = null;
		try {
			name = ObjectName.getInstance("java.lang:type=OperatingSystem");
		} catch (MalformedObjectNameException | NullPointerException e1) {
			System.err.println("Error while calculating CPU usage:");
			e1.printStackTrace();
		}
		
	    AttributeList list = null;
		try {
			list = mbs.getAttributes(name, new String[]{ "ProcessCpuLoad" });
		} catch (InstanceNotFoundException | ReflectionException e) {
			System.err.println("Error while calculating CPU usage:");
			e.printStackTrace();
		}

	    if (list.isEmpty()) 
	    	return Integer.valueOf(null);

	    Attribute att = (Attribute)list.get(0);
	    double value  = (double)att.getValue();

	    if (value < 0)
	    	return Integer.valueOf(null);
	    
	    return (value * 100);
	}
	
	/**
	 * Finds the number of threads available for calculation. This will be used
	 * by the {@code TaskManager} when allocating the {@code ForkJoinPool} for 
	 * running several tasks in parallel.  
	 * @return the number of threads, which is greater or equal to the number of cores
	 * @see pulse.tasks.TaskManager
	 */
	
	public static int threadsAvailable() {
		int number = Runtime.getRuntime().availableProcessors();
		return number > 1 ? (number - 1) : 1;
	}
	
    public static ImageIcon loadIcon(String path, int iconSize) {
    	ImageIcon imageIcon = new ImageIcon(Launcher.class.getResource(path)); // load the image to a imageIcon
    	Image image = imageIcon.getImage(); // transform it 
    	Image newimg = image.getScaledInstance(iconSize, iconSize,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
    	return new ImageIcon(newimg);  // transform it back
    }
	
}