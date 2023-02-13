package pulse.tasks.listeners;

import java.io.Serializable;
import pulse.tasks.processing.ResultFormat;

public class ResultFormatEvent implements Serializable {

    private ResultFormat rf;

    public ResultFormatEvent(ResultFormat rf) {
        this.rf = rf;
    }

    public ResultFormat getResultFormat() {
        return rf;
    }

}
