package pulse.ui.frames;

import static java.lang.System.exit;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.YES_NO_OPTION;
import static javax.swing.JOptionPane.YES_OPTION;
import static javax.swing.JOptionPane.showOptionDialog;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_BROWSING_REQUEST;
import static pulse.tasks.processing.ResultFormat.addResultFormatListener;
import static pulse.ui.Messages.getString;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.ui.Version;
import pulse.ui.components.PulseChart;
import pulse.ui.components.PulseMainMenu;
import pulse.ui.components.listeners.FrameVisibilityRequestListener;
import pulse.ui.components.listeners.TaskActionListener;
import pulse.ui.components.models.ResultTableModel;

@SuppressWarnings("serial")
public class TaskControlFrame extends JFrame {

    private final static int HEIGHT = 730;
    private final static int WIDTH = 1035;

    private static TaskControlFrame instance = new TaskControlFrame();

    private Mode mode = Mode.TASK;
    private ProblemStatementFrame problemStatementFrame;
    private SearchOptionsFrame searchOptionsFrame;
    private TaskManagerFrame taskManagerFrame;
    private ModelSelectionFrame modelFrame;
    private PreviewFrame previewFrame;
    private ResultFrame resultsFrame;
    private MainGraphFrame graphFrame;
    private LogFrame logFrame;
    private InternalGraphFrame<Calculation> pulseFrame;

    private PulseMainMenu mainMenu;

    private final static int ICON_SIZE = 16;

    public static TaskControlFrame getInstance() {
        return instance;
    }

    /**
     * Create the frame.
     */
    private TaskControlFrame() {
        setTitle(Version.getCurrentVersion().toString());
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setExtendedState(getExtendedState() | MAXIMIZED_BOTH);
        initComponents();
        initListeners();
        setIconImage(loadIcon("logo.png", 32).getImage());
        addListeners();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        TaskManager.addSessionListener(() -> resetSession());
        addRepositoryListeners(TaskManager.getManagerInstance());
    }
    
    private void addRepositoryListeners(TaskManager s) {
        s.addSelectionListener(e -> graphFrame.plot());
        s.addTaskRepositoryListener(e
                -> {
            if (e.getState() == TASK_BROWSING_REQUEST) {
                setModelSelectionFrameVisible(true);
            }
        }
        );
    }

    public void resetSession() {
        Objects.requireNonNull(mainMenu, "Menu not created");
        problemStatementFrame.resetSession();
        taskManagerFrame.resetSession();
        modelFrame.resetSession();
        resultsFrame.getResultTable().resetSession();
        logFrame.getLogger().clear();
        logFrame.scheduleLogEvents();
        mainMenu.reset();
        addRepositoryListeners(TaskManager.getManagerInstance());
    }

    private void addListeners() {
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent evt) {
                var closingWindow = (JFrame) evt.getSource();
                if (!exitConfirmed(closingWindow)) {
                    closingWindow.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
                } else {
                    closingWindow.setDefaultCloseOperation(EXIT_ON_CLOSE);
                }
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
        return showOptionDialog(closingComponent, getString("TaskControlFrame.ExitMessage"),
                getString("TaskControlFrame.ExitTitle"), YES_NO_OPTION, WARNING_MESSAGE, null, options,
                options[1]) == YES_OPTION;
    }

