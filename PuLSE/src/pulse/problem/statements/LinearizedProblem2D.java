/**
 * 
 */
package pulse.problem.statements;

import java.util.Map;

import pulse.input.ExperimentalData;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;

/**
 * @author Artem V. Lunev
 *
 */
public class LinearizedProblem2D extends LinearizedProblem implements TwoDimensional {
	
	private SecondDimensionData secondDimensionData = new SecondDimensionData();
	
	/**
	 * 
	 */
	public LinearizedProblem2D() {
		super();
	}
	
	public LinearizedProblem2D(Problem lp2) {
		super(lp2);
		
		if(! (lp2 instanceof TwoDimensional) )
			this.secondDimensionData = new SecondDimensionData();
		else 
			this.secondDimensionData = new SecondDimensionData( ((TwoDimensional)lp2).getSecondDimensionData());
	}

	/**
	 * @param curvePoints
	 * @param dimensionless
	 */
	public LinearizedProblem2D(NumericProperty curvePoints, BooleanProperty dimensionless) {
		super(curvePoints, dimensionless);
		this.secondDimensionData = new SecondDimensionData();
	}
	
	@Override
	public void copyMainDetailsFrom(Problem p) {
		super.copyMainDetailsFrom(p);
		if(p instanceof TwoDimensional) 
			this.secondDimensionData = new SecondDimensionData(((TwoDimensional) p).getSecondDimensionData());
	}
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = super.propertyNames();
		map.putAll( (new SecondDimensionData()).propertyNames() );
		return map;
	}
	
	@Override
	public DifferenceScheme[] availableSolutions() {
		return new DifferenceScheme[]{new ADIScheme()};
	}
	
	public SecondDimensionData getSecondDimensionData() {
		return secondDimensionData;
	}
	
	@Override
	public void reset(ExperimentalData c) {
		super.reset(c);
		secondDimensionData.resetHeatLosses();
	}
	
	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem2D.Descriptor"); //$NON-NLS-1$
	}

}
