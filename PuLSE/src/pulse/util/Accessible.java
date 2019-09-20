package pulse.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * <p>An {@Accessible} provides Reflection-based read- and write-access to the underlying (usually declared
 * as its own fields - but not necessarily) instances of {@code Property} and a recursive access to other {@code Accessible}, which
 * may have a family relationship with {@code this} {@code Accessible} via the {@code UpwardsNavigable}
 * implementation. It also defines its own list of {@code Saveable}s.</p>
 *
 */

public abstract class Accessible extends UpwardsNavigable {
	
	/**
	 * <p>Recursively analyses all {@code Accessible}s that this object owns
	 * (including only children) and chooses those that are {@code Saveable}.</p>
	 * @return a full list of {@code Saveable}s.
	 */
	
	public List<Saveable> saveableContents() {
		List<Saveable> contents = new ArrayList<Saveable>();

	    try {
			//contents.addAll(numericProperties());
			accessibles().stream().forEach(c -> 						
				{
					/*
					 * Filter only children, not parents! 
					 */
					
				 if(this.getParent() != c) {										
					 if(c instanceof SaveableDirectory) 
						contents.addAll(c.saveableContents() );				
					 if(c instanceof Saveable) {
						contents.add((Saveable)c);
						contents.addAll(c.saveableContents());
					 }
				 }
				 
				 });
		} catch (IllegalArgumentException e) {
			System.err.println("Unable to generate saveable contents for " + this.getClass());
			e.printStackTrace();
		}
	    
	    return contents;
	}
	
	/**
	 * <p>Searches for a {@code Property} in this {@code Accessible} that looks {@code similar} to the argument.
	 * Determines whether the {@code similar} is a {@code NumericProperty} or a generic property and calls the suitable 
	 * method in this class.</p>    
	 * @param similar a generic or a numeric {@code Property}
	 * @return the matching property of this {@code Accessible}
	 */
	
	public Property property(Property similar) {
		if(similar instanceof NumericProperty)
			return numericProperty(((NumericProperty)similar).getType());
		else
			return genericProperty(similar);
	}
	
	/**
	 * Tries to access the property getter methods in this {@code Accessible}, which should be declared as no-argument methods 
	 * with a specific return type. 
	 * @return This will return a unique {@code Set<NumericProperty>} containing all instances of {@code NumericProperty} 
	 * belonging to this {@code Accessible}. This set will not contain any duplicate elements by definition.
	 * @see pulse.properties.NumericProperty.equal(Object)
	 */
	
	public Set<NumericProperty> numericProperties() {
		Set<NumericProperty> fields = new TreeSet<NumericProperty>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {
       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	        if( NumericProperty.class.isAssignableFrom(m.getReturnType()) )
				try {
					fields.add((NumericProperty) m.invoke(this));
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					System.err.println("Error invoking method " + m);
					e.printStackTrace();
				}   
	        
	    }
	    
    	/*
    	 * Get access to the numeric properties of accessibles contained in this accessible
    	 */
    	
        for(Accessible a : children()) 
        	fields.addAll(a.numericProperties());
        	
	    return fields;
	    
	}
	
	/**
	 * Tries to access the property getter methods in this {@code Accessible}, which should be declared as no-argument methods 
	 * with a specific return type. 
	 * @return This will return a {@code List<Property>} containing all properties belonging to this {@code Accessible},
	 * which are not assignable from the {@code NumericProperty} class.
	 */
	
	public List<Property> genericProperties() {
		List<Property> fields = new ArrayList<Property>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {
       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	        if( Property.class.isAssignableFrom(m.getReturnType()) )
	        	if(! NumericProperty.class.isAssignableFrom(m.getReturnType()) )
					try {
						fields.add((Property) m.invoke(this));
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						System.err.println("Error invoking method " + m);
						e.printStackTrace();
					}   
	        
	    }
	    
    	/*
    	 * Get access to the properties of accessibles contained in this accessible
    	 */
    	
        for(Accessible a : children()) 
        	fields.addAll(a.genericProperties());
	    
	    return fields;
		
	}
	
