package pulse.util;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Group extends UpwardsNavigable {

	/**
	 * <p>
	 * Tries to access getter methods to retrieve all {@code Accessible} instances
	 * belonging to this object. Ignores any methods that return instances of the
	 * same class as {@code this} one.
	 * </p>
	 * 
	 * @return a {@code List} containing {@code Accessibles} objects which could be
	 *         accessed by the declared getter methods.
	 */

	public List<Group> subgroups() {
		var fields = new ArrayList<Group>();

		var methods = this.getClass().getMethods();
		for (var m : methods) {
			if (m.getParameterCount() > 0)
				continue;

			if (!Group.class.isAssignableFrom(m.getReturnType()))
				continue;

			Group a = null;

			try {
				a = (Group) m.invoke(this);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("Failed to invoke " + m);
				e.printStackTrace();
			}

			if (a == null)
				continue;

			/* Ignore factor/instance methods returning same accessibles */
			if (a.getDescriptor().equals(getDescriptor()))
				continue;

			fields.add(a);
			fields.addAll(a.subgroups());

		}

		return fields;

	}
	
	/**
	 * <p>
	 * Recursively analyses all {@code Group} objects that are identified as subgroups to {@code root} 
	 * (explicitly checks that subgroups exclude parents of {@code root}) and chooses those for which an
	 * {@code Exporter} exists.
	 * </p>
	 * 
	 * @param root the root group.
	 * @return a set of unique {@code Group}s objects.
	 * @see pulse.util.Group.subgroups()
	 */

	public static Set<Group> contents(Group root) {
		var contents = new HashSet<Group>();

		root.subgroups().stream().filter(ph -> root.getParent() != ph).forEach(phh -> contents.add(phh));

		for (var it = contents.iterator(); it.hasNext();)
			contents(it.next()).stream().forEach(a -> contents.add(a));

		return contents;
	}

	/**
	 * Searches for a specific {@code Accessible} with a {@code simpleName}.
	 * 
	 * @see accessibles()
	 * @param simpleName the name of the {@code Accessible},
	 * @return the {@code Accessible} object.
	 */

	public Group access(String simpleName) {

		var match = subgroups().stream().filter(a -> a.getSimpleName().equals(simpleName)).findFirst();

		if (match.isPresent())
			return match.get();

		return null;

	}

	/**
	 * <p>
	 * Selects only those {@code Accessible}s, the parent of which is {@code this}.
	 * Note that all {@code Accessible}s are required to explicitly adopt children
	 * by calling the {@code setParent()} method.
	 * </p>
	 * 
	 * @return a {@code List} of children that this {@code Accessible} has adopted.
	 * @see accessibles()
	 */

	public List<Group> children() {
		return subgroups().stream().filter(a -> a.getParent() == this).collect(toList());
	}

	/**
	 * The same as {@code getSimpleName} in this implementation.
	 * 
	 * @return the simple name of the declaring class.
	 * @see getSimpleName()
	 */

	public String getDescriptor() {
		return getClass().getSimpleName();
	}

	/**
	 * This will generate a simple name for identifying this {@code Accessible}.
	 * 
	 * @return the simple name of the declaring class.
	 */

	public String getSimpleName() {
		return getClass().getSimpleName();
	}

}