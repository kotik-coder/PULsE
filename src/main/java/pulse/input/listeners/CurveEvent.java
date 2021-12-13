package pulse.input.listeners;

import pulse.HeatingCurve;

/**
 * A {@code CurveEvent} is associated with an {@code HeatingCurve} object.
 *
 * @see pulse.HeatingCurve
 *
 */
public class CurveEvent {

    private CurveEventType type;
    private HeatingCurve data;

    /**
     * Constructs a {@code CurveEvent} object, combining the {@code type} and
     * associated {@code data}
     *
     * @param type the type of this event
     * @param data the source of the event
     */
    public CurveEvent(CurveEventType type, HeatingCurve data) {
        this.type = type;
        this.data = data;
    }

    /**
     * Used to get the type of this event.
     *
     * @return the type of this event
     */
    public CurveEventType getType() {
        return type;
    }

    /**
     * Used to get the {@code HeatingCurve} object that has undergone certain
     * changes specified by this event type.
     *
     * @return the associated data
     */
    public HeatingCurve getData() {
        return data;
    }

}
