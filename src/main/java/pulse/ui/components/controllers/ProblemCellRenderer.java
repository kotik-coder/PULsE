package pulse.ui.components.controllers;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

import com.alee.managers.icon.LazyIcon;

import pulse.problem.statements.Problem;
import pulse.util.ImageUtils;

@SuppressWarnings("serial")
public class ProblemCellRenderer extends DefaultTreeCellRenderer {

    private static ImageIcon defaultIcon = (ImageIcon) ((LazyIcon) UIManager.getIcon("Tree.leafIcon")).getIcon();

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
            int row, boolean hasFocus) {

        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        var object = ((DefaultMutableTreeNode) value).getUserObject();
        if (leaf && object instanceof Problem) {
            var icon = ImageUtils.dye(defaultIcon, ((Problem) object).getComplexity().getColor());
            setIcon(icon);
        }
        return this;
    }

}
