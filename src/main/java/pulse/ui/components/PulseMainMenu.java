package pulse.ui.components;

import static java.io.File.separator;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JOptionPane.WARNING_MESSAGE;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.SwingUtilities.getWindowAncestor;
import static pulse.io.export.ExportManager.exportCurrentTask;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.BUFFER_SIZE;
import static pulse.properties.NumericPropertyKeyword.CORRELATION_THRESHOLD;
import static pulse.properties.NumericPropertyKeyword.SIGNIFICANCE;
import static pulse.search.statistics.CorrelationTest.setThreshold;
import static pulse.search.statistics.NormalityTest.setStatisticalSignificance;
import static pulse.search.statistics.OptimiserStatistic.setSelectedOptimiserDescriptor;
import static pulse.tasks.TaskManager.getManagerInstance;
import static pulse.tasks.listeners.TaskRepositoryEvent.State.TASK_ADDED;
import static pulse.ui.components.DataLoader.loadDataDialog;
import static pulse.ui.components.DataLoader.loadMetadataDialog;
import static pulse.util.ImageUtils.loadIcon;
import static pulse.util.Reflexive.allDescriptors;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;

import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.search.statistics.OptimiserStatistic;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.processing.Buffer;
import pulse.ui.components.listeners.ExitRequestListener;
import pulse.ui.components.listeners.FrameVisibilityRequestListener;
import pulse.ui.frames.dialogs.ExportDialog;
import pulse.ui.frames.dialogs.FormattedInputDialog;
import pulse.ui.frames.dialogs.ResultChangeDialog;

@SuppressWarnings("serial")
public class PulseMainMenu extends JMenuBar {

	private final static int ICON_SIZE = 24;

	private static JMenuItem aboutItem;
	private static JMenu fileMenu;
	private static JMenuItem exitItem;
	private static JMenuItem exportAllItem;
	private static JMenuItem exportCurrentItem;
	private static JMenuItem loadDataItem;
	private static JMenuItem resultFormatItem;
	private static JMenuItem searchSettingsItem;
	private static JMenuItem loadMetadataItem;
	private static JMenuItem modelSettingsItem;

	private static ExportDialog exportDialog = new ExportDialog();
	private static FormattedInputDialog bufferDialog = new FormattedInputDialog(def(BUFFER_SIZE));

	private static File dir;

	private List<FrameVisibilityRequestListener> listeners;
	private List<ExitRequestListener> exitListeners;

	public PulseMainMenu() {
		bufferDialog.setConfirmAction(() -> Buffer.setSize(derive(BUFFER_SIZE, bufferDialog.value())));

		initComponents();
		initListeners();
		assignMenuFunctions();
		addListeners();

		listeners = new ArrayList<>();
		exitListeners = new ArrayList<>();
	}

	private void addListeners() {
		getManagerInstance().addTaskRepositoryListener(event -> {
			if (event.getState() == TASK_ADDED) {
				exportCurrentItem.setEnabled(true);
				exportAllItem.setEnabled(true);
			}
		});
	}

	private void initListeners() {
		exportCurrentItem.addActionListener(e -> {
			var selectedTask = getManagerInstance().getSelectedTask();

			if (selectedTask == null) {
				showMessageDialog(getWindowAncestor(this), "No data to export!", "No Data to Export", WARNING_MESSAGE);
				return;
			}

			var fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileSelectionMode(DIRECTORIES_ONLY);

			var returnVal = fileChooser.showSaveDialog(this);

			if (returnVal == APPROVE_OPTION) {
				dir = new File(fileChooser.getSelectedFile() + separator + getManagerInstance().describe());
				dir.mkdirs();
				exportCurrentTask(dir);
			}

		});

		exitItem.addActionListener(e -> notifyExit());

	}

