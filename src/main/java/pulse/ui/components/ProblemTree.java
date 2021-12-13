package pulse.ui.components;

import static pulse.tasks.TaskManager.getManagerInstance;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import pulse.problem.statements.Problem;
import pulse.problem.statements.ProblemComplexity;
import pulse.ui.components.controllers.ProblemCellRenderer;
import pulse.ui.components.listeners.ProblemSelectionEvent;
import pulse.ui.components.listeners.ProblemSelectionListener;

@SuppressWarnings("serial")
public class ProblemTree extends JTree {

    private List<ProblemSelectionListener> selectionListeners;

    public ProblemTree(List<Problem> allProblems) {
        super();
        this.setCellRenderer(new ProblemCellRenderer());
        var root = new DefaultMutableTreeNode("Problem Statements");

        for (var c : ProblemComplexity.values()) {
            var currentComplexity = new DefaultMutableTreeNode(c.toString() + " Complexity");

            allProblems.stream().filter(p -> p.getComplexity() == c).forEach(pFiltered -> {
                var node = new DefaultMutableTreeNode(pFiltered);
                currentComplexity.add(node);
            });

            root.add(currentComplexity);

        }

        var model = (DefaultTreeModel) this.getModel();
        model.setRoot(root);

        for (int i = 0; i < getRowCount(); i++) {
            expandRow(i);
        }

        this.setRootVisible(false);

        selectionListeners = new ArrayList<>();
        this.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        addListeners();
    }

    private void addListeners() {
        var instance = getManagerInstance();

        addTreeSelectionListener(e -> {
            var object = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
            if (object instanceof Problem) {
                fireProblemSelection(new ProblemSelectionEvent((Problem) object, this));
            }
        });

        instance.addSelectionListener(e -> {
            var current = instance.getSelectedTask().getCurrentCalculation().getProblem();
            // select appropriate problem type from list

            setSelectedProblem(current);
            fireProblemSelection(new ProblemSelectionEvent(current, instance));

        });

    }

    public void setSelectedProblem(Problem p) {
        if (p == null) {
            return;
        }

        var model = this.getModel();
        var root = model.getRoot();

        SwingUtilities.invokeLater(() -> {

            TreePath path = null;

            outer:
            for (int i = 0, size = model.getChildCount(model.getRoot()); i < size; i++) {
                var child = model.getChild(model.getRoot(), i);

                for (int j = 0, cSize = model.getChildCount(child); j < cSize; j++) {
                    var node = (DefaultMutableTreeNode) model.getChild(child, j);
                    var problem = (Problem) node.getUserObject();
                    if (p.getClass().equals(problem.getClass())) {
                        path = new TreePath(new Object[]{root, child, node});
                        break outer;
                    }
                }

            }

            this.setSelectionPath(path);

        });
    }

    public void addProblemSelectionListener(ProblemSelectionListener l) {
        selectionListeners.add(l);
    }

    private void fireProblemSelection(ProblemSelectionEvent e) {
        for (var l : selectionListeners) {
            l.onProblemSelected(e);
        }
    }

}
