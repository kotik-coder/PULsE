package pulse;

/**
 * An interface used to listen to data events related to {@code HeatingCurve}.
 *
 */

public interface HeatingCurveListener {

	/**
	 * Signals that the {@code HeatingCurve} has been rescaled.
	 */

	public void onCurveRescaled();

}