	/**
	 * <p>Tries to access getter methods to retrieve all {@code Accessible} instances belonging to this object.
	 * Ignores any methods that return instances of the same class as {@code this} one.</p>
	 * @return a {@code List} containing {@code Accessibles} objects which could be accessed by the declared
	 * getter methods.
	 */
	
	public List<Accessible> accessibles() {
		List<Accessible> fields = new ArrayList<Accessible>();
		
		Method[] methods = this.getClass().getMethods();		
		
	    for(Method m : methods)
	    {       
	    	if(m.getParameterCount() > 0)
	    		continue;
	    	
	    	if(!Accessible.class.isAssignableFrom(m.getReturnType()))
	        	continue;
	    	
	        Accessible a = null;
	        
			try {
				a = (Accessible) m.invoke(this);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println("Failed to invoke " + m);
				e.printStackTrace();
			}
	        
	        if(a == null)
	        	continue;
	        
	        /* Ignore factor/instance methods returning objects of the same class */
	        if(a.getClass().equals(getClass()))
	        	continue;
	        
	        fields.add(a);

	    }
	    
	    return fields;
		
	}
	
	/**
	 * <p>Recursively searches for a {@code NumericProperty} from the unique set of numeric properties in this 
	 * {@code Accessible} by comparing its {@code NumericPropertyKeyword} to {@code type}. This will search
	 * for this property through the children of this object, through the children of their children, etc.</p>
	 * @param type the type of the {@code NumericProperty}.
	 * @return the respective {@code NumericProperty}, or {@code null} if nothing is found.
	 * @see numericProperties()
	 */

	public NumericProperty numericProperty(NumericPropertyKeyword type) {
		
		Optional<NumericProperty> match = numericProperties().stream().
				filter(p -> p.getType() == type).findFirst();
		
		if(match.isPresent())
			return match.get();		
		
		NumericProperty property = null;
		
		for(Accessible accessible : children()) {
			property = accessible.numericProperty(type);
			if(property != null) break; 
		}
		
		return property;
		
	}
	
	/**
	 * <p>Recursively searches for a {@code Property} from the non-unique list of generic properties in this 
	 * {@code Accessible} by comparing its class to {@code sameClass.getClass()}. This will search
	 * for this property through the children of this object, through the children of their children, etc.</p>
	 * @param sameClass the class identifying this {@code Property}.
	 * @return the respective {@code Property}, or {@code null} if nothing is found.
	 * @see genericProperties()
	 */
	
	public Property genericProperty(Property sameClass) {
		
		Optional<Property> match = genericProperties().stream().
				filter(p -> p.getClass().equals(sameClass.getClass())).findFirst();
		
		if(match.isPresent())
			return match.get();
		
		Property p = null;
		
		for(Accessible accessible : children()) {
			p = accessible.genericProperty(sameClass);
			if(p != null) break; 
		}
		
		return p;
		
	}
	
	/**
	 * Searches for a specific {@code Accessible} with a {@code simpleName}. 
	 * @see accessibles()
	 * @param simpleName the name of the {@code Accessible},
	 * @return the {@code Accessible object.
	 */
	
	public  Accessible accessible(String simpleName) {
		
		Optional<Accessible> match = accessibles().stream().
				filter(a -> a.getSimpleName().equals(simpleName)).findFirst();
		
		if(match.isPresent())
			return match.get();

		return null;
		
	}
	
	/**
	 * <p>An abstract method, which must be overriden to gain access over setting the values of all relevant
	 * (selected by the programmer) {@code NumericPropert}ies in subclasses of {@code Accessible}. Typically this involves 
	 * a {@code switch} statement that goes through the different options for the {@code type} and invokes 
	 * different {@code set(...)} methods to update the matching {@code NumericProperty} with {@code property}.</p> 
	 * @param type the type, which must be equal by definition to {@code property.getType()}.
	 * @param property the property, which contains new information.
	 */
	
	public abstract void set(NumericPropertyKeyword type, NumericProperty property);
	
