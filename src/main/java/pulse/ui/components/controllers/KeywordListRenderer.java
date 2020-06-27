package pulse.ui.components.controllers;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class KeywordListRenderer extends DefaultListCellRenderer {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public KeywordListRenderer() {
		super();
	}

	@Override
	public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
			boolean cellHasFocus) {

		var renderer = super.getListCellRendererComponent(list,
				(NumericProperty.def((NumericPropertyKeyword) value).getDescriptor(true)), index, cellHasFocus,
				cellHasFocus);

		renderer.setForeground(Color.black);
		if (isSelected)
			renderer.setFont(renderer.getFont().deriveFont(Font.BOLD));
		return renderer;

	}

}