package pulse.ui.components;

import static java.awt.Font.BOLD;
import static java.lang.Boolean.TRUE;
import static javax.swing.SortOrder.ASCENDING;
import static pulse.ui.Messages.getString;

import java.awt.Font;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.components.buttons.IconCheckBox;
import pulse.ui.components.controllers.AccessibleTableRenderer;
import pulse.ui.components.controllers.ButtonEditor;
import pulse.ui.components.controllers.InstanceCellEditor;
import pulse.ui.components.controllers.NumberEditor;
import pulse.util.DiscreteSelector;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;

@SuppressWarnings("serial")
public class PropertyHolderTable extends JTable {

	private PropertyHolder propertyHolder;

	private final static Font font = new Font(getString("PropertyHolderTable.FontName"), BOLD, 12);
	private final static int ROW_HEIGHT = 40;

	public PropertyHolderTable(PropertyHolder p) {
		super();
		putClientProperty("terminateEditOnFocusLost", TRUE);

		var model = new DefaultTableModel(dataArray(p), new String[] { getString("PropertyHolderTable.ParameterColumn"), //$NON-NLS-1$
				getString("PropertyHolderTable.ValueColumn") } //$NON-NLS-1$
		);

		setModel(model);

		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);

		setShowGrid(false);
		setFont(font);
		setRowHeight(ROW_HEIGHT);

		var list = new ArrayList<SortKey>();
		list.add(new SortKey(0, ASCENDING));

		setPropertyHolder(p);

		addListeners();

	}

	private void addListeners() {
		/*
		 * Update properties of the PropertyHolder when table is changed by the user
		 */

		getModel().addTableModelListener((TableModelEvent e) -> {

			final int row = e.getFirstRow();
			final int column = e.getColumn();
			
			if ((row < 0) || (column < 0))
				return;

			var changedObject = ((TableModel) e.getSource()).getValueAt(row, column);
			
			if (changedObject instanceof Property) {
				var changedProperty = (Property) changedObject;
				propertyHolder.updateProperty(this, changedProperty);
			}

		});

	}

	private Object[][] dataArray(PropertyHolder p) {
		if (p == null)
			return null;

		List<Object[]> dataList = new ArrayList<>();
		var data = p.data().stream().map(property -> new Object[] { property.getDescriptor(true), property })
				.collect(Collectors.toList());
		dataList.addAll(data);

		if (p.ignoreSiblings())
			return dataList.toArray(new Object[data.size()][2]);

		p.subgroups().stream().filter(group -> group instanceof PropertyHolder).forEach(holder -> dataList.add(
				new Object[] { ((PropertyHolder) holder).getPrefix() != null ? ((PropertyHolder) holder).getPrefix()
						: holder.getDescriptor(), holder })

		);

		return dataList.toArray(new Object[dataList.size()][2]);

	}

	public void setPropertyHolder(PropertyHolder propertyHolder) {
		this.propertyHolder = propertyHolder;
		if (propertyHolder != null) {
			updateTable();
			propertyHolder.addListener(event -> {
				if (!(event.getSource() instanceof PropertyHolderTable))
					updateTable();
			});
		}
	}

	public void updateTable() {
		this.editCellAt(-1, -1);
		this.clearSelection();

		var model = ((DefaultTableModel) getModel());
		model.setDataVector(dataArray(propertyHolder), new String[] { model.getColumnName(0), model.getColumnName(1) });
	}

	@Override
	public TableCellEditor getCellEditor(int row, int column) {

		var value = super.getValueAt(row, column);

		if (value == null)
			super.getCellEditor(row, column);

		// do not edit labels

		if (value instanceof String)
			return null;

		if (value instanceof NumericProperty)
			return new NumberEditor((NumericProperty) value);

		if (value instanceof JComboBox)
			return new DefaultCellEditor((JComboBox<?>) value);

		if (value instanceof Enum)
			return new DefaultCellEditor(
					new JComboBox<Object>(((Enum<?>) value).getDeclaringClass().getEnumConstants()));

		if (value instanceof InstanceDescriptor) {
			var inst = new InstanceCellEditor((InstanceDescriptor<?>) value);
			return inst;
		}

		if (value instanceof DiscreteSelector) {
			var selector = (DiscreteSelector<?>) value;
			var combo = new JComboBox<>(selector.getAllOptions().toArray());
			combo.setSelectedItem(selector.getValue());
			combo.addItemListener(e -> {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					selector.attemptUpdate(e.getItem());
					updateTable();
				}
			});
			return new DefaultCellEditor(combo);
		}

		if ((value instanceof PropertyHolder))
			return new ButtonEditor((AbstractButton) getCellRenderer(row, column).getTableCellRendererComponent(this,
					value, false, false, row, column), (PropertyHolder) value);

		if (value instanceof Flag) 
			return new ButtonEditor((IconCheckBox) getCellRenderer(row, column).getTableCellRendererComponent(this,
					value, false, false, row, column), ((Flag) value).getType());

		return getDefaultEditor(value.getClass());

	}

	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		var value = super.getValueAt(row, column);
		return value != null ? new AccessibleTableRenderer() : super.getCellRenderer(row, column);
	}

	public PropertyHolder getPropertyHolder() {
		return propertyHolder;
	}

}