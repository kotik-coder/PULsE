package pulse.ui.components;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;

import javax.swing.ButtonGroup;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;

import pulse.io.export.ExportManager;
import pulse.io.readers.DataLoader;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.NormalityTest;
import pulse.search.statistics.ResidualStatistic;
import pulse.tasks.Buffer;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.ui.Launcher;
import pulse.ui.components.listeners.ExitRequestListener;
import pulse.ui.components.listeners.FrameVisibilityRequestListener;
import pulse.ui.frames.dialogs.AboutDialog;
import pulse.ui.frames.dialogs.ExportDialog;
import pulse.ui.frames.dialogs.FormattedInputDialog;
import pulse.ui.frames.dialogs.ResultChangeDialog;
import pulse.util.Reflexive;

@SuppressWarnings("serial")
public class PulseMainMenu extends JMenuBar {

	private final static int ICON_SIZE = 24;

	private static JMenuItem aboutItem;
	private static JMenu dataControlsMenu;
	private static JMenuItem exitItem;
	private static JMenuItem exportAllItem;
	private static JMenuItem exportCurrentItem;
	private static JMenuItem loadDataItem;
	private static JMenuItem resultFormatItem;
	private static JMenuItem searchSettingsItem;
	private static JMenuItem loadMetadataItem;
	private static JMenuItem modelSettingsItem;

	private static ExportDialog exportDialog = new ExportDialog();
	private static FormattedInputDialog bufferDialog = new FormattedInputDialog(
			NumericProperty.theDefault(NumericPropertyKeyword.BUFFER_SIZE));

	private static File dir;

	private List<FrameVisibilityRequestListener> listeners;
	private List<ExitRequestListener> exitListeners;

	public PulseMainMenu() {
		bufferDialog.setConfirmAction(
				() -> Buffer.setSize(NumericProperty.derive(NumericPropertyKeyword.BUFFER_SIZE, bufferDialog.value())));

		initComponents();
		initListeners();
		assignMenuFunctions();
		addListeners();

		listeners = new ArrayList<>();
		exitListeners = new ArrayList<>();
	}

	private void addListeners() {
		TaskManager.addTaskRepositoryListener(event -> {
			if (event.getState() == TaskRepositoryEvent.State.TASK_ADDED) {
				exportCurrentItem.setEnabled(true);
				exportAllItem.setEnabled(true);
			}
		});
	}

	private void initListeners() {
		exportCurrentItem.addActionListener(e -> {
			SearchTask selectedTask = TaskManager.getSelectedTask();

			if (selectedTask == null) {
				JOptionPane.showMessageDialog(SwingUtilities.getWindowAncestor(this), "No data to export!",
						"No Data to Export", JOptionPane.WARNING_MESSAGE);
				return;
			}

			var fileChooser = new JFileChooser();
			fileChooser.setMultiSelectionEnabled(false);
			fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

			int returnVal = fileChooser.showSaveDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				dir = new File(fileChooser.getSelectedFile() + File.separator + TaskManager.getInstance().describe());
				dir.mkdirs();
				ExportManager.exportCurrentTask(dir);
			}

		});

