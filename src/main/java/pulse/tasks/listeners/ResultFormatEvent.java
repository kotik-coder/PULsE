package pulse.tasks.listeners;

import pulse.tasks.ResultFormat;

public class ResultFormatEvent {

	private ResultFormat rf;

	public ResultFormatEvent(ResultFormat rf) {
		this.rf = rf;
	}

	public ResultFormat getResultFormat() {
		return rf;
	}

}
