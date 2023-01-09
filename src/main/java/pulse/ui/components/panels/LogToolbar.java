package pulse.ui.components.panels;

import static pulse.ui.Messages.getString;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;

import static pulse.tasks.logs.Log.setGraphicalLog;
import static pulse.tasks.logs.Log.isGraphicalLog;
import pulse.ui.components.listeners.LogListener;

@SuppressWarnings("serial")
public class LogToolbar extends JToolBar {

    private final static int ICON_SIZE = 16;
    private List<LogListener> listeners;

    public LogToolbar() {
        super();
        setFloatable(false);
        initComponents();
        listeners = new ArrayList<>();
    }

    public void initComponents() {
        setLayout(new GridLayout());

        var saveLogBtn = new JButton(loadIcon("save.png", ICON_SIZE, Color.white));
        saveLogBtn.setToolTipText("Save");

        var logmodeCheckbox = new JCheckBox(getString("LogToolBar.Verbose")); //$NON-NLS-1$
        logmodeCheckbox.setSelected(isGraphicalLog());
        logmodeCheckbox.setHorizontalAlignment(CENTER);

        logmodeCheckbox.addActionListener(event -> {
            boolean selected = logmodeCheckbox.isSelected();
            setGraphicalLog(selected);
            listeners.stream().forEach(l -> l.onLogModeChanged(selected));
        });

        saveLogBtn.addActionListener(e -> listeners.stream().forEach(l -> l.onLogExportRequest()));

        add(saveLogBtn);

        add(logmodeCheckbox);
    }

    public void addLogListener(LogListener l) {
        listeners.add(l);
    }

}