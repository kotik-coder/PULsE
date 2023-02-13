package pulse;

import java.io.Serializable;
import pulse.input.listeners.CurveEvent;

/**
 * An interface used to listen to data events related to {@code HeatingCurve}.
 *
 */
public interface HeatingCurveListener extends Serializable {

    /**
     * Signals that a {@code CurveEvent} has occurred.
     *
     * @param event
     */
    public void onCurveEvent(CurveEvent event);

}
