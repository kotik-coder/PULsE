package pulse.ui.components;

import javax.swing.ImageIcon;

import pulse.ui.Launcher;

public enum ExecutionState {
	EXECUTE("EXECUTE", Launcher.loadIcon("/execute.png", 24)), 
	STOP("STOP", Launcher.loadIcon("/stop.png", 24));

	private String message;
	private ImageIcon icon;
	
	private ExecutionState(String message, ImageIcon icon) {
		this.icon = icon;
		this.message = message;
	}
	
	public ImageIcon getIcon() {
		return icon;
	}
	
	public String getMessage() {
		return message;
	}
	
}