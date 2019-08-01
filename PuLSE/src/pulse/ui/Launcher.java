package pulse.ui;

import java.awt.EventQueue;

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
					controlFrame = new TaskControlFrame();
					controlFrame.setLocationRelativeTo(null);
					controlFrame.setVisible(true);
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
	
}
