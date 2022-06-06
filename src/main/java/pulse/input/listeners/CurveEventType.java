package pulse.input.listeners;

/**
 * An event type associated with an {@code HeatingCurve} object.
 *
 */
public enum CurveEventType {

    /**
     * Indicates the curve signal values have been re-scaled. This means that
     * each signal value has been multiplied by a single number.
     */
    RESCALED,
    /**
     * Indicates a new time shift is introduced between the time sequences of a
     * {@code HeatingCurve} and its linked {@code ExperimentalData}. Triggered
     * either when manually changing the time origin of the solution (i.e.,
     * shifting it relative to the experimental data points) or by the search
     * procedure.
     */
    TIME_ORIGIN_CHANGED,
    
    /**
     * A calculation associated with this curve has finished and 
     * the required arrays have been filled.
     */
    
    CALCULATION_FINISHED;

}
