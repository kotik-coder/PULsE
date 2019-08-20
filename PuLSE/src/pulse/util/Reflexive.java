package pulse.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public interface Reflexive {	
	
	public static <T extends Reflexive> List<T> instancesOf(Class<? extends T> reflexiveType, String pckgname) {		
		List<Reflexive> ref = new LinkedList<Reflexive>();
		List<T> p	= new ArrayList<T>();
		
		ref.addAll(ReflexiveFinder.simpleInstances(pckgname));
		
		for(Reflexive r : ref) 
			if(reflexiveType.isAssignableFrom(r.getClass()))
				p.add((T) r);
		
		return p;
		
	}
	
	public static <T extends Reflexive> List<T> instancesOf(Class<? extends T> reflexiveType) {
		return Reflexive.instancesOf(reflexiveType, reflexiveType.getPackage().getName());
	}
	
}
