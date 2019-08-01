package pulse.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import pulse.properties.Property;

public interface Accessible {
	
	public default Map<String,Object> fieldMap() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Map<String,Object> fieldMap = new HashMap<String,Object>();
		
		Method[] methods = this.getClass().getMethods();
		
	    for(Method m : methods)
	    {
	        if(m.getName().startsWith("get"))
	        {	       
	        	Object value = m.invoke(this);
	        	if(! (value instanceof Accessible)) {
	        		if( value instanceof Property[]) {
	        			Property[] array = (Property[]) value;
	        			for(Property p : array) {
	        				fieldMap.put(p.getSimpleName(), p);	
	        			}
	        		}
	        		else
	        			fieldMap.put(m.getName().substring(3), value);		
	        	} 
	        	else if(value != this) {
	        		Map<String,Object> innerMap = ((Accessible) value).fieldMap();
	        		fieldMap.put(value.getClass().getSimpleName(), value);
	        		fieldMap.putAll(innerMap);
	        	}
	        }

	    }
	    
	    for(Method m : methods) 
	    {
	        if(m.getName().startsWith("is"))
	        {
	        	if(m.getParameterCount() > 0)
	        		break;
	        	Object value = m.invoke(this);
	            fieldMap.put(m.getName().substring(2), value);
	        }
	    }
	    
	    return fieldMap;
		
	}
	
	public default Object objectByName(String simpleName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Map<String,Object> map = this.fieldMap();
		
		Object o = null;
		
		for(String key : map.keySet()) {
			if(key.equalsIgnoreCase(simpleName)) 
				o = map.get(key);
		}
		
		return o;
		
	}
	
	public default Property propertyByName(String simpleName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Object o = this.objectByName(simpleName);
		if(o instanceof Property)
			return (Property)o;
		else
			return null;
	}
	
	public default Object value(String propertyName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Object o = this.objectByName(propertyName);
		
		if(o instanceof Property)
			return ((Property) o).getValue();
		else
			return o;
		
	}
	
	/*
	 * Finds a property in this Accessible object with the same name as the property parameter and sets its value to the value of property parameter
	 */
	
	public static void updateProperty(Accessible accessible, Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Method[] methods = accessible.getClass().getMethods();
		
	    for(Method m : methods)	   
	        if(m.getName().startsWith("set")) {        	        
	        	if (m.getParameterCount() > 1) {
	        		if(m.getParameterTypes()[0].equals(String.class)) {
	        			if(property.getClass() == m.getParameterTypes()[1] ) {
	        				m.invoke(accessible, property.getSimpleName(), property);
	        				return;
	        			}
	        		} 
	        	}
	        	else if( (m.getName().substring(3)).
	        			equals(property.getSimpleName()) ) {
	        				m.invoke(accessible, property);
	        				return;
	        	}
	        }
	    
	    for(Method m : methods)
	    {
	        if(m.getName().startsWith("get"))
	        {        	
	        	Object value = m.invoke(accessible);
	        	if(value == accessible)
	        		return;
	        	if(value instanceof Accessible) {
	        		Accessible.updateProperty((Accessible)value, property);
	        	}
	        }
	        		
	    }
	          	
	}
	
}