    private void initListeners() {
        mainMenu.addFrameVisibilityRequestListener(new FrameVisibilityRequestListener() {

            @Override
            public void onProblemStatementShowRequest() {
                if (TaskManager.getManagerInstance().getSelectedTask() != null) {
                    problemStatementFrame.update();
                    setProblemStatementFrameVisible(true);
                } else {
                    System.out.println("Please select a task");
                }
            }

            @Override
            public void onSearchSettingsShowRequest() {
                setSearchOptionsFrameVisible(true);
            }

        });

        mainMenu.addExitRequestListener(() -> {
            if (exitConfirmed(this)) {
                exit(0);
            }
        });

        addResultFormatListener(rfe -> ((ResultTableModel) resultsFrame.getResultTable().getModel())
                .changeFormat(rfe.getResultFormat()));

        resultsFrame.addFrameCreationListener(() -> setPreviewFrameVisible(true));

        taskManagerFrame.getTaskToolbar().addTaskActionListener(new TaskActionListener() {

            @Override
            public void onRemoveRequest() {
                // no new actions
            }

            @Override
            public void onClearRequest() {
                logFrame.getLogger().clear();
                resultsFrame.getResultTable().clear();
            }

            @Override
            public void onResetRequest() {
                logFrame.getLogger().clear();
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
        logFrame.setFrameIcon(loadIcon("log.png", ICON_SIZE, Color.white));
        resultsFrame = new ResultFrame();
        resultsFrame.setFrameIcon(loadIcon("result.png", ICON_SIZE, Color.white));
        previewFrame = new PreviewFrame();
        previewFrame.setFrameIcon(loadIcon("preview.png", ICON_SIZE, Color.white));
        taskManagerFrame = new TaskManagerFrame();
        taskManagerFrame.setFrameIcon(loadIcon("task_manager.png", ICON_SIZE, Color.white));
        graphFrame = MainGraphFrame.getInstance();
        graphFrame.setFrameIcon(loadIcon("curves.png", ICON_SIZE, Color.white));

        problemStatementFrame = new ProblemStatementFrame();
        problemStatementFrame.setFrameIcon(loadIcon("heat_problem.png", ICON_SIZE, Color.white));
        modelFrame = new ModelSelectionFrame();
        modelFrame.setFrameIcon(loadIcon("stored.png", ICON_SIZE, Color.white));

        searchOptionsFrame = new SearchOptionsFrame();
        searchOptionsFrame.setFrameIcon(loadIcon("optimiser.png", ICON_SIZE, Color.white));

        pulseFrame = new InternalGraphFrame<Calculation>("Pulse Shape", new PulseChart("Time (ms)", "Laser Power (a. u.)"));
        pulseFrame.setFrameIcon(loadIcon("pulse.png", ICON_SIZE, Color.white));
        pulseFrame.setVisible(false);

        /*
		 * CONSTRAINT ADJUSTMENT
         */
        resizeQuadrants();
        desktopPane.add(taskManagerFrame);
        desktopPane.add(pulseFrame);
        desktopPane.add(graphFrame);
        desktopPane.add(previewFrame);
        desktopPane.add(logFrame);
        desktopPane.add(resultsFrame);
        desktopPane.add(problemStatementFrame);
        desktopPane.add(searchOptionsFrame);
        desktopPane.add(modelFrame);

        setDefaultResizeBehaviour();

        pack();

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

        modelFrame.addInternalFrameListener(new InternalFrameAdapter() {

            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                setModelSelectionFrameVisible(false);
            }

        });

    }

    private void doResize() {
        switch (mode) {
            case TASK:
                resizeQuadrants();
                break;
            case PROBLEM:
                resizeTriplet(problemStatementFrame, pulseFrame, graphFrame);
                break;
            case SEARCH:
                resizeFull(searchOptionsFrame);
                break;
            case PREVIEW:
                resizeHalves(previewFrame, resultsFrame);
                break;
            case MODEL_COMPARISON:
                resizeTriplet(graphFrame, resultsFrame, modelFrame);
                break;
            default:
                break;
        }
    }

    private void resizeFull(JInternalFrame f1) {
        final var gap = 10;
        final var h = this.getContentPane().getHeight() - 2 * gap;
        final var w = this.getContentPane().getWidth() - 2 * gap;

        var p1 = new Point(gap, gap);
        var s1 = new Dimension(w, h);
        f1.setLocation(p1);
        f1.setSize(s1);
    }

    private void resizeHalves(JInternalFrame f1, JInternalFrame f2) {
        final var gap = 10;
        final var h = this.getContentPane().getHeight() - 3 * gap;
        final var w = this.getContentPane().getWidth() - 2 * gap;

        var p1 = new Point(gap, gap);
        var s1 = new Dimension(w, 6 * h / 10);

        var p2 = new Point(gap, 2 * gap + 6 * h / 10);
        var s2 = new Dimension(w, 4 * h / 10);

        f1.setLocation(p1);
        f1.setSize(s1);
        f2.setLocation(p2);
        f2.setSize(s2);
    }

    private void resizeTriplet(JInternalFrame f1, JInternalFrame f2, JInternalFrame f3) {
        final var gap = 10;

        final var h = this.getContentPane().getHeight() - 3 * gap;
        var w = this.getContentPane().getWidth() - 2 * gap;

        var p1 = new Point(gap, gap);
        var s1 = new Dimension(w, 6 * h / 10);

        f1.setLocation(p1);
        f1.setSize(s1);

        w = this.getContentPane().getWidth() - 3 * gap;

        var p2 = new Point(gap, 2 * gap + 6 * h / 10);
        var s2 = new Dimension(w / 4, 4 * h / 10);

        f2.setLocation(p2);
        f2.setSize(s2);

        var p3 = new Point(2 * gap + w / 4, 2 * gap + 6 * h / 10);
        var s3 = new Dimension(3 * w / 4, 4 * h / 10);

        f3.setLocation(p3);
        f3.setSize(s3);
    }

    private void resizeQuadrants() {
        final var gap = 10;
        final var h = this.getContentPane().getHeight() - 3 * gap;
        final var w = this.getContentPane().getWidth() - 3 * gap;

        var p1 = new Point(gap, gap);
        var s1 = new Dimension(45 * w / 100, 55 * h / 100);

        var p2 = new Point(2 * gap + 45 * w / 100, gap);
        var s2 = new Dimension(55 * w / 100, 55 * h / 100);

        var p3 = new Point(gap, 2 * gap + 55 * h / 100);
        var s3 = new Dimension(45 * w / 100, 45 * h / 100);

        var p4 = new Point(2 * gap + 45 * w / 100, 2 * gap + 55 * h / 100);
        var s4 = new Dimension(55 * w / 100, 45 * h / 100);

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
        previewFrame.update(((ResultTableModel) resultsFrame.getResultTable().getModel()).getFormat(),
                resultsFrame.getResultTable().data());

        previewFrame.setVisible(show);

        resultsFrame.setVisible(true);
        taskManagerFrame.setVisible(!show);
        graphFrame.setVisible(!show);
        logFrame.setVisible(!show);

        mode = show ? Mode.PREVIEW : Mode.TASK;
        doResize();

    }

    private void setProblemStatementFrameVisible(boolean show) {
        problemStatementFrame.setVisible(show);
        pulseFrame.setVisible(show);
        graphFrame.setVisible(true);

        previewFrame.setVisible(false);
        resultsFrame.setVisible(!show);
        taskManagerFrame.setVisible(!show);
        logFrame.setVisible(!show);

        mode = show ? Mode.PROBLEM : Mode.TASK;
        doResize();
    }

    private void setSearchOptionsFrameVisible(boolean show) {
        if (show) {
            searchOptionsFrame.update();
        }
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

    private void setModelSelectionFrameVisible(boolean show) {
        modelFrame.setVisible(show);
        resultsFrame.setVisible(true);
        graphFrame.setVisible(true);

        problemStatementFrame.setVisible(false);
        previewFrame.setVisible(false);
        taskManagerFrame.setVisible(!show);
        logFrame.setVisible(!show);

        mode = show ? Mode.MODEL_COMPARISON : Mode.TASK;
        doResize();
    }

    public enum Mode {

        TASK, PROBLEM, PREVIEW, SEARCH, MODEL_COMPARISON;

    }

    public Mode getMode() {
        return mode;
    }

    public InternalGraphFrame<Calculation> getPulseFrame() {
        return pulseFrame;
    }

}
