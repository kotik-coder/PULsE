package pulse.problem.laser;

import pulse.AbstractData;

public class NumericPulseData extends AbstractData {

	private int externalID;
	
	public NumericPulseData(int id) {
		super();
		this.externalID = id;
	}
	
	public NumericPulseData(NumericPulseData data) { 
		super(data);
		this.externalID = data.externalID;
	}

	@Override
	public void addPoint(double time, double power) {
		super.addPoint(time, power);
		super.incrementCount();
	}
	
	/**
	 * Gets the external ID usually specified in the experimental files. Note this
	 * is not a {@code NumericProperty}
	 * 
	 * @return an integer, representing the external ID
	 */

	public int getExternalID() {
		return externalID;
	}
	
	public void scale(double factor) {
		
		var power = this.getSignalData();
		
		for(int i = 0, size = power.size(); i < size; i++) 
			power.set(i, power.get(i) * factor );
		
	}

}