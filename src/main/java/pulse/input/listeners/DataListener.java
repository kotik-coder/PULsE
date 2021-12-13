package pulse.input.listeners;

/**
 * A listener interface, which is used to listen to {@code DataEvent}s occurring
 * with an {@code ExperimentalData} object.
 *
 */
public interface DataListener {

    /**
     * Triggered when a certain {@code DataEvent} specified by its
     * {@code DataEventType} is initiated from within the
     * {@code ExperimentalData} object.
     *
     * @param e the event object.
     */
    public void onDataChanged(DataEvent e);

}
