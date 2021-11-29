package pulse.ui.frames;

import static java.awt.Font.ITALIC;
import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static pulse.search.direction.PathOptimiser.getInstance;
import static pulse.search.direction.PathOptimiser.setInstance;
import static pulse.ui.Messages.getString;
import static pulse.util.Reflexive.instancesOf;

import java.util.stream.Collectors;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import pulse.search.direction.ActiveFlags;
import pulse.search.direction.LMOptimiser;

import pulse.search.direction.PathOptimiser;
import pulse.tasks.TaskManager;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.controllers.KeywordListRenderer;
import pulse.ui.components.controllers.SearchListRenderer;
import pulse.ui.components.models.ParameterListModel;
import pulse.ui.components.models.ActiveFlagsListModel;
import pulse.ui.components.panels.DoubleListPanel;

@SuppressWarnings("serial")
public class SearchOptionsFrame extends JInternalFrame {

	private PropertyHolderTable pathTable;
        private JList leftList;
        private JList rightList;
	private PathSolversList pathList;

	private final static Font font = new Font(getString("PropertyHolderTable.FontName"), ITALIC, 16);
	private final static List<PathOptimiser> pathSolvers = instancesOf(PathOptimiser.class);
        
        private NumericPropertyKeyword[] mandatorySelection = new NumericPropertyKeyword[]{DIFFUSIVITY, MAXTEMP};
        
	/**
	 * Create the frame.
	 */
	public SearchOptionsFrame() {
		setClosable(true);
		setTitle(getString("SearchOptionsFrame.SelectSearch")); //$NON-NLS-1$
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setBounds(100, 100, WIDTH, HEIGHT);

		/*
		 * Path solver list and scroller
		 */

		var panel = new JPanel();
		panel.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(panel);

		pathList = new PathSolversList();
		var pathListScroller = new JScrollPane(pathList);
		pathListScroller.setBorder(createTitledBorder("Select an Optimiser"));

		pathTable = new PropertyHolderTable(null);
                
		getContentPane().setLayout(new GridBagLayout());

		var gbc = new GridBagConstraints();

		gbc.fill = BOTH;
		gbc.gridy = 0;
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.weighty = 0.3;
                
                leftList = new javax.swing.JList();
                leftList.setModel(new ParameterListModel(false));
		leftList.setCellRenderer(new KeywordListRenderer());
		
                rightList = new javax.swing.JList();
		rightList.setCellRenderer(new KeywordListRenderer());
		
                var mainContainer = new DoubleListPanel(leftList, "All Parameters", rightList, "Optimised Parameters");		
                  
		getContentPane().add(pathListScroller, gbc);

		gbc.gridy = 1;
		gbc.weighty = 0.45;
                
                getContentPane().add(mainContainer, gbc);

                gbc.gridy = 2;
                gbc.weighty = 0.25;
                
		var tableScroller = new JScrollPane(pathTable);
		tableScroller.setBorder(
                        createTitledBorder("Select search variables and settings"));
		getContentPane().add(tableScroller, gbc);

	}

	public void update() {
		var selected = PathOptimiser.getInstance();
		/*
                Select Levenberg-Marquardt as default optimiser
                */
                if (selected == null)
                    pathList.setSelectedValue(LMOptimiser.getInstance(), closable);
                
		pathList.setSelectedIndex(pathSolvers.indexOf(selected));
                ((ParameterListModel)leftList.getModel()).update();
                
                var rightListModel = rightList.getModel();
                var activeTask = TaskManager.getManagerInstance().getSelectedTask();
                
                //model for the flags list already created
                if(rightListModel instanceof ActiveFlagsListModel) {
                    var searchKeys = ActiveFlags.activeParameters(activeTask);
                    ((ActiveFlagsListModel)rightListModel).update(searchKeys);
                }
                //Create a new model for the flags list
                else {
                    if(activeTask != null 
                            && activeTask.getCurrentCalculation() != null
                            && activeTask.getCurrentCalculation().getProblem() != null) {
                        var searchKeys = ActiveFlags.activeParameters(activeTask);
                        rightList.setModel(new ActiveFlagsListModel(searchKeys, mandatorySelection));
                   
                        /*
                        Add listener to this
                        */
                        rightList.getModel().addListDataListener(new ListDataListener() {
                            @Override
                            public void intervalAdded(ListDataEvent arg0) {
                                updateFlag(arg0, true);
                            }

                            @Override
                            public void intervalRemoved(ListDataEvent arg0) {
                                updateFlag(arg0, false);
                            }

                            private void updateFlag(ListDataEvent arg0, boolean value) {
                                var source = (NumericPropertyKeyword)arg0.getSource();
                                var flag = new Flag(source);
                                flag.setValue(value);
                                PathOptimiser.getInstance().update(flag);

                            }

                            @Override
                            public void contentsChanged(ListDataEvent arg0) {
                                throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                            }
                        });
                        
                    }
                }
		pathTable.updateTable();
	}

	class PathSolversList extends JList<PathOptimiser> {

		public PathSolversList() {

			super();

			setModel(new AbstractListModel<PathOptimiser>() {
				/**
				 * 
				 */
				private static final long serialVersionUID = -7683200230096704268L;

				@Override
				public int getSize() {
					return pathSolvers.size();
				}

				@Override
				public PathOptimiser getElementAt(int index) {
					return pathSolvers.get(index);
				}
			});

			setFont(font);
			setSelectionMode(SINGLE_SELECTION);
			setCellRenderer(new SearchListRenderer());

			addListSelectionListener((ListSelectionEvent arg0) -> {
				if (arg0.getValueIsAdjusting())
					return;
				
				var optimiser = getSelectedValue();
				
				setInstance(optimiser);
				pathTable.setPropertyHolder(optimiser);
				
				for (var t : TaskManager.getManagerInstance().getTaskList()) {
					t.checkProblems(true);
				}
			});

		}
	}

}