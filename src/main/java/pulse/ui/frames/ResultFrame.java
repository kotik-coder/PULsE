package pulse.ui.frames;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.EAST;
import static javax.swing.JOptionPane.ERROR_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.io.export.ExportManager.askToExport;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.WINDOW;
import static pulse.tasks.processing.ResultFormat.getInstance;
import static pulse.ui.Messages.getString;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;

import pulse.ui.components.ResultTable;
import pulse.ui.components.listeners.PreviewFrameCreationListener;
import pulse.ui.components.listeners.ResultRequestListener;
import pulse.ui.components.panels.ResultToolbar;
import pulse.ui.frames.dialogs.FormattedInputDialog;

@SuppressWarnings("serial")
public class ResultFrame extends JInternalFrame {

	private ResultToolbar resultToolbar;
	private ResultTable resultTable;
	private List<PreviewFrameCreationListener> listeners;
	private FormattedInputDialog averageWindowDialog;

	public ResultFrame() {
		super("Results", true, false, true, true);
		initComponents();
		listeners = new ArrayList<>();
		addListeners();
		setVisible(true);
	}

	private void initComponents() {
		var resultsScroller = new JScrollPane();

		resultTable = new ResultTable(getInstance());
		resultsScroller.setViewportView(resultTable);
		getContentPane().add(resultsScroller, CENTER);

		resultToolbar = new ResultToolbar();
		getContentPane().add(resultToolbar, EAST);

		averageWindowDialog = new FormattedInputDialog(def(WINDOW));
	}

	private void addListeners() {
		resultToolbar.addResultRequestListener(new ResultRequestListener() {

			@Override
			public void onDeleteRequest() {
				resultTable.deleteSelected();
			}

			@Override
			public void onPreviewRequest() {
				if (!resultTable.hasEnoughElements(1)) {
					showMessageDialog(getWindowAncestor(resultTable), getString("ResultsToolBar.NoDataError"),
							getString("ResultsToolBar.NoResultsError"), ERROR_MESSAGE);
				} else
					notifyPreview();
			}

			@Override
			public void onMergeRequest() {
				if (resultTable.hasEnoughElements(1))
					showInputDialog();
			}

			@Override
			public void onUndoRequest() {
				resultTable.undo();
			}

			@Override
			public void onExportRequest() {
				if (!resultTable.hasEnoughElements(1)) {
					showMessageDialog(getWindowAncestor(resultTable), getString("ResultsToolBar.7"),
							getString("ResultsToolBar.8"), ERROR_MESSAGE);
					return;
				}

				askToExport(resultTable, (JFrame) getWindowAncestor(resultTable), "Calculation results");
			}

		});

		resultTable.getSelectionModel().addListSelectionListener((ListSelectionEvent arg0) -> {
			resultToolbar.setDeleteEnabled(!resultTable.isSelectionEmpty());
		});

		resultTable.getModel().addTableModelListener((TableModelEvent arg0) -> {
			resultToolbar.setPreviewEnabled(resultTable.hasEnoughElements(3));
			resultToolbar.setMergeEnabled(resultTable.hasEnoughElements(2));
			resultToolbar.setExportEnabled(resultTable.hasEnoughElements(1));
			resultToolbar.setUndoEnabled(resultTable.hasEnoughElements(1));
		});
	}

	public void notifyPreview() {
		listeners.stream().forEach(l -> l.onPreviewFrameRequest());
	}

	public void addFrameCreationListener(PreviewFrameCreationListener l) {
		listeners.add(l);
	}

	private void showInputDialog() {
		averageWindowDialog.setLocationRelativeTo(null);
		averageWindowDialog.setVisible(true);
		averageWindowDialog.setConfirmAction(() -> resultTable.merge(averageWindowDialog.value().doubleValue()));
	}

	public ResultTable getResultTable() {
		return resultTable;
	}

}