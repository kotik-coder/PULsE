package pulse.tasks.listeners;

import pulse.tasks.StateEntry;

public interface DataCollectionListener {
	public void onDataCollected(StateEntry e);
}