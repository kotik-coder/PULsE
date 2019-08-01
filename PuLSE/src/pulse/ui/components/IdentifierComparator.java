package pulse.ui.components;

import java.util.Comparator;

import pulse.tasks.Identifier;

public class IdentifierComparator implements Comparator<Identifier> {
	
	protected IdentifierComparator() {
		
	}
	
	@Override
	public int compare(Identifier id1, Identifier id2) {
		Integer i1 = id1.getValue();
		Integer i2 = id2.getValue();
		
		return i1.compareTo(i2);
	}
	
}
