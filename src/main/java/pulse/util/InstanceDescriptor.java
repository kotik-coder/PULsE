package pulse.util;

import java.util.Set;

import pulse.properties.Property;

public class InstanceDescriptor<T extends PropertyHolder & Reflexive> 
										implements Property {

	private String selectedDescriptor = "";
	private Set<String> allDescriptors;
	private String generalDescriptor;
	
	public InstanceDescriptor(String generalDescriptor, Class<T> c, Object...arguments) {
		allDescriptors = Reflexive.allSubclassesNames(c);
		selectedDescriptor = allDescriptors.iterator().next();
		this.generalDescriptor = generalDescriptor;
	}
	
	public InstanceDescriptor(Class<T> c, Object...arguments) {
		this(c.getSimpleName(), c, arguments);
	}
	
	public <K extends Reflexive> T newInstance(Class<K> c, Object... arguments) {
		var instances = Reflexive.instancesOf(c, arguments);	
			
		for(K r : instances)
			if(getValue().equals(r.getClass().getSimpleName()))
				return (T)r;
			
		return null; //this should never happen
	}

	public void setSelectedDescriptor(String selectedDescriptor) {
		this.selectedDescriptor = selectedDescriptor;
	}

	@Override
	public Object getValue() {
		return selectedDescriptor;
	}
	
	@Override
	public boolean attemptUpdate(Object object) {
		if( ! (object instanceof String) )
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
	
}