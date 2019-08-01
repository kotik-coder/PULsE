package pulse.problem.statements;

import java.util.Map;

import pulse.input.ExperimentalData;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Vector;

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
	public Map<String,String> propertyNames() {
		Map<String,String> map = super.propertyNames();
		map.putAll( (new SecondDimensionData()).propertyNames() );
		return map;
	}
	
	@Override
	public DifferenceScheme[] availableSolutions() {
		return new DifferenceScheme[]{new ADIScheme()};
	}
	
	@Override
	public Vector objectiveFunction(BooleanProperty[] flags) {
		Vector v = super.objectiveFunction(flags);

		for(int i = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue; 
			
			if( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()).equals(ObjectiveFunctionIndex.MAX_TEMP)  ) {
				v.set(i, qAbs);
				break;
			}
		
		}
		
		return v;
	}
	
	@Override 
	public void assign(Vector params, BooleanProperty[] flags) {
		super.assign(params, flags);

		for(int i = 0, realIndex = 0; i < flags.length; i++) {
			if(! (boolean) flags[i].getValue() )
				continue;
			
			realIndex = convert(i, flags);
			
			switch( ObjectiveFunctionIndex.valueOf(flags[i].getSimpleName()) ) {
			case HEAT_LOSSES : 
				secondDimensionData.setSideLosses( new NumericProperty(params.get(realIndex), NumericProperty.DEFAULT_BIOT) ); break;
			case MAX_TEMP :
				qAbs = params.get(realIndex); break;
			default :
				continue;
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
