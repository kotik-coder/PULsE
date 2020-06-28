package pulse.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.Identifier;

/**
 * An {@code Accessible} that has a list of parameters it accepts as its own and
 * a list of {@code PropertyHolderListener} that track changes with all
 * properties of the {@code Accessible}.
 *
 */

public abstract class PropertyHolder extends Accessible {

	private List<Property> parameters = listedTypes();
	private List<PropertyHolderListener> listeners;
	private String prefix;

	/**
	 * <p>
	 * By default, this will search the children of this {@code PropertyHolder} to
	 * collect the types of their listed parameters recursively. Note this method is
	 * used only to retrieve the type and not the data!
	 * </p>
	 * 
	 * @return a list of {@code Property} instances, which have been explicitly
	 *         marked as a listed parameter for this {@code PropertyHolder}.
	 */

	public List<Property> listedTypes() {

		List<Property> properties = new ArrayList<>();

		for (Accessible accessible : accessibleChildren()) {
                    if (accessible instanceof PropertyHolder)
                        properties.addAll(((PropertyHolder) accessible).listedTypes());
                }

		return properties;
	}

	public PropertyHolder() {
		this.listeners = new ArrayList<>();
	}

	/**
	 * Checks whether {@code p} belongs to the list of parameters for this
	 * {@code PropertyHolder}, i.e. if the associated
	 * {@code NumericPropertyKeyword}s match.
	 * 
	 * @param p the {@code NumericProperty} of a certain type.
	 * @return {@code true} if {@code p} is listed.
	 * @see listedParameters()
	 */

	private boolean isListedNumericType(NumericProperty p) {
		return isListedNumericType(p.getType());
	}

	public boolean isListedNumericType(NumericPropertyKeyword p) {
		if (p == null)
			return false;

		return parameters.stream().filter(pr -> pr instanceof NumericProperty).map(prop -> (NumericProperty) prop)
				.anyMatch(param -> param.getType() == p);

	}

	/**
	 * Checks whether {@code p} belongs to the list of parameters for this
	 * {@code PropertyHolder}, i.e. if the properties are of the same class.
	 * 
	 * @param p the {@code Property}.
	 * @return {@code true} if {@code p} is listed.
	 * @see listedParameters()
	 */

	private boolean isListedGenericType(Property p) {
		if (p == null)
			return false;

		return parameters.stream().anyMatch(param -> param.getClass().equals(p.getClass()));

	}

	/**
	 * Checks whether {@code p}, which is either a generic or a numeric property, is
	 * listed as as parameter for this {@code PropertyHolder}.
	 * 
	 * @param p the {@code Property}
	 * @return {@code true} if {@code p} is listed, {@code false} otherwise.
	 */

	public boolean isListedParameter(Property p) {
		return p instanceof NumericProperty ? isListedNumericType((NumericProperty) p) : isListedGenericType(p);
	}

	/**
	 * Lists all data contained in this {@code PropertyHolder}. The data objects
	 * must satisfy the following conditions: (a) they must be explicitly listed;
	 * (b) the corresponding property must not be auto-adjustable if the details
	 * need to remain hidden.
	 * 
	 * @return a list of data, which combines generic and numeric properties.
	 */

	public List<Property> data() {
		List<NumericProperty> numeric = numericData();
		List<Property> all = genericProperties().stream().filter(p -> isListedGenericType(p))
				.collect(Collectors.toList());

		all.addAll(numeric);
		return all;
	}

	/**
	 * Lists all numeric data contained in this {@code PropertyHolder}. The data
	 * objects must satisfy the following conditions: (a) they must be explicitly
	 * listed; (b) the corresponding property must not be auto-adjustable if the
	 * details need to remain hidden.
	 * 
	 * @return a list of {@code Property} data.
	 * @see areDetailsHidden()
	 * @see pulse.properties.NumericProperty.isAutoAdjustable()
	 * @see isListedNumericType(NumericProperty)
	 */

	public List<NumericProperty> numericData() {
		return numericProperties().stream()
				.filter(p -> (isListedNumericType(p) && (areDetailsHidden() ? !p.isAutoAdjustable() : true)))
				.collect(Collectors.toList());
	}

