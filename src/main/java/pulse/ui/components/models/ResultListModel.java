package pulse.ui.components.models;

import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractListModel;

import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.ResultFormat;

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
		elements.addAll(ResultFormat.getInstance().getKeywords());
	}

	public int getSize() {
		return elements.size();
	}

	@Override
	public NumericPropertyKeyword getElementAt(int i) {
		return elements.get(i);
	}

	public void add(NumericPropertyKeyword key) {
		elements.add(key);
		int size = this.getSize();
		this.fireContentsChanged(this, size - 1, size);
	}

	public void remove(NumericPropertyKeyword key) {
		if (!elements.contains(key))
			return;

		for (NumericPropertyKeyword keyMin : ResultFormat.getMinimalArray())
			if (key == keyMin)
				return;

		int index = elements.indexOf(key);
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