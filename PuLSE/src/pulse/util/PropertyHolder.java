package pulse.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pulse.properties.NumericProperty;
import pulse.properties.Property;

public abstract class PropertyHolder implements Accessible {

	protected Map<String,String> propertyNames = propertyNames();
	private List<PropertyHolderListener> listeners;

	public abstract Map<String,String> propertyNames();
	
	private PropertyHolder parent;
	
	public PropertyHolder() {
		this.listeners = new ArrayList<PropertyHolderListener>();
	}
	
	public PropertyHolder(PropertyHolder parent) {
		this();
	}
	
	public String paramName(String key) {
		
		if(!propertyNames.containsKey(key)) 
			return Messages.getString("PropertyHolder.0"); //$NON-NLS-1$
		
		return propertyNames.get(key);
			
	}
	
	/*
	 * Provides a two-column (title - value) representation of properties contained in this PropertyHolder object
	 */
	
	public Object[][] data() {	
		
		Map<String,Object> map = null;
		
		try {
			map = this.fieldMap();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			System.err.println(Messages.getString("PropertyHolder.FieldMapError")); //$NON-NLS-1$
			e.printStackTrace();
		}	
		
		Set<String> keySet	= map.keySet();
		Object[] keyArray	= keySet.toArray();
		
		/*
		 * remove unknown properties from key array
		 */
		
		for(int i = 0; i < keyArray.length; i++) {
			if( this.paramName(keyArray[i].toString() ).equals( Messages.getString("PropertyHolder.0") ))
				map.remove(keyArray[i]); 
			
			if(! areDetailsHidden() )
				continue;
			
			Property p = null;
			
			try {
				p = this.propertyByName( (String) keyArray[i]);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if( p instanceof NumericProperty )
				if( ((NumericProperty)p).isAutoAdjustable() )
					map.remove(keyArray[i]);
					
		}
		
		Object[][] data	= new Object[map.values().size()][2];
		keyArray 		= keySet.toArray(); //updated key array		

		for(int i = 0; i < keyArray.length; i++) {
			data[i][0] = this.paramName(keyArray[i].toString());
			data[i][1] = map.get(keyArray[i]);
		}				
		
		return data;
		
	}
	
	public void updateProperty(Property property) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {	
		Accessible.updateProperty(this, property);
		PropertyEvent event = new PropertyEvent(this, property);
		
		for(PropertyHolderListener l : listeners)
			l.onPropertyChanged(event);
		
	}
	
	public void removeListeners() {
		this.listeners.clear();
	}
	
	public void addListener(PropertyHolderListener l) {
		this.listeners.add(l);
	}
	
	public List<PropertyHolderListener> getListeners() {
		return listeners;
	}
	
	public boolean areDetailsHidden() {
		return false;
	}

	public List<PropertyHolder> getChildren() {
		List<PropertyHolder> list = new ArrayList<PropertyHolder>();
		Class<? extends PropertyHolder> thisClass = this.getClass();
		Method[] methods = thisClass.getMethods();
		Object o ;
		for(Method method : methods) {
			if(method.getName().startsWith("get")) { //$NON-NLS-1$
				if(method.getName().substring(3).equalsIgnoreCase("Children")) //$NON-NLS-1$
					continue;
				if(method.getParameterCount() < 1) {
					try {
						o = method.invoke(this);
						if(o instanceof PropertyHolder)
							list.add((PropertyHolder) o);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						System.err.println("Unable to call " + method + " from " + thisClass); //$NON-NLS-1$ //$NON-NLS-2$
						e.printStackTrace();
					}
				}
			}
		}
	
		return list;
		
	}
	
	public PropertyHolder getParent() {
		return parent;
	}
	
	public void updatePropertyNames() {
		this.propertyNames = propertyNames();
	}

}
