package pulse.input.listeners;

public enum CurveEventType {

	/**
	 * Indicates the curve signal values have been re-scaled.
	 */
	
	RESCALED, 
	
	/**
	 * Indicates a new time shift is introduced between the time sequences of a {@code HeatingCurve} and
	 * its linked {@code ExperimentalData}. Triggered either when manually changing
	 * the time origin of the solution (i.e., shifting it relative to the
	 * experimental data points) or by the search procedure.
	 */
	
	TIME_ORIGIN_CHANGED;
	
}