	/**
	 * Lists all instances of {@code EnumProperty} contained in this
	 * {@code PropertyHolder}. The data objects must be explicitly listed.
	 * 
	 * @return a list of {@code Property} data.
	 * @see isListedGenericType(Property)
	 */

	public List<EnumProperty> enumData() {
		return data().stream().filter(p -> p instanceof EnumProperty).map(eP -> (EnumProperty) eP)
				.collect(Collectors.toList());
	}

	/**
	 * <p>
	 * Attempts to update an {@code updatedProperty} similar to one found in this
	 * {@code PropertyHolder}. The call originator is declared to be the
	 * {@code sourceComponent}. If the originator is not the parent of this
	 * {@code UpwardsNavigable}, this object will tell their parent about this
	 * behavior. The update is done by calling the superclass method
	 * {@code update(Property} -- if and only if a property similar to
	 * {@code updatedProperty} exists and its value is not equal to the
	 * {@code updatedProperty}. When the update happens, this will pass the
	 * corresponding {@code PropertyEvent} to the available listeners.
	 * </p>
	 * 
	 * @param sourceComponent the originator of the change.
	 * @param updatedProperty the updated property that will be assigned to this
	 *                        {@code PropertyHolder}.
	 * @see pulse.util.Accessible.update(Property)
	 */

	public boolean updateProperty(Object sourceComponent, Property updatedProperty) {
		Property existing = property(updatedProperty);

		if (existing == null)
			return false;

		if (existing.equals(updatedProperty))
			return false;

		super.update(updatedProperty);

		notifyListeners(sourceComponent, updatedProperty);
		return true;
	}

	public void notifyListeners(Object source, Property property) {
		PropertyEvent event = new PropertyEvent(source, property);
		listeners.forEach(l -> l.onPropertyChanged(event));

		/*
		 * If the changes are triggered by an external GUI component (such as
		 * PropertyHolderTable), inform parents about this
		 */

		if (source != getParent())
			tellParent(event);

	}

	/**
	 * This method will update this {@code PropertyHolder} with all properties that
	 * are contained in a different {@code propertyHolder}, if they also are present
	 * in the former.
	 * 
	 * @param sourceComponent the source of the change
	 * @param propertyHolder  another {@code PropertyHolder}
	 * @see updateProperty(Object, Property)
	 */

	public void updateProperties(Object sourceComponent, PropertyHolder propertyHolder) {
		propertyHolder.data().stream().forEach(entry -> this.updateProperty(sourceComponent, entry));
	}

	public void removeListeners() {
		this.listeners.clear();
	}

	public void addListener(PropertyHolderListener l) {
		this.listeners.add(l);
	}

	public List<PropertyHolderListener> getListeners() {
		return listeners;
	}

	/**
	 * By default, this is set to {@code false}. If the overriding subclass sets
	 * this to {@code true}, only those {@code NumericPropert}ies that have the
	 * {@code autoAdjustable} flag set {@code false} will be shown.
	 * 
	 * @return {@code true} if the auto-adjustable numeric properties need to stay
	 *         hidden, {@code false} otherwise.
	 * @see pulse.properties.NumericProperty.isAutoAdjustable()
	 */

	public boolean areDetailsHidden() {
		return false;
	}

	public void parameterListChanged() {
		this.parameters = listedTypes();
	}

	/**
	 * Should {@code Accessible}s that belong to this {@code PropertyHolder} be
	 * ignored when this {@code PropertyHolder} is displayed in a table?
	 * 
	 * @return {@code false} by default
	 * @see pulse.ui.components.PropertyHolderTable
	 */

	public boolean ignoreSiblings() {
		return false;
	}

	@Override
	public String describe() {
		if (prefix == null)
			return super.describe();

		Identifier id = identify();

		if (id == null)
			return super.describe();

		if (!prefix.trim().isEmpty())
			return prefix + "_" + id.getValue();
		else
			return describe() + "_" + id.getValue();
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

}