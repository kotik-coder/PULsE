package pulse.util;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.Property;

public abstract class PropertyHolder implements Accessible {

	private List<Property> parameters = listedParameters();
	private List<PropertyHolderListener> listeners;

	public List<Property> listedParameters() {
		
		List<Property> properties = new ArrayList<Property>();
		
		try {
			for(Accessible accessible : accessibles()) 
				if(accessible instanceof PropertyHolder) 
					properties.addAll( ((PropertyHolder) accessible).listedParameters());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return properties;
	}
	
	public PropertyHolder() {
		this.listeners = new ArrayList<PropertyHolderListener>();
	}
	
	private boolean isListedNumericParameter(NumericProperty p) {
		if(p == null)
			return false;
		
		return parameters.stream().filter(pr -> pr instanceof NumericProperty)
				.map(prop -> (NumericProperty)prop).anyMatch(param -> 
				param.getType() == p.getType() );		
			
	}
	
	private boolean isListedGenericParameter(Property p) {
		if(p == null)
			return false;
		
		return parameters.stream().anyMatch(param -> 
				param.getClass().equals(p.getClass()) );		
			
	}
	
	private boolean isListedParameter(Property p) {
		return p instanceof NumericProperty ? isListedNumericParameter((NumericProperty)p) : 
											  isListedGenericParameter(p);
	}
	
	/*
	 * Provides a two-column (title - value) representation of properties contained in this PropertyHolder object
	 */
	
	public List<Property> data() {	
		
		List<Property> properties = new LinkedList<Property>();
		
		try {
			properties.addAll(this.numericProperties());
			properties.addAll(this.genericProperties());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Property p = null;
		
		/*
		 * remove unknown properties from key array
		 */				
		
		for(Iterator<Property> pIterator = properties.iterator(); pIterator.hasNext(); ) {					
			
			p = pIterator.next();
					
			/*
			 * If property is not flagged as readable in this class, remove it
			 */
			
			if( ! isListedParameter(p) ) {
				pIterator.remove();
				continue;
			}
						
			/*
			 * Hide auto-adjustable properties in simple mode
			 */
			
			if( areDetailsHidden() )	
				if( p instanceof NumericProperty )
					if( ((NumericProperty)p).isAutoAdjustable() )
						pIterator.remove();										
		
		}
				
		return properties;		
		
	}
	
	public List<NumericProperty> numericData() {
		return data().stream().filter(p -> p instanceof NumericProperty).
				map(nP -> (NumericProperty)nP).collect(Collectors.toList());		
	}
	
	public List<EnumProperty> enumData() {
		return data().stream().filter(p -> p instanceof EnumProperty).
				map(eP -> (EnumProperty)eP).collect(Collectors.toList());
	}
	
	public void updateProperty(Object sourceComponent, Property updatedProperty) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {	
		Property existing = property(updatedProperty);
		
		if(existing == null)
			return;
		
		if(existing.equals(updatedProperty))
			return;
		
		Accessible.super.update(updatedProperty);
		PropertyEvent event = new PropertyEvent(sourceComponent, this, updatedProperty);
		listeners.forEach(l -> l.onPropertyChanged(event));
		
	}
	
	public void updateProperties(Object sourceComponent, PropertyHolder propertyHolder) {
		propertyHolder.data().stream().forEach( entry -> 
		{ 
				try {
					this.updateProperty( sourceComponent, entry );
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					System.err.println("Error when trying to update property " + entry.toString() + " in " + this);
					e.printStackTrace();
				} 
			} 
		);
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

	public void parameterListChanged() {
		this.parameters = listedParameters();
	}
	
	public boolean internalHolderPolicy() {
		return true;
	}

}
