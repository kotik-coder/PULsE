package pulse.ui.components.panels;

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;

import pulse.ui.Launcher;
import pulse.ui.components.listeners.ResultRequestListener;

@SuppressWarnings("serial")
public class ResultToolbar extends JPanel {

	private final static int ICON_SIZE = 16;

	private JButton deleteEntryBtn;
	private JButton mergeBtn;
	private JButton undoBtn;
	private JButton previewBtn;
	private JButton saveResultsBtn;

	private List<ResultRequestListener> listeners;

	public ResultToolbar() {
		initComponents();
		listeners = new ArrayList<ResultRequestListener>();
	}

	public void initComponents() {
		deleteEntryBtn = new JButton(Launcher.loadIcon("remove.png", ICON_SIZE));
		mergeBtn = new JButton(Launcher.loadIcon("merge.png", ICON_SIZE));
		undoBtn = new JButton(Launcher.loadIcon("reset.png", ICON_SIZE));
		previewBtn = new JButton(Launcher.loadIcon("preview.png", ICON_SIZE));
		saveResultsBtn = new JButton(Launcher.loadIcon("save.png", ICON_SIZE));
		setLayout(new GridLayout(5, 0));

		deleteEntryBtn.setToolTipText("Delete Entry");
		add(deleteEntryBtn);

		mergeBtn.setToolTipText("Merge (Auto)");
		add(mergeBtn);

		undoBtn.setToolTipText("Undo");
		add(undoBtn);

		previewBtn.setToolTipText("Preview");
		add(previewBtn);

		saveResultsBtn.setToolTipText("Save");
		add(saveResultsBtn);

		deleteEntryBtn.setEnabled(false);
		deleteEntryBtn.addActionListener(e -> notifyDelete());

		mergeBtn.setEnabled(false);
		mergeBtn.addActionListener(e -> notifyMerge());

		undoBtn.setEnabled(false);
		undoBtn.addActionListener(e -> notifyUndo());

		previewBtn.setEnabled(false);
		previewBtn.addActionListener(e -> notifyPreview());

		saveResultsBtn.setEnabled(false);
		saveResultsBtn.addActionListener(e -> notifyExport());

	}

	public void setDeleteEnabled(boolean deleteEnabled) {
		deleteEntryBtn.setEnabled(deleteEnabled);
	}

	public void setMergeEnabled(boolean mergeEnabled) {
		mergeBtn.setEnabled(mergeEnabled);
	}

	public void setUndoEnabled(boolean undoEnabled) {
		undoBtn.setEnabled(undoEnabled);
	}

	public void setPreviewEnabled(boolean previewEnabled) {
		previewBtn.setEnabled(previewEnabled);
	}

	public void setExportEnabled(boolean exportEnabled) {
		saveResultsBtn.setEnabled(exportEnabled);
	}

	private void notifyDelete() {
		listeners.stream().forEach(l -> l.onDeleteRequest());
	}

	private void notifyMerge() {
		listeners.stream().forEach(l -> l.onMergeRequest());
	}

	private void notifyUndo() {
		listeners.stream().forEach(l -> l.onUndoRequest());
	}

	private void notifyPreview() {
		listeners.stream().forEach(l -> l.onPreviewRequest());
	}

	private void notifyExport() {
		listeners.stream().forEach(l -> l.onExportRequest());
	}

	public void addResultRequestListener(ResultRequestListener l) {
		listeners.add(l);
	}

}