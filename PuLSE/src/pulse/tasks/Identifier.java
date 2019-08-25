package pulse.tasks;

import pulse.properties.NumericProperty;
import static pulse.properties.NumericPropertyKeyword.*;

public class Identifier extends NumericProperty {
		private static int lastId = -1;
		
		private final static String TASK_TAG = "Task";
		
		public final static Identifier DEFAULT_IDENTIFIER = new Identifier();
		
		private Identifier(int value) {
			super(NumericProperty.def(IDENTIFIER));
			setValue(value);
			Identifier.lastId = value;		
		}
		
		public Identifier() {
			this(Identifier.lastId + 1);
		}
		
		public static Identifier identifier(int id) {
			int val;
			for(SearchTask t : TaskManager.getTaskList()) {
				val = ((Number)t.getIdentifier().getValue()).intValue();
				if( val > id )
					continue;
				if( val < id )
					continue;
				return t.getIdentifier();
			}
			
			return null;	
		}
		
		public static Identifier identify(String string) {
			String[] tokens = string.split(" ");
			
			if(! tokens[0].equals(TASK_TAG))
				return null;
			if(tokens.length != 2)
				return null;
			
			int id;
			
			try {					
				id = Integer.parseInt(tokens[1]);
			} catch(NumberFormatException e) {
				return null;
			}
			
			return identifier(id);
			
		}

		@Override
		public String toString() {
			return TASK_TAG + " " + getValue();
		}

}
