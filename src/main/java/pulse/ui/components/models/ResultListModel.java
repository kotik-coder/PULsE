package pulse.ui.components.models;

import static pulse.tasks.ResultFormat.getInstance;
import static pulse.tasks.ResultFormat.getMinimalArray;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import pulse.properties.NumericPropertyKeyword;

public class ResultListModel extends AbstractListModel<NumericPropertyKeyword> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<NumericPropertyKeyword> elements = new ArrayList<NumericPropertyKeyword>();

	public ResultListModel() {
		super();
		update();
	}

	public void update() {
		elements.clear();
		elements.addAll(getInstance().getKeywords());
	}

	@Override
	public int getSize() {
		return elements.size();
	}

	@Override
	public NumericPropertyKeyword getElementAt(int i) {
		return elements.get(i);
	}

	public void add(NumericPropertyKeyword key) {
		elements.add(key);
		var size = this.getSize();
		this.fireContentsChanged(this, size - 1, size);
	}

	public void remove(NumericPropertyKeyword key) {
		if (!elements.contains(key))
			return;

		for (var keyMin : getMinimalArray()) {
			if (key == keyMin)
				return;
		}
		var index = elements.indexOf(key);
		elements.remove(key);
		this.fireContentsChanged(this, index - 1, index);

	}

	public boolean contains(NumericPropertyKeyword key) {
		return elements.contains(key);
	}

	public List<NumericPropertyKeyword> getData() {
		return elements;
	}

}