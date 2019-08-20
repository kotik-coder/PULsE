package pulse.ui.charts;

public enum TimeAxisSpecs {
	
	SECONDS(1, "Time (s)"), MILLIS(1000, "Time (ms)");
	
	private int factor;
	private String title;
	
	private TimeAxisSpecs(int factor, String title) {
		this.factor = factor;
		this.title = title;
	}
	
	public int getFactor() {
		return factor;
	}
	
	public String getTitle() {
		return title;
	}
	
}
