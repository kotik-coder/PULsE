package pulse.ui.components.controllers;

import com.alee.utils.swing.PopupMenuAdapter;
import java.awt.Component;
import java.awt.event.ItemEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import pulse.util.InstanceDescriptor;

@SuppressWarnings("serial")
public class InstanceCellEditor extends DefaultCellEditor {

    private InstanceDescriptor<?> descriptor;
    private JComboBox<Object> combobox;

    public InstanceCellEditor(InstanceDescriptor<?> value) {
        super(new JComboBox<Object>(((InstanceDescriptor<?>) value).getAllDescriptors().toArray()));
        this.descriptor = value;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        combobox = new JComboBox<>(((InstanceDescriptor<?>) value).getAllDescriptors().toArray());
        combobox.setSelectedItem(descriptor.getValue());

        combobox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                descriptor.attemptUpdate(e.getItem());
            }
        });

        combobox.addPopupMenuListener(new PopupMenuAdapter() {

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                fireEditingCanceled();
            }

        }
        );

        return combobox;
    }

    @Override
    public Object getCellEditorValue() {
        descriptor.setSelectedDescriptor((String) combobox.getSelectedItem());
        return descriptor;
    }

}
