package pulse.tasks.listeners;

import pulse.tasks.logs.StateEntry;

public interface StatusChangeListener {
	public void onStatusChange(StateEntry e);
}
