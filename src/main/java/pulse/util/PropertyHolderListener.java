package pulse.util;

/**
 * A listener used by {@code PropertyHolder}s to track changes with the
 * associated {@code Propert}ies.
 */

public interface PropertyHolderListener {

	/**
	 * This event is triggered by any {@code PropertyHolder}, the properties of
	 * which have been changed.
	 * 
	 * @param event the event associated with actions taken on a {@code Property}.
	 */

	public void onPropertyChanged(PropertyEvent event);

}
