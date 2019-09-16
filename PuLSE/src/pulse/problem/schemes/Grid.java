package pulse.problem.schemes;

import static java.lang.Math.pow;
import java.util.ArrayList;
import java.util.List;

import pulse.problem.statements.Pulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public class Grid extends PropertyHolder {
	
	protected double	tau, tauFactor;
	protected int 		N;
	protected double	hx;
	
	public Grid(NumericProperty gridDensity, NumericProperty timeFactor) {
		setGridDensity(gridDensity);
		setTimeFactor(timeFactor);
	}
	
	public Grid copy() {
		return new Grid(getGridDensity(), getTimeFactor());
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>(2);
		list.add(NumericProperty.def(NumericPropertyKeyword.GRID_DENSITY));
		list.add(NumericProperty.def(NumericPropertyKeyword.TAU_FACTOR));
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case TAU_FACTOR		: setTimeFactor(property); break;
		case GRID_DENSITY	: setGridDensity(property); break;
		}
	}
	
	public double getXStep() {
		return hx;
	}

	public void setXStep(double hx) {
		this.hx = hx;
	}
	
	public double getTimeStep() {
		return tau;
	}
	
	public NumericProperty getTimeFactor() {
		return NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 
				tauFactor);
	}
	
	public NumericProperty getGridDensity() {
		return NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, this.N);
	}
	
	public void setGridDensity(NumericProperty gridDensity) {
		if(gridDensity.getType() != NumericPropertyKeyword.GRID_DENSITY)
			throw new IllegalArgumentException("Type of property passed to constructor " + gridDensity.getType());
		this.N = (int)gridDensity.getValue();
		hx = 1./N;
	}
	
	public void setTimeFactor(NumericProperty timeFactor) {
		if(timeFactor.getType() != NumericPropertyKeyword.TAU_FACTOR)
			throw new IllegalArgumentException
				("Wrong NumericProperty type passed to method: " + timeFactor.getType());
		
		this.tauFactor	= (double)timeFactor.getValue();
		tau				= tauFactor*pow(hx, 2);
	}
	
	/**
	 * The dimensionless time on a grid, which is the {@code time/timeFactor} rounded up to a factor of the time step {@code tau}.
	 * @param problem the problem containing a reference to {@code Pulse}
	 * @return a double representing the time on the finite grid
	 * @see Pulse
	 */
	
	public double gridTime(double time, double dimensionFactor) {		
		return Math.rint((time/dimensionFactor)/tau)*tau;	
	}
	
	public double gridAxialDistance(double distance, double lengthFactor) {
		return Math.rint((distance/lengthFactor)/hx)*hx;	
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append("Grid: <math><i>h<sub>x</sub></i>=" + String.format("%3.2e", hx) + "; ");
		sb.append("<i>&tau;</i>="+String.format("%3.2e", tau));
		return sb.toString();
	}
	
}