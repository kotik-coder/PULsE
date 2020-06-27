package pulse.ui.components.models;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.direction.PathOptimiser;

public class ParameterListModel extends AbstractListModel<NumericPropertyKeyword> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<NumericPropertyKeyword> elements = new ArrayList<NumericPropertyKeyword>();

	public ParameterListModel() {
		super();
		update();
	}

	public void update() {
		elements.clear();
		var list = new ArrayList<Property>();
		PathOptimiser.listAvailableProperties(list);
		list.stream().forEach(property -> elements.add(((Flag) property).getType()));
		elements.add(NumericPropertyKeyword.OPTIMISER_STATISTIC);
		elements.add(NumericPropertyKeyword.TEST_STATISTIC);
		elements.add(NumericPropertyKeyword.IDENTIFIER);
	}

	public int getSize() {
		return elements.size();
	}

	public NumericPropertyKeyword getElementAt(int i) {
		return elements.get(i);
	}

}
