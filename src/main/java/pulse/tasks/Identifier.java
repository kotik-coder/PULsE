package pulse.tasks;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;

/**
 * <p>
 * An {@code Identifier} is used to identify {@code SearchTask}s. It stores the
 * internal task ID as its integer value and the last recorded ID.
 * </p>
 *
 */

public class Identifier extends NumericProperty {
	private static int lastId = -1;

	private Identifier(int value, boolean addToList) {
		super(def(IDENTIFIER));
		setValue(value);
		if (addToList)
			setLastId(value);
	}

	private static void setLastId(int value) {
		Identifier.lastId = value;
	}

	/**
	 * Creates an {@code Identifier} by incrementing the previously recorded ID.
	 */

	public Identifier() {
		this(Identifier.lastId + 1, true);
	}

	/**
	 * Seeks an {@code Identifier} from the list of available tasks in
	 * {@code TaskManager} that matches this {@code string}.
	 * 
	 * @param string the string describing the identifier.
	 * @return a matching {@code Identifier}.
	 */

	public static Identifier parse(String string) {
		var i = TaskManager.getTaskList().stream().map(t -> t.getIdentifier())
				.filter(id -> id.toString().equals(string)).findFirst();
		return i.isPresent() ? i.get() : null;
	}

	public static Identifier externalIdentifier(int id) {
		return id > -1 ? new Identifier(id, false) : null;
	}

	@Override
	public String toString() {
		return Messages.getString("Identifier.Tag") + " " + getValue();
	}

}