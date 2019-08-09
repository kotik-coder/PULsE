package pulse.ui;

import java.awt.EventQueue;
import java.lang.management.ManagementFactory;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import javafx.application.Platform;
import pulse.ui.charts.Chart;
import pulse.ui.frames.ProblemStatementFrame;
import pulse.ui.frames.SearchOptionsFrame;
import pulse.ui.frames.TaskControlFrame;

public class Launcher {

	private static TaskControlFrame controlFrame;
	private static ProblemStatementFrame directFrame;
	private static SearchOptionsFrame searchOptionsFrame; 
	
	private Launcher() {
		
	}
	
	/**
	 * Launch the application.
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

	public static void showProblemStatementFrame() {
		if(directFrame == null) {
			directFrame = 
				new ProblemStatementFrame();
			directFrame.setLocationRelativeTo(controlFrame);
		}
		directFrame.setVisible(true);
	}
	
	public static void showSearchOptionsFrame() {
		if(searchOptionsFrame == null) {
			searchOptionsFrame = 
				new SearchOptionsFrame(  );
			searchOptionsFrame.setLocationRelativeTo(controlFrame);
		}
		searchOptionsFrame.setVisible(true);
	}
	
	public static double getMemoryUsage() {
		double totalMemory = Runtime.getRuntime().totalMemory();
		double maxMemory = Runtime.getRuntime().maxMemory();
		return ( totalMemory/maxMemory * 100 );
	}
	
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
	
	public static int threadsAvailable() {
		int number = Runtime.getRuntime().availableProcessors();
		return number > 1 ? (number - 1) : 1;
	}
	
}
