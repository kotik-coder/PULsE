package pulse.ui.components.controllers;

import static javax.swing.SwingConstants.CENTER;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.JFrame;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.components.buttons.IconCheckBox;
import pulse.ui.frames.DataFrame;
import pulse.util.PropertyHolder;

@SuppressWarnings("serial")
public class ButtonEditor extends AbstractCellEditor implements TableCellEditor {

	private AbstractButton btn;
	private PropertyHolder dat;
	private NumericPropertyKeyword type;

	public ButtonEditor(AbstractButton btn, PropertyHolder dat) {
		this.btn = btn;
		this.dat = dat;

		btn.addActionListener((ActionEvent e) -> {
			JFrame dataFrame = new DataFrame(dat, btn);
			dataFrame.setVisible(true);
			btn.setEnabled(false);
			dataFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosed(WindowEvent we) {
					btn.setText(((DataFrame) dataFrame).getDataObject().toString());
					btn.setEnabled(true);
				}
			});
		});

	}

	public ButtonEditor(IconCheckBox btn, NumericPropertyKeyword index) {
		this.btn = btn;
		this.type = index;

		btn.addActionListener((ActionEvent e) -> {
			var source = (IconCheckBox) e.getSource();
			source.setHorizontalAlignment(CENTER);
			stopCellEditing();
		});

	}

	@Override
	public Object getCellEditorValue() {
		if (dat != null)
			return dat;
		var f = new Flag(type);
		f.setValue(btn.isSelected());
		return f;
	}

	@Override
	public Component getTableCellEditorComponent(JTable arg0, Object value, boolean arg2, int arg3, int arg4) {
		return btn;
	}

}