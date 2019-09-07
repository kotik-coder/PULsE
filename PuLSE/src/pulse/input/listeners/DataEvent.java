package pulse.input.listeners;

import pulse.input.ExperimentalData;

public class DataEvent {

	private DataEventType type;
	private ExperimentalData data;
	
	public DataEvent(DataEventType type, ExperimentalData data) {
		this.type = type;
		this.data = data;
	}

	public DataEventType getType() {
		return type;
	}

	public ExperimentalData getData() {
		return data;
	}
	
}
