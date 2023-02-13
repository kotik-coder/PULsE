package pulse.ui.components.controllers;

import static java.awt.Font.BOLD;

import java.awt.Component;
import javax.swing.JLabel;

import javax.swing.JTable;

import pulse.properties.NumericProperty;
import pulse.tasks.Identifier;
import pulse.tasks.logs.Status;

@SuppressWarnings("serial")
public class TaskTableRenderer extends NumericPropertyRenderer {

    public TaskTableRenderer() {
        super();
    }

    @Override

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        var superRenderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        if (value instanceof Identifier) {
            var lab = (JLabel) superRenderer;
            lab.setHorizontalAlignment(JLabel.CENTER);
        } else if (value instanceof NumericProperty) {
            return superRenderer;
        } else if (value instanceof Status) {

            superRenderer.setForeground(((Status) value).getColor());
            superRenderer.setFont(superRenderer.getFont().deriveFont(BOLD));
            ((JLabel) superRenderer).setHorizontalAlignment(JLabel.CENTER);

            return superRenderer;

        }

        return superRenderer;

    }

}
