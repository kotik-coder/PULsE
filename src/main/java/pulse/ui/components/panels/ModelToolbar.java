package pulse.ui.components.panels;

import static pulse.util.ImageUtils.loadIcon;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JToolBar;

import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;

@SuppressWarnings("serial")
public class ModelToolbar extends JToolBar {

	private final static int ICON_SIZE = 20;
	
	public ModelToolbar() {
		super();
		setOpaque(false);
		setFloatable(false);
		setRollover(true);
		var set = Calculation.getModelSelectionDescriptor().getAllDescriptors();
		var criterionSelection = new JComboBox<>(set.toArray(String[]::new));
		criterionSelection.addActionListener(e -> 
			Calculation.getModelSelectionDescriptor().setSelectedDescriptor((String)criterionSelection.getSelectedItem())
		);
		criterionSelection.setSelectedItem(Calculation.getModelSelectionDescriptor().getValue());
		
		add(new JLabel("Model Selection Criterion: "));
		add(Box.createRigidArea(new Dimension(5,0)));
		add(criterionSelection);
			
		var doCalc = new JButton(loadIcon("go_estimate.png", ICON_SIZE, Color.WHITE));
		doCalc.setToolTipText("Re-calculate model weights");
		add(Box.createRigidArea(new Dimension(15,0)));
		add(doCalc);
		
		doCalc.addActionListener(e -> {
			var instance = TaskManager.getManagerInstance();
			var t = instance.getSelectedTask();
			t.getStoredCalculations().forEach(c -> c.getModelSelectionCriterion().calcCriterion() );
			instance.notifyListeners(new TaskRepositoryEvent(TaskRepositoryEvent.State.TASK_CRITERION_SWITCH, t.getIdentifier()));
		});
		
		var bestSelection = new JButton(loadIcon("best_model.png", ICON_SIZE, Color.RED));
		bestSelection.setToolTipText("Select Best Model");
		add(Box.createRigidArea(new Dimension(15,0)));
		add(bestSelection);
		
		bestSelection.addActionListener(e -> {
			var t = TaskManager.getManagerInstance().getSelectedTask();
			t.switchToBestModel();
		});
	
	}

}
