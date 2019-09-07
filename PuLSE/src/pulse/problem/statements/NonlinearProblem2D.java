package pulse.problem.statements;

import java.util.List;
import java.util.Map;

import pulse.input.ExperimentalData;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.search.math.Vector;
import pulse.ui.Messages;

public class NonlinearProblem2D extends NonlinearProblem implements TwoDimensional {
	
	private SecondDimensionData secondDimensionData = new SecondDimensionData();
	
	/**
	 * 
	 */
	public NonlinearProblem2D() {
		super();
	}
	
	public NonlinearProblem2D(Problem p) {
		super(p);
		if(! (p instanceof TwoDimensional) )
			this.secondDimensionData = new SecondDimensionData();
		else 
			this.secondDimensionData = new SecondDimensionData( ((TwoDimensional)p).getSecondDimensionData());

	}

	public NonlinearProblem2D(NumericProperty a, NumericProperty cV, NumericProperty rho, NumericProperty qAbs, NumericProperty T) {
		super(a, cV, rho, qAbs, T);
		this.secondDimensionData = new SecondDimensionData();
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
	
	@Override
	public IndexedVector objectiveFunction(List<Flag> flags) {	
		IndexedVector objectiveFunction = super.objectiveFunction(flags);
		int size = objectiveFunction.dimension(); 		
		
		for(int i = 0; i < size; i++) {

			if( objectiveFunction.getIndex(i) == NumericPropertyKeyword.MAXTEMP ) {
				objectiveFunction.set(i, qAbs);	
				break;		
			}
		}
		
		return objectiveFunction;
		
	}	
	
	@Override
	public void assign(IndexedVector params) {
		super.assign(params);		
		int size = params.dimension(); 		
		
		for(int i = 0; i < size; i++) {

			if( params.getIndex(i) == NumericPropertyKeyword.MAXTEMP ) {
				qAbs = params.get(i);	
				break;		
			}
		}
		
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
		return Messages.getString("NonlinearProblem2D.Descriptor"); //$NON-NLS-1$
	}
	
}
