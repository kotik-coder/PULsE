package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

public class SumOfSquares extends ResidualStatistic {

	/**
	 * Calculates the sum of squared deviations using {@code curve} as reference.
	 * <p>
	 * This calculates
	 * <math><munderover><mo>&#x2211;</mo><mrow><mi>i</mi><mo>=</mo><msub><mi>i</mi><mn>1</mn></msub></mrow><msub><mi>i</mi><mn>2</mn></msub></munderover><mo>(</mo><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo><mo>-</mo><mi>T</mi><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><msup><mo>)</mo><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msup><msup><mo>)</mo><mn>2</mn></msup></math>,
	 * where
	 * <math><msubsup><mi>T</mi><mi>i</mi><mrow><mi>r</mi><mi>e</mi><mi>f</mi></mrow></msubsup></math>
	 * is the temperature value corresponding to the {@code time} at index {@code i}
	 * for the reference {@code curve}. Note that the time
	 * <math><msub><mi>t</mi><mi>i</mi></msub></math> corresponds to the
	 * <b>reference's</b> time list, which generally does not match to that of this
	 * heating curve. The
	 * <math><mover><mi>T</mi><mo>&#x23DE;</mo></mover><mo>(</mo><msub><mi>t</mi><mi>i</mi></msub><mo>)</mo></math>
	 * is the interpolated value for this heating curve at the reference time. The
	 * temperature value is interpolated using two nearest elements of the
	 * <b>baseline-subtracted</b> temperature list. The value is interpolated using
	 * the experimental time <math><i>t</i><sub>i</sub></math> and the nearest
	 * solution points to that time. The accuracy of this interpolation depends on
	 * the number of points. The boundaries of the summation are set by the
	 * {@code curve.getFittingStartIndex()} and {@code curve.getFittingEndIndex()}
	 * methods.
	 * 
	 * @param t The task containing the reference and calculated curves
	 */

	@Override
	public void evaluate(SearchTask t) {
		calculateResiduals(t);
		final double statistic = getResiduals().stream().map(r -> r[1] * r[1]).reduce(Double::sum).get() / getResiduals().size();
		setStatistic(derive(OPTIMISER_STATISTIC, statistic));
	}

	@Override
	public String getDescriptor() {
		return "Ordinary least squares";
	}

}