package pulse.ui.components.controllers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

import pulse.problem.statements.Problem;

@SuppressWarnings("serial")
public class ProblemListCellRenderer extends DefaultListCellRenderer {

	public ProblemListCellRenderer() {
		super();
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		var renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		var complexity = ((Problem) value).getComplexity();
		var color = blend(renderer.getBackground(), complexity.getColor(), 15);

		if (isSelected) {
			color = color.darker();
			renderer.setFont(renderer.getFont().deriveFont(Font.BOLD));
		}

		renderer.setForeground(Color.black);
		renderer.setBackground(color);

		var border = BorderFactory.createTitledBorder("Complexity: " + complexity);
		border.setTitleColor(complexity.getColor().darker().darker());
		((JComponent) renderer).setBorder(border);
		return renderer;

	}

	private static Color blend(Color c0, Color c1, int alpha) {
		double totalAlpha = c0.getAlpha() + c1.getAlpha();
		double weight0 = c0.getAlpha() / totalAlpha;
		double weight1 = c1.getAlpha() / totalAlpha;

		double r = weight0 * c0.getRed() + weight1 * c1.getRed();
		double g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
		double b = weight0 * c0.getBlue() + weight1 * c1.getBlue();

		return new Color((int) r, (int) g, (int) b, alpha);
	}

}