	private void initComponents() {
		fileMenu = new JMenu("File");
		loadDataItem = new JMenuItem("Load Heating Curve(s)...", loadIcon("load.png", ICON_SIZE));
		loadMetadataItem = new JMenuItem("Load Metadata...", loadIcon("metadata.png", ICON_SIZE));
		exportCurrentItem = new JMenuItem("Export Current", loadIcon("save.png", ICON_SIZE));
		exportAllItem = new JMenuItem("Export...", loadIcon("save.png", ICON_SIZE));
		exitItem = new JMenuItem("Exit");
		var settingsMenu = new JMenu("Calculation Settings");
		modelSettingsItem = new JMenuItem("Heat Problem: Statement & Solution",
				loadIcon("heat_problem.png", ICON_SIZE));
		searchSettingsItem = new JMenuItem("Parameter Estimation: Method & Settings",
				loadIcon("inverse_problem.png", ICON_SIZE));
		resultFormatItem = new JMenuItem("Change Result Format...", loadIcon("result_format.png", ICON_SIZE));
		var infoMenu = new JMenu("Info");
		aboutItem = new JMenuItem("PULsE Web-site");
		var selectBuffer = new JMenuItem("Buffer size...", loadIcon("buffer.png", ICON_SIZE));
		selectBuffer.addActionListener(e -> bufferDialog.setVisible(true));

		fileMenu.setMnemonic('f');
		loadDataItem.setMnemonic('h');
		loadMetadataItem.setMnemonic('m');
		exportCurrentItem.setMnemonic('c');
		exportAllItem.setMnemonic('e');
		exitItem.setMnemonic('x');
		aboutItem.setMnemonic('a');
		settingsMenu.setMnemonic('s');

		loadMetadataItem.setEnabled(false);
		exportCurrentItem.setEnabled(false);
		exportAllItem.setEnabled(false);
		modelSettingsItem.setEnabled(false);
		searchSettingsItem.setEnabled(false);

		fileMenu.add(loadDataItem);
		fileMenu.add(loadMetadataItem);
		fileMenu.add(new JSeparator());
		fileMenu.add(exportCurrentItem);
		fileMenu.add(exportAllItem);
		fileMenu.add(new JSeparator());
		fileMenu.add(exitItem);
		add(fileMenu);

		settingsMenu.add(modelSettingsItem);
		settingsMenu.add(searchSettingsItem);
		settingsMenu.add(initAnalysisSubmenu());
		settingsMenu.add(new JSeparator());
		settingsMenu.add(resultFormatItem);
		settingsMenu.add(selectBuffer);

		add(settingsMenu);

		infoMenu.add(aboutItem);
		add(infoMenu);
	}

