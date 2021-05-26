package pulse.search.direction;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.ITERATION;

import pulse.properties.NumericProperty;

public class IterativeState {

	private int iteration;
	private int failedAttempts;

	public void reset() {
		iteration = 0;
	}
	
	public NumericProperty getIteration() {
		return derive(ITERATION, iteration);
	}

	public void incrementStep() {
		iteration++;
	}
	
	public int getFailedAttempts() {
		return failedAttempts;
	}
	
	public void resetFailedAttempts() {
		failedAttempts = 0;
	}
	
	public void incrementFailedAttempts() {
		failedAttempts++;
	}

}