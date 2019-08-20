package pulse.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public interface Accessible {
	
	public default Property property(Property similar) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if(similar instanceof NumericProperty)
			return numericProperty(((NumericProperty)similar).getType());
		else
			return genericProperty(similar);
	}
	
	/*
	 * The same as for genericProperties() -- however, this returns a Set<NumericProperty>, meaning that 
	 * it will contain only unique objects
	 */
	
	public default Set<NumericProperty> numericProperties() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Set<NumericProperty> fields = new TreeSet<NumericProperty>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {
       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	        if( NumericProperty.class.isAssignableFrom(m.getReturnType()) )
	        	fields.add((NumericProperty) m.invoke(this));   
	        
	    }
	    
    	/*
    	 * Get access to the numeric properties of accessibles contained in this accessible
    	 */
    	
        for(Accessible a : accessibles()) 
        	fields.addAll(a.numericProperties());
	    
	    return fields;
	}
	
	/*
	 * This will return a List<Property> containing all properties except instances of NumericProperty
	 */
	
	public default List<Property> genericProperties() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Property> fields = new ArrayList<Property>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {
       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	        if( Property.class.isAssignableFrom(m.getReturnType()) )
	        	if(! NumericProperty.class.isAssignableFrom(m.getReturnType()) )
	        		fields.add((Property) m.invoke(this));   
	        
	    }
	    
    	/*
    	 * Get access to the properties of accessibles contained in this accessible
    	 */
    	
        for(Accessible a : accessibles()) 
        	fields.addAll(a.genericProperties());
	    
	    return fields;
		
	}
	
	/*
	 * return objects via getter methods that are instances of the Accessible superclass
	 */
	
	public default List<Accessible> accessibles() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		List<Accessible> fields = new ArrayList<Accessible>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	    	if(!Accessible.class.isAssignableFrom(m.getReturnType()))
	        	continue;
	    	
	        Accessible a = (Accessible) m.invoke(this);
	        
	        if(a == null)
	        	continue;
	        
	        /* Ignore factor/instance methods returning objects of the same class */
	        if(a.getClass().equals(getClass()))
	        	continue;
	        
	        fields.add(a);

	    }
	    
	    return fields;
		
	}
	
	public default NumericProperty numericProperty(NumericPropertyKeyword type) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Set<NumericProperty> properties = this.numericProperties();
		
		for(NumericProperty property : properties) {
			NumericPropertyKeyword key = property.getType();
			if(key == type)
				return property;
		}
		
		NumericProperty property = null;
		
		for(Accessible accessible : accessibles()) {
			property = accessible.numericProperty(type);
			if(property != null) break; 
		}
		
		return property;
		
	}
	
	public default Property genericProperty(Property sameClass) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Optional<Property> match = genericProperties().stream().
				filter(p -> p.getClass().equals(sameClass.getClass())).findFirst();
		
		if(match.isPresent())
			return match.get();
		
		Property p = null;
		
		for(Accessible accessible : accessibles()) {
			p = accessible.genericProperty(sameClass);
			if(p != null) break; 
		}
		
		return p;
		
	}
	
	public default Accessible accessible(String simpleName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		List<Accessible> accessibles = this.accessibles();
		
		for(Accessible accessible : accessibles) {
			String key = accessible.getSimpleName();
			if(key.equals(simpleName))
				return accessible;
		}
		
		return null;
		
	}
	
	public abstract void set(NumericPropertyKeyword type, NumericProperty property);
	
	/*
	 * Finds a property in this Accessible object with the same name as the property parameter and sets its value to the value of property parameter
	 */
	
	public default void update(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {					
		
		if(property instanceof NumericProperty) {
			NumericProperty p = (NumericProperty)property;
			this.set( p.getType(), p);
			for(Accessible a : accessibles())
				a.set(p.getType(), p);
			return;
		}
	        		
		/*
		* if there is a 'setter' method where first parameter is an Iterable containing Property objects
		* and second parameter is the property contained in that Iterable, use that method
		*/
		
		Method[] methods = this.getClass().getMethods();
	    
	    for(Method m : methods) {
	    	
	    	if (m.getParameterCount() == 2) {
	    
	    	if(Iterable.class.isAssignableFrom(m.getParameterTypes()[0])) {
	    		if(property.getClass().equals(m.getParameterTypes()[1])) {
	        				
	    			for(Method met : methods)
	    				if(met.getReturnType().
	    						equals(m.getParameterTypes()[0]) ) {
	        						Iterable returnType = (Iterable) met.invoke(this);
	        						Iterator iterator = returnType.iterator();
	        									
	        						if(!iterator.hasNext())
	        							continue;
	        									
	        						if(!iterator.next().getClass().equals(m.getParameterTypes()[1]))
	        							continue;
	        									
	        						m.invoke(this, met.invoke(this), property);
	        					}
	        				
	        		}
	        			
	        	}
	    	
	    	}
	    	
		    /*
		     * For generic Properties: use a simple setter method with enum value as argument
		     */
		    
	    	else if (m.getParameterCount() == 1)
	    		if(m.getParameterTypes()[0].equals(property.getClass())) 	    			
	    			m.invoke(this, property);
	    			        		
	        }	   	    
	        	
	    	/*
	    	* if above doesn't work: check if there are getter methods in this class returning Accessible objects,
	    	* use those methods to get access to those objects and call update(Property) recursively
	    	*/
	    
	    	for(Accessible a : accessibles())
	    		a.update(property);	    		    	
	        		
	}

	
	public default String getSimpleName() {
		return getClass().getSimpleName();
	}
	
	public default String getDescriptor() {
		return getSimpleName();
	}
	
}
