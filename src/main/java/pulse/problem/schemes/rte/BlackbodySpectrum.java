package pulse.problem.schemes.rte;

import static pulse.math.MathUtils.fastPowLoop;

import org.apache.commons.math3.analysis.UnivariateFunction;

import pulse.problem.statements.NonlinearProblem;
import pulse.problem.statements.Pulse2D;

/**
 * Contains methods for calculating the integral spectral characteristics of a
 * black body with a specific spatial temperature profile. The latter is managed
 * using a {@code UnivariateFunction} previously generated with a
 * {@code SplineInterpolator}.
 *
 */

public class BlackbodySpectrum {

	private double reductionFactor;
	private UnivariateFunction interpolation;

	/**
	 * Creates a {@code BlackbodySpectrum}. Calculates the reduction factor
	 * <math>&delta;<i>T</i><sub>m</sub>/<i>T</i><sub>0</sub></math>, which
	 * is needed for calculations of the maximum heating. Note the interpolation
	 * needs to be set 
	 * @param p a problem statement
	 */
	
	public BlackbodySpectrum(NonlinearProblem p) {
		final double maxHeating = p.getProperties().maximumHeating((Pulse2D)p.getPulse());
		reductionFactor = maxHeating / ((double) p.getProperties().getTestTemperature().getValue());
	}

	/**
	 * Calculates the spectral radiance, which is equal to the spectral power
	 * divided by &pi;, at the given coordinate.
	 * 
	 * @param x the geometric coordinate at which calculation should be performed
	 * @return the spectral radiance at {@code x}
	 */

	public double radianceAt(double x) {
		return radiance(interpolation.value(x));
	}

	/**
	 * Calculates the emissive power at the given coordinate. This is equal to
	 * <math>0.25 <i>T</i><sub>0</sub>/&delta;<i>T</i><sub>m</sub> [1
	 * +&delta;<i>T</i><sub>m</sub> /<i>T</i><sub>0</sub> &theta; (<i>x</i>)
	 * ]<sup>4</sup></math>, where &theta; is the reduced temperature.
	 * 
	 * @param x the geometric coordinate inside the sample
	 * @return the local emissive power value
	 */

	public double powerAt(double x) {
		return emissivePower(interpolation.value(x));
	}

	/**
	 * Sets a new function for the spatial temperature profile. The function is
	 * generally constructed using a {@code SplineInterpolator}
	 * 
	 * @param interpolation
	 */

	public void setInterpolation(UnivariateFunction interpolation) {
		this.interpolation = interpolation;
	}

	public UnivariateFunction getInterpolation() {
		return interpolation;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ": Rel. heating = " + reductionFactor + "]";
	}

	private double emissivePower(double reducedTemperature) {
		return 0.25 / reductionFactor * fastPowLoop(1.0 + reducedTemperature * reductionFactor, 4);
	}

	private double radiance(double reducedTemperature) {
		return emissivePower(reducedTemperature) / Math.PI;
	}

}