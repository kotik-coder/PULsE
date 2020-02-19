package pulse.ui.components;

import pulse.tasks.listeners.ResultFormatEvent;

public interface ResultListener {

	public void onFormatChanged(ResultFormatEvent fme);
	
}
