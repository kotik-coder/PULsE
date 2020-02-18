package pulse.ui.components;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JToolBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.ui.Messages;


public class SettingsToolBar extends JToolBar {

	private static final long serialVersionUID = -1171612225785102673L;

	private JCheckBox cbSingleStatement, cbHideDetails;
	
	private Font f = new Font(Messages.getString("TaskSelectionToolBar.FontName"), Font.PLAIN, 14); //$NON-NLS-1$
	
	public SettingsToolBar(PropertyHolderTable... tables) {
		super();
		setFloatable(false);
		
		TaskBox taskBox = new TaskBox();
		
		cbSingleStatement = new JCheckBox(Messages.getString("TaskSelectionToolBar.ApplyToAll")); //$NON-NLS-1$
		cbSingleStatement.setSelected( Problem.isSingleStatement() );
		cbSingleStatement.setFont(f);
		
		cbHideDetails = new JCheckBox(Messages.getString("TaskSelectionToolBar.Hide")); //$NON-NLS-1$
		cbHideDetails.setSelected(true);
		cbHideDetails.setFont(f);
		
		setLayout(new GridBagLayout());
		
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.BOTH;
		gbc.weightx = 3.0;
		gbc.gridx = 0;
		gbc.gridy = 0;
				
		add(taskBox, gbc);		
		
		gbc.gridx = 1;
		gbc.weightx = 1.0;
		add(Box.createHorizontalStrut(5), gbc);
		
		gbc.gridx = 2;
		gbc.weightx = 1.0;
		
		add(cbSingleStatement, gbc);
		
		cbSingleStatement.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				Problem.setSingleStatement( cbSingleStatement.isSelected() );
			}
			
		});
		
		gbc.gridx = 3;
		
		add(cbHideDetails, gbc);
		
		cbHideDetails.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
			
				boolean selected = cbHideDetails.isSelected();
				Problem.setDetailsHidden(selected);
				DifferenceScheme.setDetailsHidden(selected);
				for(PropertyHolderTable table : tables)
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
