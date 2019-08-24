/**
 * 
 */
package pulse.problem.statements;

import java.util.List;
import pulse.input.ExperimentalData;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.NumericProperty;
import pulse.properties.Property;

/**
 * @author Artem V. Lunev
 *
 */
public class LinearisedProblem2D extends LinearisedProblem implements TwoDimensional {
	
	private SecondDimensionData secondDimensionData = new SecondDimensionData();
	
	/**
	 * 
	 */
	public LinearisedProblem2D() {
		super();
	}
	
	public LinearisedProblem2D(Problem lp2) {
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
	public LinearisedProblem2D(NumericProperty curvePoints) {
		super(curvePoints);
		this.secondDimensionData = new SecondDimensionData();
	}
	
	@Override
	public void copyMainDetailsFrom(Problem p) {
		super.copyMainDetailsFrom(p);
		if(p instanceof TwoDimensional) 
			this.secondDimensionData = new SecondDimensionData(((TwoDimensional) p).getSecondDimensionData());
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = super.listedParameters();
		list.addAll((new SecondDimensionData()).listedParameters());
		return list;
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
	
	@Override
	public boolean isEnabled() {
		return false;
	}

}
