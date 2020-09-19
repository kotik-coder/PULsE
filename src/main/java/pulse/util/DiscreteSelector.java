package pulse.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pulse.io.readers.AbstractReader;
import pulse.io.readers.ReaderManager;
import pulse.properties.Property;

public class DiscreteSelector<T extends Descriptive> implements Property {

	private Set<T> allOptions;
	private T defaultSelection;
	private T selection;
	
	private List<DescriptorChangeListener> listeners;
	
	public DiscreteSelector(AbstractReader<T> reader, String directory, String listLocation) {
		allOptions = ReaderManager.load(reader, directory, listLocation);
		listeners = new ArrayList<>();
	}
	
	public void addListener(DescriptorChangeListener l) {
		listeners.add(l);
	}
	
	public List<DescriptorChangeListener> getListeners() {
		return listeners;
	}
	
	public void fireDescriptorChange() {
		for(var l : listeners)
			l.onDescriptorChanged();
	}
	
	@Override
	public String toString() {
		return selection.toString();
	}
	
	@Override
	public Object getValue() {
		return selection;
	}

	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return selection.describe();
	}
	
	@Override
	public boolean attemptUpdate(Object value) {
		selection = find(value.toString());
		
		if(selection == null)
			return false;
		
		fireDescriptorChange();
		return true;
	}

	public T find(String name) {
		var optional = allOptions.stream().filter(t -> t.toString().equalsIgnoreCase(name)).findAny();
		return optional.get();
	}

	public T getDefaultSelection() {
		return defaultSelection;
	}

	public void setDefaultSelection(String name) {
		defaultSelection = allOptions.stream().filter(d -> d.toString().equals(name)).findAny().get();
		selection = defaultSelection;
	}

	public Set<T> getAllOptions() {
		return allOptions;
	}

}