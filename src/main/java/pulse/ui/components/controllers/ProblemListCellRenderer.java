package pulse.ui.components.controllers;

import static java.awt.Color.black;
import static java.awt.Font.BOLD;
import static javax.swing.BorderFactory.createTitledBorder;

import java.awt.Color;
import java.awt.Component;

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
			renderer.setFont(renderer.getFont().deriveFont(BOLD));
		}

		renderer.setForeground(black);
		renderer.setBackground(color);

		var border = createTitledBorder("Complexity: " + complexity);
		border.setTitleColor(complexity.getColor().darker().darker());
		((JComponent) renderer).setBorder(border);
		return renderer;

	}

	private static Color blend(Color c0, Color c1, int alpha) {
		double totalAlpha = c0.getAlpha() + c1.getAlpha();
		var weight0 = c0.getAlpha() / totalAlpha;
		var weight1 = c1.getAlpha() / totalAlpha;
		var r = weight0 * c0.getRed() + weight1 * c1.getRed();
		var g = weight0 * c0.getGreen() + weight1 * c1.getGreen();
		var b = weight0 * c0.getBlue() + weight1 * c1.getBlue();

		return new Color((int) r, (int) g, (int) b, alpha);
	}

}