		exitItem.addActionListener(e -> notifyExit());

	}

	private void initComponents() {
		dataControlsMenu = new JMenu("File");
		loadDataItem = new JMenuItem("Load Heating Curve(s)...", Launcher.loadIcon("load.png", ICON_SIZE));
		loadMetadataItem = new JMenuItem("Load Metadata...", Launcher.loadIcon("metadata.png", ICON_SIZE));
		exportCurrentItem = new JMenuItem("Export Current", Launcher.loadIcon("save.png", ICON_SIZE));
		exportAllItem = new JMenuItem("Export...", Launcher.loadIcon("save.png", ICON_SIZE));
		exitItem = new JMenuItem("Exit");
		var settingsMenu = new JMenu("Calculation Settings");
		modelSettingsItem = new JMenuItem("Heat Problem: Statement & Solution",
				Launcher.loadIcon("heat_problem.png", ICON_SIZE));
		searchSettingsItem = new JMenuItem("Parameter Estimation: Method & Settings",
				Launcher.loadIcon("inverse_problem.png", ICON_SIZE));
		resultFormatItem = new JMenuItem("Change Result Format...", Launcher.loadIcon("result_format.png", ICON_SIZE));
		var infoMenu = new JMenu("Info");
		aboutItem = new JMenuItem("About...");
		var selectBuffer = new JMenuItem("Buffer size...", Launcher.loadIcon("buffer.png", ICON_SIZE));
		selectBuffer.addActionListener(e -> bufferDialog.setVisible(true));

		dataControlsMenu.setMnemonic('f');
		loadDataItem.setMnemonic('h');
		loadMetadataItem.setMnemonic('m');
		loadMetadataItem.setMnemonic('m');
		exportCurrentItem.setMnemonic('c');
		exportAllItem.setMnemonic('a');
		exitItem.setMnemonic('x');
		settingsMenu.setMnemonic('e');

		loadMetadataItem.setEnabled(false);
		exportCurrentItem.setEnabled(false);
		exportAllItem.setEnabled(false);
		modelSettingsItem.setEnabled(false);
		searchSettingsItem.setEnabled(false);

		Font menuFont = new Font("Arial", Font.PLAIN, 18);
		dataControlsMenu.setFont(menuFont);
		settingsMenu.setFont(menuFont);
		infoMenu.setFont(menuFont);

		dataControlsMenu.add(loadDataItem);
		dataControlsMenu.add(loadMetadataItem);
		dataControlsMenu.add(new JSeparator());
		dataControlsMenu.add(exportCurrentItem);
		dataControlsMenu.add(exportAllItem);
		dataControlsMenu.add(new JSeparator());
		dataControlsMenu.add(exitItem);
		add(dataControlsMenu);

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
		JMenu analysisSubMenu = new JMenu("Statistical Analysis");
		JMenu statisticsSubMenu = new JMenu("Normality tests");
		statisticsSubMenu.setIcon(Launcher.loadIcon("normality_test.png", ICON_SIZE));

		ButtonGroup statisticItems = new ButtonGroup();

		JRadioButtonMenuItem item = null;

		for (String statisticName : Reflexive.allDescriptors(NormalityTest.class)) {
			item = new JRadioButtonMenuItem(statisticName);
			statisticItems.add(item);
			statisticsSubMenu.add(item);
			item.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					NormalityTest.setSelectedTestDescriptor(text);

					TaskManager.getTaskList().stream().forEach(t -> t.initNormalityTest());

				}

			});
		}

		var significanceDialog = new FormattedInputDialog(
				NumericProperty.theDefault(NumericPropertyKeyword.SIGNIFICANCE));

		significanceDialog.setConfirmAction(() -> NormalityTest.setStatisticalSignificance(
				NumericProperty.derive(NumericPropertyKeyword.SIGNIFICANCE, significanceDialog.value())));

		JMenuItem sigItem = new JMenuItem("Change significance...");
		statisticsSubMenu.add(new JSeparator());
		statisticsSubMenu.add(sigItem);
		sigItem.addActionListener(e -> significanceDialog.setVisible(true));

		statisticsSubMenu.getItem(0).setSelected(true);
		analysisSubMenu.add(statisticsSubMenu);

		JMenu optimisersSubMenu = new JMenu("Optimiser statistics");
		optimisersSubMenu.setIcon(Launcher.loadIcon("optimiser.png", ICON_SIZE));

		ButtonGroup optimisersItems = new ButtonGroup();

		item = null;

		var set = Reflexive.allDescriptors(ResidualStatistic.class);
		set.removeAll(Reflexive.allDescriptors(NormalityTest.class));

		for (String statisticName : set) {
			item = new JRadioButtonMenuItem(statisticName);
			optimisersItems.add(item);
			optimisersSubMenu.add(item);
			item.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					ResidualStatistic.setSelectedOptimiserDescriptor(text);
					TaskManager.getTaskList().stream().forEach(t -> t.initOptimiser());
				}

			});
		}

		optimisersSubMenu.getItem(0).setSelected(true);
		analysisSubMenu.add(optimisersSubMenu);

		//

		JMenu correlationsSubMenu = new JMenu("Correlation tests");
		correlationsSubMenu.setIcon(Launcher.loadIcon("correlation.png", ICON_SIZE));

		ButtonGroup corrItems = new ButtonGroup();

		JRadioButtonMenuItem corrItem = null;

		for (String corrName : Reflexive.allDescriptors(CorrelationTest.class)) {
			corrItem = new JRadioButtonMenuItem(corrName);
			corrItems.add(corrItem);
			correlationsSubMenu.add(corrItem);
			corrItem.addItemListener(e -> {

				if (((AbstractButton) e.getItem()).isSelected()) {
					var text = ((AbstractButton) e.getItem()).getText();
					CorrelationTest.setSelectedTestDescriptor(text);
					TaskManager.getTaskList().stream().forEach(t -> t.initCorrelationTest());
				}

			});
		}

		var thresholdDialog = new FormattedInputDialog(
				NumericProperty.theDefault(NumericPropertyKeyword.CORRELATION_THRESHOLD));

		thresholdDialog.setConfirmAction(() -> CorrelationTest.setThreshold(
				NumericProperty.derive(NumericPropertyKeyword.CORRELATION_THRESHOLD, thresholdDialog.value())));

		JMenuItem thrItem = new JMenuItem("Change threshold...");
		correlationsSubMenu.add(new JSeparator());
		correlationsSubMenu.add(thrItem);
		thrItem.addActionListener(e -> thresholdDialog.setVisible(true));

		correlationsSubMenu.getItem(0).setSelected(true);

		analysisSubMenu.add(correlationsSubMenu);
		return analysisSubMenu;
	}

	private void assignMenuFunctions() {
		loadDataItem.addActionListener(e -> DataLoader.loadDataDialog());
		loadMetadataItem.setEnabled(false);
		loadMetadataItem.addActionListener(e -> DataLoader.loadMetadataDialog());

		modelSettingsItem.setEnabled(false);

		modelSettingsItem.addActionListener(e -> notifyProblem());
		searchSettingsItem.addActionListener(e -> notifySearch());

		searchSettingsItem.setEnabled(false);

		resultFormatItem.addActionListener(e -> {
			ResultChangeDialog changeDialog = new ResultChangeDialog();
			changeDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
			changeDialog.setAlwaysOnTop(true);
			changeDialog.setVisible(true);
		});

		TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

			@Override
			public void onTaskListChanged(TaskRepositoryEvent e) {
				if (TaskManager.getTaskList().size() > 0) {
					loadMetadataItem.setEnabled(true);
					modelSettingsItem.setEnabled(true);
					searchSettingsItem.setEnabled(true);
				} else {
					loadMetadataItem.setEnabled(false);
					modelSettingsItem.setEnabled(false);
					searchSettingsItem.setEnabled(false);
				}
			}

		});

		exportAllItem.setEnabled(true);
		exportAllItem.addActionListener(e -> {
			exportDialog.setLocationRelativeTo(null);
			exportDialog.setAlwaysOnTop(true);
			exportDialog.setVisible(true);
		});

		aboutItem.addActionListener(e -> {
			var aboutDialog = new AboutDialog();
			aboutDialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
			aboutDialog.setAlwaysOnTop(true);
			aboutDialog.setVisible(true);
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