	private JMenu initAnalysisSubmenu() {
		var analysisSubMenu = new JMenu("Statistical Analysis");
		var statisticsSubMenu = new JMenu("Normality tests");
		statisticsSubMenu.setIcon(loadIcon("normality_test.png", ICON_SIZE));

		var statisticItems = new ButtonGroup();

		JRadioButtonMenuItem item = null;

		for (var statisticName : allDescriptors(NormalityTest.class)) {
			item = new JRadioButtonMenuItem(statisticName);
			statisticItems.add(item);
			statisticsSubMenu.add(item);
			item.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					NormalityTest.setSelectedTestDescriptor(text);

					getManagerInstance().getTaskList().stream().forEach(t -> t.initNormalityTest());

				}

			});
		}

		var significanceDialog = new FormattedInputDialog(def(SIGNIFICANCE));

		significanceDialog
				.setConfirmAction(() -> setStatisticalSignificance(derive(SIGNIFICANCE, significanceDialog.value())));

		var sigItem = new JMenuItem("Change significance...");
		statisticsSubMenu.add(new JSeparator());
		statisticsSubMenu.add(sigItem);
		sigItem.addActionListener(e -> significanceDialog.setVisible(true));

		statisticsSubMenu.getItem(0).setSelected(true);
		analysisSubMenu.add(statisticsSubMenu);

		var optimisersSubMenu = new JMenu("Optimiser statistics");
		optimisersSubMenu.setIcon(loadIcon("optimiser.png", ICON_SIZE));

		var optimisersItems = new ButtonGroup();

		item = null;

		var set = allDescriptors(OptimiserStatistic.class);

		for (var statisticName : set) {
			item = new JRadioButtonMenuItem(statisticName);
			optimisersItems.add(item);
			optimisersSubMenu.add(item);
			item.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					setSelectedOptimiserDescriptor(text);
					getManagerInstance().getTaskList().stream().forEach(t -> t.getCurrentCalculation().initOptimiser());
				}

			});
		}

		optimisersSubMenu.getItem(0).setSelected(true);
		analysisSubMenu.add(optimisersSubMenu);

		//

		var correlationsSubMenu = new JMenu("Correlation tests");
		correlationsSubMenu.setIcon(loadIcon("correlation.png", ICON_SIZE));

		var corrItems = new ButtonGroup();

		JRadioButtonMenuItem corrItem = null;

		for (var corrName : allDescriptors(CorrelationTest.class)) {
			corrItem = new JRadioButtonMenuItem(corrName);
			corrItems.add(corrItem);
			correlationsSubMenu.add(corrItem);
			corrItem.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					CorrelationTest.setSelectedTestDescriptor(text);
					getManagerInstance().getTaskList().stream().forEach(t -> t.initCorrelationTest());
				}

			});
		}

		var thresholdDialog = new FormattedInputDialog(def(CORRELATION_THRESHOLD));

		thresholdDialog.setConfirmAction(() -> setThreshold(derive(CORRELATION_THRESHOLD, thresholdDialog.value())));

		var thrItem = new JMenuItem("Change threshold...");
		correlationsSubMenu.add(new JSeparator());
		correlationsSubMenu.add(thrItem);
		thrItem.addActionListener(e -> thresholdDialog.setVisible(true));

		correlationsSubMenu.getItem(0).setSelected(true);

		analysisSubMenu.add(correlationsSubMenu);
		return analysisSubMenu;
	}

	private void assignMenuFunctions() {
		loadDataItem.addActionListener(e -> loadDataDialog());
		loadMetadataItem.setEnabled(false);
		loadMetadataItem.addActionListener(e -> loadMetadataDialog());

		modelSettingsItem.setEnabled(false);

		modelSettingsItem.addActionListener(e -> notifyProblem());
		searchSettingsItem.addActionListener(e -> notifySearch());

		searchSettingsItem.setEnabled(false);

		resultFormatItem.addActionListener(e -> {
			var changeDialog = new ResultChangeDialog();
			changeDialog.setLocationRelativeTo(getWindowAncestor(this));
			changeDialog.setAlwaysOnTop(true);
			changeDialog.setVisible(true);
		});

		getManagerInstance().addTaskRepositoryListener((TaskRepositoryEvent e) -> {
			if (getManagerInstance().getTaskList().size() > 0) {
				loadMetadataItem.setEnabled(true);
				modelSettingsItem.setEnabled(true);
				searchSettingsItem.setEnabled(true);
			} else {
				loadMetadataItem.setEnabled(false);
				modelSettingsItem.setEnabled(false);
				searchSettingsItem.setEnabled(false);
			}
		});

		exportAllItem.setEnabled(true);
		exportAllItem.addActionListener(e -> {
			exportDialog.setLocationRelativeTo(null);
			exportDialog.setAlwaysOnTop(true);
			exportDialog.setVisible(true);
		});

		aboutItem.addActionListener(e -> {
			try {
				Desktop.getDesktop().browse(new URL("https://kotik-coder.github.io/").toURI());
			} catch (IOException | URISyntaxException e1) {
				System.err.println("Unable to open URL. Details: ");
				e1.printStackTrace();
			}

		});

	}

	public void addFrameVisibilityRequestListener(FrameVisibilityRequestListener l) {
		listeners.add(l);
	}

	public void addExitRequestListener(ExitRequestListener el) {
		exitListeners.add(el);
	}

	public void notifyProblem() {
		listeners.stream().forEach(l -> l.onProblemStatementShowRequest());
	}

	public void notifySearch() {
		listeners.stream().forEach(l -> l.onSearchSettingsShowRequest());
	}

	public void notifyExit() {
		exitListeners.stream().forEach(el -> el.onExitRequested());
	}

}