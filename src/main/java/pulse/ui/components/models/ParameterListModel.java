package pulse.ui.components.models;

import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;
import static pulse.search.direction.PathOptimiser.listAvailableProperties;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

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
		listAvailableProperties(list);
		list.stream().forEach(property -> elements.add(((Flag) property).getType()));
		elements.add(OPTIMISER_STATISTIC);
		elements.add(TEST_STATISTIC);
		elements.add(IDENTIFIER);
	}

        @Override
	public int getSize() {
		return elements.size();
	}

        @Override
	public NumericPropertyKeyword getElementAt(int i) {
		return elements.get(i);
	}

}
