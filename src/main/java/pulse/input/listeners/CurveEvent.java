package pulse.input.listeners;

import pulse.AbstractData;

public class CurveEvent {

	private CurveEventType type;
	private AbstractData data;

	/**
	 * Constructs a {@code CurveEvent} object, combining the {@code type} and
	 * associated {@code data}
	 * 
	 * @param type the type of this event
	 * @param data the source of the event
	 */

	public CurveEvent(CurveEventType type, AbstractData data) {
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

	public AbstractData getData() {
		return data;
	}
	
}