package pulse.ui;

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

import pulse.ui.frames.TaskControlFrame;

/**
 * <p>This is the main class used to launch {@code PULsE} and start the GUI. 
 * In addition to providing the launcher methods, it also provides some functionality
 * for accessing the System CPU and memory usage, as well as the number of available
 * threads that can be used in calculation.</p>
 *
 */

public class Launcher {

	private Launcher() { }
	
	/**
	 * Launches the application and creates a GUI.
	 */
	
	public static void main(String[] args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Launcher.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
				TaskControlFrame.getInstance().setVisible(true);
		        TaskControlFrame.getInstance().setLocationRelativeTo(null);
            }
        });
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
    	ImageIcon imageIcon = new ImageIcon(Launcher.class.getResource("/"  + path)); // load the image to a imageIcon
    	Image image = imageIcon.getImage(); // transform it 
    	Image newimg = image.getScaledInstance(iconSize, iconSize,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
    	return new ImageIcon(newimg);  // transform it back
    }
	
}