package pulse.ui.components.panels;

import static java.awt.Color.black;
import static java.awt.Color.red;
import static pulse.util.ImageUtils.blend;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JToolBar;

import pulse.tasks.TaskManager;
import pulse.ui.components.buttons.ExecutionButton;
import pulse.ui.components.listeners.TaskActionListener;

@SuppressWarnings("serial")
public class TaskToolbar extends JToolBar {

    private final static int ICON_SIZE = 16;

    private JButton removeBtn;
    private JButton clearBtn;
    private JButton graphBtn;
    private JButton execBtn;
    private JButton resetBtn;

    private List<TaskActionListener> listeners;

    public TaskToolbar() {
        super();
        setFloatable(false);
        initComponents();
        listeners = new ArrayList<>();
        addButtonListeners();
    }

    private void initComponents() {

        removeBtn = new JButton(loadIcon("remove.png", ICON_SIZE));
        clearBtn = new JButton(loadIcon("clear.png", ICON_SIZE, blend(red, black, 0.5f)));
        resetBtn = new JButton(loadIcon("reset.png", ICON_SIZE));
        graphBtn = new JButton(loadIcon("graph.png", ICON_SIZE));
        execBtn = new ExecutionButton();

        setLayout(new GridLayout(1, 0));

        removeBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        resetBtn.setEnabled(false);
        graphBtn.setEnabled(false);
        execBtn.setEnabled(false);

        removeBtn.setToolTipText("Remove Task");
        add(removeBtn);

        clearBtn.setToolTipText("Clear All Tasks");
        add(clearBtn);

        resetBtn.setToolTipText("Reset All Tasks");
        add(resetBtn);

        graphBtn.setToolTipText("Show Graph");
        add(graphBtn);

        execBtn.setToolTipText("Execute All Tasks");
        add(execBtn);
    }
    
    public void resetSession() {
        ((ExecutionButton)execBtn).resetSession();
    }

    public void setRemoveEnabled(boolean b) {
        removeBtn.setEnabled(b);
    }

    public void setClearEnabled(boolean b) {
        clearBtn.setEnabled(b);
    }

    public void setGraphEnabled(boolean b) {
        graphBtn.setEnabled(b);
    }

    public void setExecEnabled(boolean b) {
        execBtn.setEnabled(b);
    }

    public void setResetEnabled(boolean b) {
        resetBtn.setEnabled(b);
    }

    private void addButtonListeners() {
        removeBtn.addActionListener(e -> notifyRemove());
        clearBtn.addActionListener(e -> notifyClear());
        resetBtn.addActionListener(e -> {
            TaskManager.getManagerInstance().reset();
            notifyReset();
        });
        graphBtn.addActionListener(e -> notifyGraph());
    }

    public void notifyRemove() {
        listeners.stream().forEach(l -> l.onRemoveRequest());
    }

    public void notifyClear() {
        listeners.stream().forEach(l -> l.onClearRequest());
    }

    public void notifyReset() {
        listeners.stream().forEach(l -> l.onResetRequest());
    }

    public void notifyGraph() {
        listeners.stream().forEach(l -> l.onGraphRequest());
    }

    public void addTaskActionListener(TaskActionListener l) {
        listeners.add(l);
    }
    
    public void removeListeners() {
        if(listeners != null) {
            listeners.clear();
        }
    }

}
