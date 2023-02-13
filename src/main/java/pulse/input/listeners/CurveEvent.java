package pulse.input.listeners;

import java.io.Serializable;

/**
 * A {@code CurveEvent} is associated with an {@code HeatingCurve} object.
 *
 * @see pulse.HeatingCurve
 *
 */
public class CurveEvent implements Serializable {

    private CurveEventType type;

    /**
     * Constructs a {@code CurveEvent} object, combining the {@code type} and
     * associated {@code data}
     *
     * @param type the type of this event
     */
    public CurveEvent(CurveEventType type) {
        this.type = type;
    }

    /**
     * Used to get the type of this event.
     *
     * @return the type of this event
     */
    public CurveEventType getType() {
        return type;
    }

}
