package pulse.ui.components;

import java.awt.Color;

public enum ExecutionState {
	EXECUTE("EXECUTE", Color.GREEN), STOP("STOP", Color.RED);
	
	private Color color;
	private String message;
	
	private ExecutionState(String message, Color clr) {
		this.color = clr;
		this.message = message;
	}
	
	public Color getColor() {
		return color;
	}
	
	public String getMessage() {
		return message;
	}
	
}