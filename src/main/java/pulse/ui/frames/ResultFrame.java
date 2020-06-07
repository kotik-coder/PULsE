package pulse.ui.frames;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import pulse.io.export.ExportManager;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.ResultFormat;
import pulse.ui.Messages;
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
		listeners = new ArrayList<PreviewFrameCreationListener>();
		addListeners();
		setVisible(true);
	}

	private void initComponents() {
		JScrollPane resultsScroller = new JScrollPane();

		resultTable = new ResultTable(ResultFormat.DEFAULT_FORMAT);
		resultsScroller.setViewportView(resultTable);
		getContentPane().add(resultsScroller, BorderLayout.CENTER);

		resultToolbar = new ResultToolbar();
		getContentPane().add(resultToolbar, BorderLayout.EAST);

		averageWindowDialog = new FormattedInputDialog(NumericProperty.theDefault(NumericPropertyKeyword.WINDOW));
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
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(resultTable),
							Messages.getString("ResultsToolBar.NoDataError"),
							Messages.getString("ResultsToolBar.NoResultsError"), JOptionPane.ERROR_MESSAGE);
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
				if (resultTable.hasEnoughElements(1)) {
					JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(resultTable),
							Messages.getString("ResultsToolBar.7"), Messages.getString("ResultsToolBar.8"),
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				ExportManager.askToExport(resultTable, (JFrame) SwingUtilities.getWindowAncestor(resultTable),
						"Calculation results");
			}

		});

		resultTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent arg0) {
				resultToolbar.setDeleteEnabled(!resultTable.isSelectionEmpty());
			}

		});

		resultTable.getModel().addTableModelListener(new TableModelListener() {

			@Override
			public void tableChanged(TableModelEvent arg0) {
				resultToolbar.setPreviewEnabled(resultTable.hasEnoughElements(3));
				resultToolbar.setMergeEnabled(resultTable.hasEnoughElements(2));
				resultToolbar.setExportEnabled(resultTable.hasEnoughElements(1));
				resultToolbar.setUndoEnabled(resultTable.hasEnoughElements(1));
			}

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