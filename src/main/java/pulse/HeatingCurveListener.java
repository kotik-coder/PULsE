package pulse;

import pulse.input.listeners.CurveEvent;

/**
 * An interface used to listen to data events related to {@code HeatingCurve}.
 *
 */

public interface HeatingCurveListener {

	/**
	 * Signals that the {@code HeatingCurve} has been rescaled.
	 */

	public void onCurveEvent(CurveEvent event);

}