package pulse.ui.components.panels;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import pulse.tasks.Log;
import pulse.ui.Launcher;
import pulse.ui.Messages;
import pulse.ui.components.listeners.LogExportListener;

@SuppressWarnings("serial")
public class LogToolbar extends JPanel {

	private final static int ICON_SIZE = 16;
	private List<LogExportListener> listeners;

	public LogToolbar() {
		initComponents();
		listeners = new ArrayList<LogExportListener>();
	}

	public void initComponents() {
		setLayout(new GridLayout());

		var saveLogBtn = new JButton(Launcher.loadIcon("save.png", ICON_SIZE));
		saveLogBtn.setToolTipText("Save");

		var verboseCheckBox = new JCheckBox(Messages.getString("LogToolBar.Verbose")); //$NON-NLS-1$
		verboseCheckBox.setSelected(Log.isVerbose());
		verboseCheckBox.setHorizontalAlignment(SwingConstants.CENTER);

		verboseCheckBox.addActionListener(event -> Log.setVerbose(verboseCheckBox.isSelected()));

		saveLogBtn.addActionListener(e -> notifyLog());

		add(saveLogBtn);
		add(verboseCheckBox);
	}

	public void notifyLog() {
		listeners.stream().forEach(l -> l.onLogExportRequest());
	}

	public void addLogExportListener(LogExportListener l) {
		listeners.add(l);
	}

}