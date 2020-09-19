package pulse.ui.components;

import static java.awt.Color.WHITE;
import static java.awt.event.ItemEvent.SELECTED;
import static pulse.ui.Messages.getString;

import java.awt.Dimension;
import java.awt.event.ItemEvent;

import javax.swing.JComboBox;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.ui.components.models.TaskBoxModel;

@SuppressWarnings("serial")
public class TaskBox extends JComboBox<SearchTask> {

	private final static int FONT_SIZE = 12;

	public TaskBox() {
		super();

		init();
		this.setModel(new TaskBoxModel());

		var instance = TaskManager.getManagerInstance();
		
		addItemListener((ItemEvent event) -> {
			if (event.getStateChange() == SELECTED) {
				var id = ((SearchTask) this.getModel().getSelectedItem()).getIdentifier();
				/*
				 * if task already selected, just ignore this event and return
				 */
				if (instance.getSelectedTask() != instance.getTask(id)) {
					instance.selectTask(id, this);
				}
				
			}
		});

		instance.addSelectionListener((TaskSelectionEvent e) -> {
			// simply ignore if source of event is taskBox
			if (e.getSource() != this)
				getModel().setSelectedItem(instance.getSelectedTask());
		});
	}

	public void init() {
		setMaximumSize(new Dimension(32767, 24));
		setFont(getFont().deriveFont(FONT_SIZE));
		setMinimumSize(new Dimension(250, 20));
		setToolTipText(getString("TaskBox.DefaultText")); //$NON-NLS-1$
		setBackground(WHITE);
	}

}