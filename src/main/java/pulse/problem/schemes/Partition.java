package pulse.problem.schemes;

import pulse.properties.NumericPropertyKeyword;

public class Partition {
	
	private int density;
	private double multiplier;
	private double shift;
		
	public Partition(int value, double multiplier, double shift) {
		this.setDensity(value);
		this.setShift(shift);;
		this.setGridMultiplier(multiplier);
	}

	public double evaluate() {
		return multiplier/( density*(1.0 + shift) );
	}
		
	public int getDensity() {
		return density;
	}

	public void setDensity(int density) {
		this.density = density;
	}

	public double getGridMultiplier() {
		return multiplier;
	}

	public void setGridMultiplier(double multiplier) {
		this.multiplier = multiplier;
	}

	public double getShift() {
		return shift;
	}

	public void setShift(double shift) {
		this.shift = shift;
	}

	public enum Location {

		FRONT_Y, REAR_Y, SIDE_Y, SIDE_X, CORE_X, CORE_Y;

		public NumericPropertyKeyword densityKeyword() {
			switch(this) {
				case FRONT_Y : 
				case REAR_Y : 
				case SIDE_Y : 
				case SIDE_X : 
					return NumericPropertyKeyword.SHELL_GRID_DENSITY;
				case CORE_X :
				case CORE_Y :
							 return NumericPropertyKeyword.GRID_DENSITY;
				default : 
					throw new IllegalArgumentException("Type not recognized: " + this);
			}
		}
		
	}
	
}