package pulse.input.listeners;

public enum CurveEventType {

	RESCALED, 
	
	/**
	 * <p>
	 * Signal a time shift between the time sequences of a {@code HeatingCurve} and
	 * its linked {@code ExperimentalData}. Triggered either when manually changing
	 * the time origin of the solution (i.e., shifting it relative to the
	 * experimental data points) or by the search procedure.
	 */
	
	TIME_ORIGIN_CHANGED;
	
}