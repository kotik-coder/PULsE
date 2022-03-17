package pulse.util;

import static pulse.util.Reflexive.allSubclassesNames;
import static pulse.util.Reflexive.instancesOf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pulse.properties.Property;

public class InstanceDescriptor<T extends Reflexive> implements Property {

    private String selectedDescriptor = "";
    private Set<String> allDescriptors;
    private String generalDescriptor;
    private int hashCode;

    private List<DescriptorChangeListener> listeners;

    private static Map<Class<? extends Reflexive>, Set<String>> nameMap = new HashMap<>();

    public InstanceDescriptor(String generalDescriptor, Class<T> c, Object... arguments) {
        if (nameMap.get(c) == null) {
            nameMap.put(c, allSubclassesNames(c));
        }
        this.hashCode = c.hashCode();
        allDescriptors = nameMap.get(c);
        selectedDescriptor = allDescriptors.iterator().next();
        this.generalDescriptor = generalDescriptor;
        listeners = new ArrayList<DescriptorChangeListener>();
    }

    public InstanceDescriptor(Class<T> c, Object... arguments) {
        this(c.getSimpleName(), c, arguments);
    }

    public <K extends Reflexive> K newInstance(Class<K> c, Object... arguments) {
        return instancesOf(c, arguments).stream().filter(r -> getValue().equals(r.getClass().getSimpleName())).findAny()
                .get();
    }

    @Override
    public Object getValue() {
        return selectedDescriptor;
    }

    @Override
    public boolean attemptUpdate(Object object) {
        var string = object.toString();
            
        if (selectedDescriptor.equals(string)) {
            return false;
        }
        
        if(!allDescriptors.contains(string))
            throw new IllegalArgumentException("Unknown descriptor: " + selectedDescriptor);

        this.selectedDescriptor = string;
        listeners.stream().forEach(l -> l.onDescriptorChanged());
        return true;
    }

    public void setSelectedDescriptor(String selectedDescriptor) {
        attemptUpdate(selectedDescriptor);
    }

    @Override
    public Object identifier() {
        return hashCode;
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

        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (!(o instanceof InstanceDescriptor)) {
            return false;
        }

        var descriptor = (InstanceDescriptor<?>) o;

        if (!allDescriptors.containsAll(descriptor.allDescriptors)) {
            return false;
        }

        return selectedDescriptor.equals(descriptor.selectedDescriptor);

    }

}
