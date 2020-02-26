package pulse.problem.schemes;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public abstract class ADIScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 1.0);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);

	public ADIScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}	
	
	public ADIScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		grid = new Grid2D(N, timeFactor);	
		grid.setParent(this);
	}
	
	public ADIScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public String toString() {
		return Messages.getString("ADIScheme.4");
	}
	
}