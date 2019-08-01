package pulse.tasks;

import pulse.properties.NumericProperty;

public class Identifier {
		private int id;
		private static int lastId = -1;
		
		private final static String TASK_TAG = "Task";
		
		private Identifier(int value) {
			this.id = value;
			Identifier.lastId = value;		
		}
		
		public Identifier() {
			this(Identifier.lastId + 1);
		}
		
		public int getValue() {
			return id;
		}
		
		@Override
		public String toString() {
			return TASK_TAG + " " + id;
		}
		
		public static Identifier identifier(int id) {
			for(SearchTask t : TaskManager.getTaskList()) {
				if(t.getIdentifier().getValue() > id)
					continue;
				if(t.getIdentifier().getValue() < id)
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
		
		public NumericProperty asNumericProperty() {
			return new NumericProperty(null, id);
		}
		
		@Override
		public boolean equals(Object o) {
			if(! (o instanceof Identifier))
				return false;

			int id1 = this.getValue();
			int id2 = ((Identifier)o).getValue();
			
			if(id1 > id2)
				return false;
			
			if(id1 < id2)
				return false;
			
			return true;
			
		}

}
