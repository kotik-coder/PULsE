package pulse;

import pulse.input.listeners.CurveEvent;

/**
 * An interface used to listen to data events related to {@code HeatingCurve}.
 *
 */
public interface HeatingCurveListener {

    /**
     * Signals that a {@code CurveEvent} has occurred.
     */
    public void onCurveEvent(CurveEvent event);

}
