package pulse.ui.components.listeners;

import pulse.tasks.listeners.ResultFormatEvent;

public interface ResultListener {

    public void onFormatChanged(ResultFormatEvent fme);

}