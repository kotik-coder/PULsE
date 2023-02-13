package pulse.ui.components.controllers;

import java.awt.Component;
import java.awt.event.ItemEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;

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

        combobox.addItemListener((ItemEvent e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                try {
                    descriptor.attemptUpdate(e.getItem());
                } catch (NullPointerException npe) {
                    String text = "Error updating " + descriptor.getDescriptor(false)
                            + ". Cannot be set to " + e.getItem();
                    System.out.println(text);
                    npe.printStackTrace();
                }
            }
        });

        return combobox;
    }

    @Override
    public Object getCellEditorValue() {
        descriptor.setSelectedDescriptor((String) combobox.getSelectedItem());
        return descriptor;
    }

}
