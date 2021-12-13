package pulse.util;

import pulse.properties.Property;

/**
 * An event which is created to signal about the changes with a
 * {@code PropertyHolder}.
 *
 */
public class PropertyEvent {

    private Object source;
    private PropertyHolder propertyHolder;
    private Property property;

    /**
     * Constructs an event that has happened because of {@code source},
     * resulting in an action taken on the {@code property}.
     *
     * @param source the originator of the event
     * @param property the object of the event
     */
    public PropertyEvent(Object source, PropertyHolder propertyHolder, Property property) {
        this.source = source;
        this.property = property;
        this.propertyHolder = propertyHolder;
    }

    /**
     * Gets the 'source', which is an Object that is the originator of this
     * event.
     *
     * @return
     */
    public Object getSource() {
        return source;
    }

    /**
     * Gets the property, which is related to this event.
     *
     * @return the related property.
     */
    public Property getProperty() {
        return property;
    }

    public PropertyHolder getPropertyHolder() {
        return propertyHolder;
    }

    @Override
    public String toString() {
        return "Source: " + source.getClass().getSimpleName() + " ; Holder: "
                + propertyHolder.getClass().getSimpleName() + " ; Property: " + property;
    }

}