	/**
	 * <p>Selects only those {@code Accessible}s, the parent of which is {@code this}. Note that all {@code Accessible}s
	 * are required to explicitly adopt children by calling the {@code setParent()} method.</p> 
	 * @return a {@code List} of children that this {@code Accessible} has adopted.
	 * @see accessibles()
	 */
	
	public List<Accessible> children() {
		return accessibles().stream().filter(a -> a.getParent() == this).collect(Collectors.toList());
	}
	
	/**
	 * Runs recursive search for a property in this {@code Accessible} object with the same identifier as {@code property} 
	 * and sets its value to the value of the {@code property} parameter.
	 * <p>If {@code property} is a {@code NumericProperty}, uses its {@code NumericPropertyKeyword} for identification. 
	 * If the property is part of an {@code Iterable}, such as {@code List},
	 * will search for a setter-type method that accepts both an {@code Iterable} argument, the generic type of which is the
	 * same as that for {@code property}, and a similar {@code property}. For generic properties, will simply search for a 
	 * setter method that accepts an instance of {@code Property} as its parameter. If nothing is found, will recursively
	 * search for the property that belongs to the children, the children of their children, etc.</p>.
	 * @param property the {@code Property}, which will update a similar property of this {@code Accessible}.  
	 */
	
	public void update(Property property) {

		List<Accessible> children = children();
		
		if(property instanceof NumericProperty) {
			NumericProperty p = (NumericProperty)property;
			this.set( p.getType(), p);			
			for(Accessible a : children)
				a.set(p.getType(), p);
			return;
		}	        	
		
		Method[] methods = this.getClass().getMethods();
	    
	    for(Method m : methods) {	    	
	    	
	    	if (m.getParameterCount() == 2) {
	    		
	    	/*
			* if there is a 'setter' method where first parameter is an Iterable containing Property objects
			* and second parameter is the property contained in that Iterable, use that method
			*/
	    
	    	if(Iterable.class.isAssignableFrom(m.getParameterTypes()[0])) {
	    		if(property.getClass().equals(m.getParameterTypes()[1])) {
	        				
	    			for(Method met : methods)
	    				if(met.getReturnType().
	    						equals(m.getParameterTypes()[0]) ) {
	        						Iterable<Property> returnType = null;
									try {
										returnType = (Iterable<Property>) met.invoke(this);
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										System.err.println("Cannot invoke method: " + met);
										e.printStackTrace();
									}
	        						Iterator<Property> iterator = returnType.iterator();
	        									
	        						if(!iterator.hasNext())
	        							continue;
	        									
	        						if(!iterator.next().getClass().equals(m.getParameterTypes()[1]))
	        							continue;
	        									
	        						try {
										m.invoke(this, met.invoke(this), property);
									} catch (IllegalAccessException | IllegalArgumentException
											| InvocationTargetException e) {
										System.err.println("Cannot invoked method " + m);
										e.printStackTrace();
									}
	        					}
	        				
	        		}
	        			
	        	}
	    	
	    	}
	    	
		    /*
		     * For generic Properties: use a simple setter method with enum value as argument
		     */
		    
	    	else if (m.getParameterCount() == 1)
	    		if(m.getParameterTypes()[0].equals(property.getClass()))
					try {
						m.invoke(this, property);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						System.err.println("Cannot invoked method " + m);
						e.printStackTrace();
					}
	    			        		
	        }	   	    
	        	
	    	/*
	    	* if above doesn't work: refer to children
	    	*/	    
	    
	    	for(Accessible a : children)
	    		a.update(property);	    	
	        		
	}

	
	/**
	 * This will generate a simple name for identifying this {@code Accessible}.
	 * @return the simple name of the declaring class.
	 */
	
	public String getSimpleName() {
		return getClass().getSimpleName();
	}
	
	/**
	 * The same as {@code getSimpleName} in this implementation.
	 * @return the simple name of the declaring class.
	 * @see getSimpleName()
	 */
	
	public  String getDescriptor() {
		return getSimpleName();
	}
	
}