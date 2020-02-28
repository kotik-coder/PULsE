package pulse.tasks.listeners;

import pulse.tasks.StateEntry;

public interface StatusChangeListener {
	public void onStatusChange(StateEntry e);
}
