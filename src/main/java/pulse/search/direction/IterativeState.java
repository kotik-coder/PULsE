package pulse.search.direction;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.ITERATION;

import pulse.properties.NumericProperty;

public class IterativeState {

	private int iteration;

	public void reset() {
		iteration = 0;
	}
	
	public NumericProperty getIteration() {
		return derive(ITERATION, iteration);
	}

	public void incrementStep() {
		iteration++;
	}

}