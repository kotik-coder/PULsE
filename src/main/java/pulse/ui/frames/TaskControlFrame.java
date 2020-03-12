package pulse.ui.frames;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import pulse.tasks.ResultFormat;
import pulse.tasks.TaskManager;
import pulse.ui.Launcher;
import pulse.ui.Messages;
import pulse.ui.components.PulseMainMenu;
import pulse.ui.components.listeners.FrameVisibilityRequestListener;
import pulse.ui.components.listeners.TaskActionListener;
import pulse.ui.components.models.ResultTableModel;

@SuppressWarnings("serial")
public class TaskControlFrame extends JFrame {

	private static Mode mode = Mode.TASK;
	
	private final static int HEIGHT = 730;
	private final static int WIDTH = 1035;	                                        	    		
	
	private static TaskControlFrame instance = new TaskControlFrame();	    		     
   
    private static ProblemStatementFrame problemStatementFrame;
    private static SearchOptionsFrame searchOptionsFrame;
    private static TaskManagerFrame taskManagerFrame;
    private static PreviewFrame previewFrame;		
	private static ResultFrame resultsFrame;
	private static GraphFrame graphFrame;	
    private static LogFrame logFrame;
    
    private static PulseMainMenu mainMenu;
	
	public static TaskControlFrame getInstance() {
		return instance;
	}		
    
    /**
	 * Create the frame.
	 */
	
