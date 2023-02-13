package pulse.input.listeners;

import java.io.Serializable;
import pulse.AbstractData;

/**
 * A {@code DataEvent} is used to track changes happening with a
 * {@code ExperimentalData}.
 *
 */
public class DataEvent implements Serializable {

    private DataEventType type;
    private AbstractData data;

    /**
     * Constructs a {@code DataEvent} object, combining the {@code type} and
     * associated {@code data}
     *
     * @param type the type of this event
     * @param data the source of the event
     */
    public DataEvent(DataEventType type, AbstractData data) {
        this.type = type;
        this.data = data;
    }

    /**
     * Used to get the type of this event.
     *
     * @return the type of this event
     */
    public DataEventType getType() {
        return type;
    }

    /**
     * Used to get the {@code ExperimentalData} object that has undergone
     * certain changes specified by this event type.
     *
     * @return the associated data
     */
    public AbstractData getData() {
        return data;
    }

}
