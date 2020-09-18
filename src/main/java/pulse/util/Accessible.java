package pulse.util;

import static java.lang.System.err;
import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * <p>
 * An {@code Accessible} provides Reflection-based read- and write-access to the
 * underlying (usually declared as its own fields - but not necessarily)
 * instances of {@code Property} and a recursive access to other
 * {@code Accessible}, which may have a family relationship with {@code this}
 * {@code Accessible} via the {@code UpwardsNavigable} implementation. It also
 * defines its own list of {@code Saveable}s.
 * </p>
 *
 */

public abstract class Accessible extends Group {

	/**
	 * <p>
	 * Searches for a {@code Property} in this {@code Accessible} that looks
	 * {@code similar} to the argument. Determines whether the {@code similar} is a
	 * {@code NumericProperty} or a generic property and calls the suitable method
	 * in this class.
	 * </p>
	 * 
	 * @param similar a generic or a numeric {@code Property}
	 * @return the matching property of this {@code Accessible}
	 */

	public Property property(Property similar) {
		if (similar instanceof NumericProperty)
			return numericProperty(((NumericProperty) similar).getType());
		else
			return genericProperty(similar);
	}

	/**
	 * Tries to access the property getter methods in this {@code Accessible}, which
	 * should be declared as no-argument methods with a specific return type.
	 * 
	 * @return This will return a unique {@code Set<NumericProperty>} containing all
	 *         instances of {@code NumericProperty} belonging to this
	 *         {@code Accessible}. This set will not contain any duplicate elements
	 *         by definition.
	 * @see pulse.properties.NumericProperty.equal(Object)
	 */

	public Set<NumericProperty> numericProperties() {
		Set<NumericProperty> fields = new TreeSet<>();

		var methods = this.getClass().getMethods();
		for (var m : methods) {

			if (m.getParameterCount() > 0)
				continue;

			if (NumericProperty.class.isAssignableFrom(m.getReturnType()))
				try {
					var obj = m.invoke(this);
					if (obj != null)
						fields.add((NumericProperty) m.invoke(this));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					err.println("Error invoking method " + m);
					e.printStackTrace();
				}

		}

		/*
		 * Get access to the numeric properties of accessibles contained in this
		 * accessible
		 */
		/*
		 * for (var a : accessibleChildren()) { fields.addAll(a.numericProperties()); }
		 */

		return fields;

	}

	/**
	 * Tries to access the property getter methods in this {@code Accessible}, which
	 * should be declared as no-argument methods with a specific return type.
	 * 
	 * @return This will return a {@code List<Property>} containing all properties
	 *         belonging to this {@code Accessible}, which are not assignable from
	 *         the {@code NumericProperty} class.
	 */

	public List<Property> genericProperties() {
		List<Property> fields = new ArrayList<>();

		var methods = this.getClass().getMethods();
		for (var m : methods) {

			if (m.getParameterCount() > 0)
				continue;

			if (Property.class.isAssignableFrom(m.getReturnType())
					&& !NumericProperty.class.isAssignableFrom(m.getReturnType()))
				try {
					fields.add((Property) m.invoke(this));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					err.println("Error invoking method " + m);
					e.printStackTrace();
				}

		}
		/*
		 * Get access to the properties of accessibles contained in this accessible
		 */

//		for (var a : accessibleChildren()) {
//			fields.addAll(a.genericProperties());
//		}

		return fields;

	}

	/**
	 * <p>
	 * Recursively searches for a {@code NumericProperty} from the unique set of
	 * numeric properties in this {@code Accessible} by comparing its
	 * {@code NumericPropertyKeyword} to {@code type}. This will search for this
	 * property through the children of this object, through the children of their
	 * children, etc.
	 * </p>
	 * 
	 * @param type the type of the {@code NumericProperty}.
	 * @return the respective {@code NumericProperty}, or {@code null} if nothing is
	 *         found.
	 * @see numericProperties()
	 */

	public NumericProperty numericProperty(NumericPropertyKeyword type) {

		var match = numericProperties().stream().filter(p -> p.getType() == type).findFirst();

		if (match.isPresent())
			return match.get();

		NumericProperty property = null;

		for (var accessible : accessibleChildren()) {
			property = accessible.numericProperty(type);
			if (property != null)
				break;
		}

		return property;

	}

	/**
	 * <p>
	 * Recursively searches for a {@code Property} from the non-unique list of
	 * generic properties in this {@code Accessible} by comparing its class to
	 * {@code sameClass.getClass()}. This will search for this property through the
	 * children of this object, through the children of their children, etc.
	 * </p>
	 * 
	 * @param sameClass the class identifying this {@code Property}.
	 * @return the respective {@code Property}, or {@code null} if nothing is found.
	 * @see genericProperties()
	 */

	public Property genericProperty(Property sameClass) {

		var match = genericProperties().stream().filter(p -> p.identifier().equals(sameClass.identifier()))
				.collect(Collectors.toList());

		Property result = null;
		
		switch (match.size()) {
		case 0:
			
			break;
		// just one matching element found
		case 1:
			result = match.get(0);
			break;
		// several possible matches found; use other criteria
		default:
			throw new IllegalArgumentException("Too many matches found: " + sameClass + " : " + match.size());
		}

		return result;

	}

	/**
	 * <p>
	 * An abstract method, which must be overriden to gain access over setting the
	 * values of all relevant (selected by the programmer) {@code NumericPropert}ies
	 * in subclasses of {@code Accessible}. Typically this involves a {@code switch}
	 * statement that goes through the different options for the {@code type} and
	 * invokes different {@code set(...)} methods to update the matching
	 * {@code NumericProperty} with {@code property}.
	 * </p>
	 * 
	 * @param type     the type, which must be equal by definition to
	 *                 {@code property.getType()}.
	 * @param property the property, which contains new information.
	 */

	public abstract void set(NumericPropertyKeyword type, NumericProperty property);

	/**
	 * Runs recursive search for a property in this {@code Accessible} object with
	 * the same identifier as {@code property} and sets its value to the value of
	 * the {@code property} parameter.If {@code property} is a
	 * {@code NumericProperty}, uses its {@code NumericPropertyKeyword} for
	 * identification. For generic properties, calls {@code attemptUpdate}.
	 * 
	 * @param property the {@code Property}, which will update a similar property of
	 *                 this {@code Accessible}.
	 * @see Property.attemptUpdate(Property)
	 */

	public void update(Property property) {

		if (property instanceof NumericProperty)
			update((NumericProperty) property);
		else {
			var p = genericProperty(property);

			if (p == null) 
				accessibleChildren().stream().forEach(c -> c.update(property));
			else
				p.attemptUpdate(property.getValue());
		}

	}

	/**
	 * Set a NumericProperty contained in this Accessible or any of its accessible
	 * childern, using the NumericPropertyKeyword of the argument as identifier and
	 * its value.
	 * 
	 * @param p a NumericProperty
	 * @see Accessible.accessibleChildren()
	 */

	public void update(NumericProperty p) {
		this.set(p.getType(), p);
		for (var a : accessibleChildren())
			a.update(p);
	}

	/**
	 * <p>
	 * Selects only those {@code Accessible}s, the parent of which is {@code this}.
	 * Note that all {@code Accessible}s are required to explicitly adopt children
	 * by calling the {@code setParent()} method.
	 * </p>
	 * 
	 * @return a {@code List} of children that this {@code Accessible} has adopted.
	 * @see children
	 */

	public List<Accessible> accessibleChildren() {
		return children().stream().filter(group -> group instanceof Accessible).map(acGroup -> (Accessible) acGroup)
				.collect(toList());
	}

}