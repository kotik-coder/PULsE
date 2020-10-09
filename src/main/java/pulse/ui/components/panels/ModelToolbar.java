package pulse.ui.components.panels;

import static pulse.ui.Launcher.loadIcon;
import static pulse.util.Reflexive.allDescriptors;

import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import pulse.search.statistics.ModelSelectionCriterion;
import pulse.tasks.TaskManager;

@SuppressWarnings("serial")
public class ModelToolbar extends JToolBar {

	private final static int ICON_SIZE = 20;
	
	public ModelToolbar() {
		super();
		setFloatable(false);
		setRollover(true);
		var set = allDescriptors(ModelSelectionCriterion.class);
		var criterionSelection = new JComboBox<>(set.toArray(String[]::new));
		criterionSelection.addActionListener(e -> {
			ModelSelectionCriterion.setSelectedCriterionDescriptor(criterionSelection.getSelectedItem().toString());
		}
		);
		criterionSelection.setSelectedIndex(0);
		
		this.setBorder(BorderFactory.createEtchedBorder());
		
		add(new JLabel("Model Selection Criterion: "));
		add(Box.createRigidArea(new Dimension(5,0)));
		add(criterionSelection);
			
		var bestSelection = new JButton(loadIcon("best_model.png", ICON_SIZE));
		bestSelection.setToolTipText("Select Best Model");
		add(Box.createRigidArea(new Dimension(15,0)));
		add(bestSelection);
		
		bestSelection.addActionListener(e -> {
			var t = TaskManager.getManagerInstance().getSelectedTask();
			t.switchToBestModel();
		});
	
	}

}
