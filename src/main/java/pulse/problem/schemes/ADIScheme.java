package pulse.problem.schemes;

import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.TAU_FACTOR;
import static pulse.ui.Messages.getString;

import pulse.properties.NumericProperty;

public abstract class ADIScheme extends DifferenceScheme {

	public ADIScheme() {
		this(derive(GRID_DENSITY, 30), 
				derive(TAU_FACTOR, 1.0));
	}

	public ADIScheme(NumericProperty N, NumericProperty timeFactor) {
		super();
		initGrid(N, timeFactor);
	}

	public ADIScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(timeLimit);
		initGrid(N, timeFactor);
	}

	@Override
	public void initGrid(NumericProperty N, NumericProperty timeFactor) {
		setGrid(new Grid2D(N, timeFactor));
		getGrid().setTimeFactor(timeFactor);
	}

	@Override
	public String toString() {
		return getString("ADIScheme.4");
	}

}