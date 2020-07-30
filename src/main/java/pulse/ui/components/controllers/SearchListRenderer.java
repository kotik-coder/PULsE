package pulse.ui.components.controllers;

import static javax.swing.BorderFactory.createEmptyBorder;

import java.awt.Color;
import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;

public class SearchListRenderer extends DefaultListCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		var renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

		((JComponent) renderer).setBorder(createEmptyBorder(10, 10, 10, 10));
		((JComponent) renderer).setOpaque(true);

		if (isSelected)
			renderer.setBackground(new Color(51, 102, 153, 210));

		return renderer;

	}

}