	private TaskControlFrame() {
        setTitle(Messages.getString("TaskControlFrame.SoftwareTitle"));
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
		initComponents();
		initListeners();
		TaskManager.addSelectionListener(e -> graphFrame.plot());
		setIconImage(Launcher.loadIcon("logo.png", 32).getImage());
		addListeners();
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);					
	}
	
	private void addListeners() {
		addWindowListener(new WindowAdapter() {
			
			@Override
			public void windowClosing(WindowEvent evt) {                               
			        JFrame closingWindow = (JFrame) evt.getSource();
			        if(!exitConfirmed(closingWindow)) {
			            closingWindow.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
			        } else
			            closingWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
			}
			
		});
		
		addComponentListener(new ComponentAdapter() {
			
			@Override
			public void componentResized(ComponentEvent e) {
				doResize();
			}
			
		});
		
	}
    
	private boolean exitConfirmed(Component closingComponent) {
        	Object[] options = {"Yes", "No"};
        	return JOptionPane.showOptionDialog(
                                    closingComponent,
                                    Messages.getString("TaskControlFrame.ExitMessage"), 
                                    Messages.getString("TaskControlFrame.ExitTitle"), 
                                    JOptionPane.YES_NO_OPTION,
                                    JOptionPane.WARNING_MESSAGE,
                                    null,
                                    options,
                                    options[1]) == JOptionPane.YES_OPTION;
    }
	
	private void initListeners() {
        mainMenu.addFrameVisibilityRequestListener(new FrameVisibilityRequestListener() {

			@Override
			public void onProblemStatementShowRequest() {
				setProblemStatementFrameVisible(true);
			}

			@Override
			public void onSearchSettingsShowRequest() {
				setSearchOptionsFrameVisible(true);
			}        	        	
        	
        });
        
        mainMenu.addExitRequestListener(() -> {                                  
	        if(exitConfirmed(this))
	            System.exit(0);
        });
		
		ResultFormat.addResultFormatListener(rfe ->  
			 ( (ResultTableModel)resultsFrame.getResultTable()
					 .getModel()).changeFormat(rfe.getResultFormat() ));
		
        resultsFrame.addFrameCreationListener(() -> setPreviewFrameVisible(true));
		
        taskManagerFrame.getTaskToolbar().addTaskActionListener(new TaskActionListener() {

			@Override
			public void onRemoveRequest() {
				//no new actions
			}

			@Override
			public void onClearRequest() {
				logFrame.getLogTextPane().clear();
				resultsFrame.getResultTable().clear();
			}

			@Override
			public void onResetRequest() {				
				logFrame.getLogTextPane().clear();
				resultsFrame.getResultTable().removeAll();
			}

			@Override
			public void onGraphRequest() {
				graphFrame.plot();
			}        	        	
        	
        });
	}
	
	/**
     * This method is called from within the constructor to initialize the form.    
     */           
    
    private void initComponents() {

        var desktopPane = new JDesktopPane();                        
        setContentPane(desktopPane);
        
        mainMenu = new PulseMainMenu();
        setJMenuBar(mainMenu);
                
        logFrame = new LogFrame();                
        graphFrame = new GraphFrame();        
        resultsFrame = new ResultFrame();                             
        previewFrame = new PreviewFrame();
        taskManagerFrame = new TaskManagerFrame();
                        
		problemStatementFrame = 
				new ProblemStatementFrame();	
		        		
		searchOptionsFrame = 
				new SearchOptionsFrame(  );		
				
        /*
         * CONSTRAINT ADJUSTMENT
         */
		
        pack();
		
		resizeQuadrants();
		desktopPane.add(taskManagerFrame);
        desktopPane.add(graphFrame);
        desktopPane.add(previewFrame);
        desktopPane.add(logFrame);
        desktopPane.add(resultsFrame);
		desktopPane.add(problemStatementFrame);
		desktopPane.add(searchOptionsFrame);
        
		setDefaultResizeBehaviour();
        
    }
    
    private void setDefaultResizeBehaviour() {
		var ifa = new InternalFrameAdapter() {
			
			@Override
			public void internalFrameDeiconified(InternalFrameEvent e) {
				resizeQuadrants();
			}
			
		};
		
        taskManagerFrame.addInternalFrameListener(ifa);        
        graphFrame.addInternalFrameListener(ifa);
        logFrame.addInternalFrameListener(ifa);
        resultsFrame.addInternalFrameListener(ifa);
        		
        previewFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		setPreviewFrameVisible(false);
        	}
        	
        });
		
		problemStatementFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		setProblemStatementFrameVisible(false);
        	}
        	
        });
		
		searchOptionsFrame.addInternalFrameListener(new InternalFrameAdapter() {
        	
        	@Override
        	public void internalFrameClosing(InternalFrameEvent e) {
        		setSearchOptionsFrameVisible(false);
        	}
        	
        });
        
    }
	
	private void doResize() {
		switch(mode) {
		case TASK : resizeQuadrants(); break;
		case PROBLEM : resizeHalves(problemStatementFrame,graphFrame); break;
		case SEARCH : resizeFull(searchOptionsFrame); break;
		case PREVIEW : resizeHalves(previewFrame, resultsFrame); break;
		}
	}
	
	private void resizeFull(JInternalFrame f1) {
		final int gap = 10;
		final int h = this.getContentPane().getHeight()-2*gap;
		final int w = this.getContentPane().getWidth()-2*gap;
		
		var p1 = new Point(gap, gap);
		var s1 = new Dimension(w, h );	
		f1.setLocation(p1);
		f1.setSize(s1);
	}
	
	private void resizeHalves(JInternalFrame f1, JInternalFrame f2) {
		final int gap = 10;
		final int h = this.getContentPane().getHeight()-3*gap;
		final int w = this.getContentPane().getWidth()-2*gap;
		
		var p1 = new Point(gap, gap);
		var s1 = new Dimension(w, 6*h/10 );
		
		var p2 = new Point(gap, 2*gap+6*h/10);
		var s2 = new Dimension(w, 4*h/10);
	
		f1.setLocation(p1);
		f1.setSize(s1);
		f2.setLocation(p2);
		f2.setSize(s2);
	}
	
	private void resizeQuadrants() {
		final int gap = 10;
		final int h = this.getContentPane().getHeight()-3*gap;
		final int w = this.getContentPane().getWidth()-3*gap;
		
		var p1 = new Point(gap, gap);
		var s1 = new Dimension(45*w/100, 55*h/100 );
		
		var p2 = new Point(2*gap + 45*w/100, gap );
		var s2 = new Dimension(55*w/100, 55*h/100);
		
		var p3 = new Point(gap, 2*gap + 55*h/100);
		var s3 = new Dimension(45*w/100, 45*h/100);
		
		var p4 = new Point(2*gap + 45*w/100, 2*gap + 55*h/100);
		var s4 = new Dimension(55*w/100, 45*h/100);
		
		taskManagerFrame.setLocation(p1);
		taskManagerFrame.setSize(s1);
		graphFrame.setLocation(p2);
		graphFrame.setSize(s2);
		logFrame.setLocation(p3);
		logFrame.setSize(s3);
		resultsFrame.setLocation(p4);
		resultsFrame.setSize(s4);
	}
	   
	private void setPreviewFrameVisible(boolean show) {
		previewFrame.update(
	    		((ResultTableModel)resultsFrame.getResultTable().getModel()).getFormat()
	    		, resultsFrame.getResultTable().data());		
		
		previewFrame.setVisible(show);				
		
		resultsFrame.setVisible(true);		
		taskManagerFrame.setVisible(!show);
		graphFrame.setVisible(!show);
		logFrame.setVisible(!show);
		
		mode = show ? Mode.PREVIEW : Mode.TASK;
		doResize();
		
	}
	private void setProblemStatementFrameVisible(boolean show) {
		problemStatementFrame.setVisible(show );
		graphFrame.setVisible(true);
		
		previewFrame.setVisible(false);		
		resultsFrame.setVisible(!show);		
		taskManagerFrame.setVisible(!show);
		logFrame.setVisible(!show);	
		
		mode = show ? Mode.PROBLEM : Mode.TASK;
		doResize();		
	}
	
	private void setSearchOptionsFrameVisible(boolean show) {	
		if(show) searchOptionsFrame.update();	
		searchOptionsFrame.setVisible(show);
		
		problemStatementFrame.setVisible(false);
		previewFrame.setVisible(false);				
		resultsFrame.setVisible(!show);		
		taskManagerFrame.setVisible(!show);
		graphFrame.setVisible(!show);
		logFrame.setVisible(!show);
		
		mode = show ? Mode.SEARCH : Mode.TASK;
		doResize();
	}
	
	private enum Mode {
		
		TASK, PROBLEM, PREVIEW, SEARCH;
		
	}
		
}