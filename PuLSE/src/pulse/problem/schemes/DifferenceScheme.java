package pulse.problem.schemes;

import java.util.ArrayList;
import java.util.List;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TwoDimensional;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.PropertyHolder;

public abstract class DifferenceScheme extends PropertyHolder {

	protected DiscretePulse	discretePulse;	
	protected Grid 			grid;
	
	protected double		timeLimit;
	protected int	 		timeInterval;
	
	private boolean normalized 						= true;	
	private static boolean hideDetailedAdjustment	= true;
	
	public DifferenceScheme(NumericProperty N, NumericProperty timeFactor) {
		grid = new Grid(N, timeFactor);	
		grid.setParent(this);
		setTimeLimit(NumericProperty.def(NumericPropertyKeyword.TIME_LIMIT));
	}
	
	public DifferenceScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	public abstract DifferenceScheme copy();
	
	public void copyFrom(DifferenceScheme df) {
		this.grid = df.getGrid().copy();
		discretePulse = null;
		timeLimit = df.timeLimit;
		normalized = df.normalized;
	}
	
	public void solve(Problem problem) {
		if(problem instanceof TwoDimensional) {
			if(grid instanceof Grid2D)
				discretePulse = new DiscretePulse2D(
						(Problem&TwoDimensional)problem, problem.getPulse(), (Grid2D)grid);
		}
		else
			discretePulse = new DiscretePulse(
					problem, problem.getPulse(), grid);	
		
		discretePulse.optimise(grid);
		
		timeInterval = (int) ( timeLimit / 
				 ( grid.tau*problem.timeFactor() * 
				   (int) problem.getHeatingCurve().getNumPoints().getValue() ) 
				 + 1 );
		
		if(timeInterval < 1)
			throw new IllegalStateException(Messages.getString("ExplicitScheme.2") + timeInterval);
	}
	
	public final boolean isNormalized() {
		return this.normalized;
	}
	
	public final void setNormalized(boolean normalized) {
		this.normalized = normalized;
	}

	public NumericProperty getTimeLimit() {
		return NumericProperty.derive(NumericPropertyKeyword.TIME_LIMIT, timeLimit);
	}

	public void setTimeLimit(NumericProperty timeLimit) {
		if(timeLimit.getType() != NumericPropertyKeyword.TIME_LIMIT)
			throw new IllegalArgumentException("Wrong type passed to method: " + timeLimit.getType());
		this.timeLimit = (double)timeLimit.getValue();
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>(9);
		list.add(NumericProperty.def(NumericPropertyKeyword.TIME_LIMIT));
		return list;
	}
	
	@Override	
	public String toString() {
		return shortName();
	}		
	
	public String shortName() {
		return this.getClass().getSimpleName();
	}
	
	@Override
	public boolean areDetailsHidden() {
		return DifferenceScheme.hideDetailedAdjustment;
	}
	
	public static void setDetailsHidden(boolean b) {
		DifferenceScheme.hideDetailedAdjustment = b;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case TIME_LIMIT : setTimeLimit(property); break;
		default : throw new IllegalArgumentException("Property not recognised: " + property);
		}
	}
	
	public Grid getGrid() {
		return grid;
	}
	
	public DiscretePulse getDiscretePulse() {
		return discretePulse;
	}
	
}