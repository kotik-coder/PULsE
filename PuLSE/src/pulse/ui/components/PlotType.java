package pulse.ui.components;

public enum PlotType {

	EXPERIMENTAL_DATA(0, Messages.getString("Charting.0")), 
	SOLUTION(1, Messages.getString("Charting.1")), 
	CLASSIC_SOLUTION(2, Messages.getString("Charting.2"));
	
	private int index;
	private String style;
		
	private PlotType(int i, String style) {
		this.index = i;
		this.style = style;
	}

	public int getChartIndex() {
		return index;
	}

	public String getStyle() {
		return style;
	}
	
}
