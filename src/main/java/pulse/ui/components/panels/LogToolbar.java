package pulse.ui.components.panels;

import static javax.swing.SwingConstants.CENTER;
import static pulse.tasks.logs.Log.isVerbose;
import static pulse.tasks.logs.Log.setVerbose;
import static pulse.ui.Messages.getString;
import static pulse.util.ImageUtils.loadIcon;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import pulse.ui.components.listeners.LogExportListener;

@SuppressWarnings("serial")
public class LogToolbar extends JPanel {

	private final static int ICON_SIZE = 16;
	private List<LogExportListener> listeners;

	public LogToolbar() {
		initComponents();
		listeners = new ArrayList<>();
	}

	public void initComponents() {
		setLayout(new GridLayout());

		var saveLogBtn = new JButton(loadIcon("save.png", ICON_SIZE));
		saveLogBtn.setToolTipText("Save");

		var verboseCheckBox = new JCheckBox(getString("LogToolBar.Verbose")); //$NON-NLS-1$
		verboseCheckBox.setSelected(isVerbose());
		verboseCheckBox.setHorizontalAlignment(CENTER);

		verboseCheckBox.addActionListener(event -> setVerbose(verboseCheckBox.isSelected()));

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