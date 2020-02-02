package pulse.tasks;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

import java.util.Optional;

/**
 * <p>An {@code Identifier} is used to identify {@code SearchTask}s. 
 * It stores the internal task ID as its integer value and the last recorded ID.</p>
 *
 */

public class Identifier extends NumericProperty {
		private static int lastId = -1;
		
		private Identifier(int value, boolean addToList) {
			super(NumericProperty.theDefault(IDENTIFIER));
			setValue(value);
			if(addToList) 
				Identifier.lastId = value;		
		}
		
		/**
		 * Creates an {@code Identifier} by incrementing the previously recorded ID.
		 */
		
		public Identifier() {
			this(Identifier.lastId + 1, true);
		}
		
		/**
		 * Seeks an {@code Identifier} from the list of available tasks in {@code TaskManager}
		 * that matches this {@code string}.  
		 * @param string the string describing the identifier.
		 * @return a matching {@code Identifier}.
		 */

		public static Identifier parse(String string) {
			Optional<Identifier> i = TaskManager.getTaskList().stream().
			map(t -> t.getIdentifier()).filter(id -> id.toString().equals(string))
			.findFirst();
			if(i.isPresent())
				return i.get();
			else 
				return null;
		}
		
		public static Identifier externalIdentifier(int id) {
			if(id > -1)
				return new Identifier(id, false);
			else 
				return null;
		}

		@Override
		public String toString() {
			return Messages.getString("Identifier.Tag") + " " + getValue();
		}

}