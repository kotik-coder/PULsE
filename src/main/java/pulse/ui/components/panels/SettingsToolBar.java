package pulse.ui.components.panels;

import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.Box.createHorizontalStrut;
import static pulse.ui.Messages.getString;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.JCheckBox;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.tasks.TaskManager;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.TaskBox;

public class SettingsToolBar extends JToolBar {

	private static final long serialVersionUID = -1171612225785102673L;

	private JCheckBox cbSingleStatement, cbHideDetails;

	public SettingsToolBar(PropertyHolderTable... tables) {
		super();
		setFloatable(false);

		var taskBox = new TaskBox();

		cbSingleStatement = new JCheckBox(getString("TaskSelectionToolBar.ApplyToAll")); //$NON-NLS-1$
		cbSingleStatement.setSelected(TaskManager.getManagerInstance().isSingleStatement());

		cbHideDetails = new JCheckBox(getString("TaskSelectionToolBar.Hide")); //$NON-NLS-1$
		cbHideDetails.setSelected(true);

		setLayout(new GridBagLayout());

		var gbc = new GridBagConstraints();
		gbc.fill = BOTH;
		gbc.weightx = 3.0;
		gbc.gridx = 0;
		gbc.gridy = 0;

		add(taskBox, gbc);

		gbc.gridx = 1;
		gbc.weightx = 1.0;
		add(createHorizontalStrut(5), gbc);

		gbc.gridx = 2;
		gbc.weightx = 1.0;

		add(cbSingleStatement, gbc);

		cbSingleStatement.addChangeListener(e -> TaskManager.getManagerInstance().setSingleStatement(cbSingleStatement.isSelected()));

		gbc.gridx = 3;

		add(cbHideDetails, gbc);

		cbHideDetails.addChangeListener((ChangeEvent e) -> {
			var selected = cbHideDetails.isSelected();
			Problem.setDetailsHidden(selected);
			DifferenceScheme.setDetailsHidden(selected);
			for (var table : tables) {
				table.updateTable();
			}
		});

	}

	public JCheckBox getHideDetailsCheckBox() {
		return cbHideDetails;
	}

	public JCheckBox getSingleStatementCheckBox() {
		return cbHideDetails;
	}

}
