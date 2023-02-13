package pulse.tasks.listeners;

import java.io.Serializable;

public interface ResultFormatListener extends Serializable {

    public void resultFormatChanged(ResultFormatEvent rfe);

}
