package pulse.tasks;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class Identifier extends NumericProperty {
		private int id;
		private static int lastId = -1;
		
		private final static String TASK_TAG = "Task";
		
		public final static Identifier DEFAULT_IDENTIFIER = new Identifier();
		
		private Identifier(int value) {
			super(NumericPropertyKeyword.IDENTIFIER, 
					"Identifier", "ID", value, NumericProperty.COUNT);
			this.id = value;
			Identifier.lastId = value;		
		}
		
		public Identifier() {
			this(Identifier.lastId + 1);
		}
		
		public static Identifier identifier(int id) {
			for(SearchTask t : TaskManager.getTaskList()) {
				if(t.getIdentifier().id > id)
					continue;
				if(t.getIdentifier().id < id)
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
		public int compareTo(NumericProperty p) {
			if(! (p instanceof Identifier) )
				return super.compareTo(p);
			
			Identifier i = (Identifier) p;
			return Integer.valueOf(id).compareTo(Integer.valueOf(i.id));
		}
		
		@Override
		public boolean equals(Object o) {
			if(! (o instanceof Identifier) )
				return false;
			
			Identifier i = (Identifier) o;
			
			if(id > i.id)
				return false;
			
			if(id < i.id)
				return false;
			
			return true;
			
		}
		
		@Override
		public Object getValue() {
			return id;
		}
		
		@Override
		public String toString() {
			return TASK_TAG + " " + id;
		}

}
