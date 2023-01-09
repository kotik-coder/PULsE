package pulse.ui.frames;

import static java.awt.Font.ITALIC;
import static java.awt.GridBagConstraints.BOTH;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.ListSelectionModel.SINGLE_SELECTION;
import static pulse.search.direction.PathOptimiser.setInstance;
import static pulse.ui.Messages.getString;
import static pulse.util.Reflexive.instancesOf;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.JInternalFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import pulse.search.direction.ActiveFlags;
import pulse.search.direction.LMOptimiser;

import pulse.search.direction.PathOptimiser;
import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.ui.components.PropertyHolderTable;
import pulse.ui.components.controllers.SearchListRenderer;
import pulse.ui.components.models.ParameterTableModel;
import pulse.ui.components.models.SelectedKeysModel;
import pulse.ui.components.panels.DoubleTablePanel;

@SuppressWarnings("serial")
public class SearchOptionsFrame extends JInternalFrame {

    private final PropertyHolderTable pathTable;
    private final JTable leftTable;
    private final JTable rightTable;
    private final PathSolversList pathList;

    private final static List<PathOptimiser> pathSolvers = instancesOf(PathOptimiser.class);

    private final NumericPropertyKeyword[] mandatorySelection = new NumericPropertyKeyword[]{MAXTEMP};

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

        leftTable = new javax.swing.JTable();
        leftTable.setModel(new ParameterTableModel(false));
        leftTable.setTableHeader(null);
        leftTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        rightTable = new javax.swing.JTable();
        rightTable.setTableHeader(null);
        rightTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
         
        var mainContainer = new DoubleTablePanel(leftTable, "All Parameters", rightTable, "Optimised Parameters");

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
        if (selected == null) {
            pathList.setSelectedValue(LMOptimiser.getInstance(), closable);
        }

        pathList.setSelectedIndex(pathSolvers.indexOf(selected));
        ((ParameterTableModel) leftTable.getModel()).populateWithAllProperties();

        leftTable.setAutoCreateRowSorter(true);
        leftTable.getRowSorter().toggleSortOrder(0);
        
        var rightTblModel = rightTable.getModel();
        var activeTask = TaskManager.getManagerInstance().getSelectedTask();

        //model for the flags list already created
        if (rightTblModel instanceof SelectedKeysModel) {
            var searchKeys = activeTask.activeParameters();
            ((ParameterTableModel)leftTable.getModel()).populateWithAllProperties();
            ((SelectedKeysModel) rightTblModel).update(searchKeys);
        } //Create a new model for the flags list
        else {
            var c = (Calculation)activeTask.getResponse();
            if (c != null && c.getProblem() != null) {
                var searchKeys = activeTask.activeParameters();
                rightTable.setModel(new SelectedKeysModel(searchKeys, mandatorySelection));

                /*
                        Add listener to this
                 */
                rightTable.getModel().addTableModelListener(new TableModelListener() {

                    private void updateFlag(TableModelEvent arg0, boolean value) {
                        var source = (NumericPropertyKeyword) 
                                ( (SelectedKeysModel)rightTable.getModel() )
                                        .getElementAt(arg0.getFirstRow());
                        var flag = new Flag(source);
                        flag.setValue(value);
                        PathOptimiser.getInstance().update(flag);
                    }
                    
                    @Override
                    public void tableChanged(TableModelEvent tme) {
                        if(tme.getType() == TableModelEvent.INSERT)
                            updateFlag(tme, true);
                        else if(tme.getType() == TableModelEvent.DELETE)
                            updateFlag(tme, false);
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

            setSelectionMode(SINGLE_SELECTION);
            setCellRenderer(new SearchListRenderer());

            addListSelectionListener((ListSelectionEvent arg0) -> {
                if (arg0.getValueIsAdjusting()) {
                    return;
                }

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
