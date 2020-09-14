package pulse.util;

import static pulse.util.Reflexive.allSubclassesNames;
import static pulse.util.Reflexive.instancesOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pulse.properties.Property;

public class InstanceDescriptor<T extends Reflexive> implements Property {

	private String selectedDescriptor = "";
	private Set<String> allDescriptors;
	private String generalDescriptor;

	private List<DescriptorChangeListener> listeners = new ArrayList<DescriptorChangeListener>();

	public InstanceDescriptor(String generalDescriptor, Class<T> c, Object... arguments) {
		allDescriptors = allSubclassesNames(c);
		selectedDescriptor = allDescriptors.iterator().next();
		this.generalDescriptor = generalDescriptor;
	}

	public InstanceDescriptor(Class<T> c, Object... arguments) {
		this(c.getSimpleName(), c, arguments);
	}

	public <K extends Reflexive> K newInstance(Class<K> c, Object... arguments) {
		var instances = instancesOf(c, arguments);

		for (var r : instances) {
			if (getValue().equals(r.getClass().getSimpleName()))
				return r;
		}

		return null; // this should never happen
	}

	public void setSelectedDescriptor(String selectedDescriptor) {
		this.selectedDescriptor = selectedDescriptor;
		listeners.stream().forEach(l -> l.onDescriptorChanged());
	}

	@Override
	public Object getValue() {
		return selectedDescriptor;
	}

	@Override
	public boolean attemptUpdate(Object object) {
		if (!(object instanceof String))
			return false;

		if (selectedDescriptor.equals(object))
			return false;

		if (!allDescriptors.contains(object))
			return false;

		setSelectedDescriptor((String) object);
		return true;
	}

	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return generalDescriptor;
	}

	public Set<String> getAllDescriptors() {
		return allDescriptors;
	}

	@Override
	public String toString() {
		return selectedDescriptor;
	}

	public void addListener(DescriptorChangeListener l) {
		this.listeners.add(l);
	}

	public List<DescriptorChangeListener> getListeners() {
		return listeners;
	}

	@Override
	public boolean equals(Object o) {

		if (o == null)
			return false;

		if (o == this)
			return true;

		if (!(o instanceof InstanceDescriptor))
			return false;

		var descriptor = (InstanceDescriptor<?>) o;

		if (!allDescriptors.containsAll(descriptor.allDescriptors))
			return false;

		return selectedDescriptor.equals(descriptor.selectedDescriptor);

	}

}