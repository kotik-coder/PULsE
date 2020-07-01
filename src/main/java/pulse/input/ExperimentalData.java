package pulse.input;

import static java.lang.Double.valueOf;
import static java.util.Collections.max;
import static pulse.input.listeners.DataEventType.TRUNCATED;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import pulse.HeatingCurve;
import pulse.input.listeners.DataEvent;
import pulse.properties.NumericProperty;
import pulse.ui.Messages;

/**
 * <p>
 * An {@code ExperimentalData} object is essentially a {@code HeatingCurve} with
 * adjustable fitting range and linked {@code Metadata}. It is used to store
 * experimental data points loaded using one of the available
 * {@code CurveReader}s. Any manipulation (e.g. truncation) of the data triggers
 * an event associated with this {@code ExperimentalData}.
 */

public class ExperimentalData extends HeatingCurve {

	private Metadata metadata;
	private IndexRange indexRange;
	private Range range;

	private final static double CUTOFF_FACTOR = 7.2;
	private final static int	REDUCTION_FACTOR = 32;
	private final static double FAIL_SAFE_FACTOR = 3.0;

	private static Comparator<Point2D> pointComparator = (p1, p2) -> valueOf(p1.getY())
			.compareTo(valueOf(p2.getY()));

	/**
	 * Constructor for {@code ExperimentalData}. This constructs a
	 * {@code HeatingCurve} using the no-argument constructor of the superclass, but
	 * it rejects the responsibility for the baseline, making its parent
	 * {@code null}.
	 * 
	 * @see HeatingCurve
	 */

	public ExperimentalData() {
		super();
		setPrefix("RawData");
		this.clear();
		count = 0;
		indexRange = new IndexRange();
		getBaseline().setParent(null);	//no baseline required
	}

	public void resetRanges() {
		indexRange.reset(time);
		range.reset(indexRange, time);
	}

	@Override
	public String toString() {
		var sb = new StringBuilder();
		sb.append("Experimental data ");
		if (metadata.getSampleName() != null)
			sb.append("for " + metadata.getSampleName() + " ");
		sb.append("(" + metadata.numericProperty(TEST_TEMPERATURE).formattedValue(false) + ")");
		return sb.toString();
	}

	/**
	 * Adds {@code time} and {@code temperature} to the respective {@code List}s.
	 * <p>
	 * Note that the {@code baselineAdjustedTemperature} will be the same as the
	 * corresponding {@code temperature}, i.e. no baseline subtraction is performed.
	 * Upon completion, the {@code count} variable will be incremented.
	 * </p>
	 * 
	 * @param time        the next time value
	 * @param signal the next signal value
	 */

	@Override
	public void addPoint(double time, double signal) {
		this.time.add(time);
		this.signal.add(signal);
		this.adjustedSignal.add(signal); //the same as previous since baseline is null
		count++;
	}

	/**
	 * Constructs a deliberately crude representation of this heating curve.
	 * <p>
	 * This is done using a binning algorithm, which will group the time-temperature
	 * data associated with this {@code ExperimentalData} in
	 * {@code count/reductionFactor - 1} bins, calculate the average value for time
	 * and temperature within each bin, and collect those values in a
	 * {@code List<Point2D>}. This is useful to cancel-out the effect of temperature
	 * outliers, e.g. when calculating the half-rise time.
	 * </p>
	 * 
	 * @param reductionFactor the factor, by which the number of points
	 *                        {@code count} will be reduced for this
	 *                        {@code ExperimentalData}.
	 * @return a {@code List<Point2D>}, representing the degraded
	 *         {@code ExperimentalData}.
	 * @see halfRiseTime
	 * @see maxTemperature
	 */

	public List<Point2D> crudeAverage(int reductionFactor) {

		List<Point2D> crudeAverage = new ArrayList<>(count / reductionFactor);

		int start = indexRange.getLowerBound();
		int end = indexRange.getUpperBound();

		int step = (end - start) / (count / reductionFactor);
		double av = 0;

		int i1, i2;

		for (int i = 0; i < (count / reductionFactor) - 1; i++) {
			i1 = start + step * i;
			i2 = i1 + step;
			
			av = 0;

			for (int j = i1; j < i2; j++) 
				av += signal.get(j);
			
			av /= step;

			crudeAverage.add(new Point2D.Double(time.get((i1 + i2) / 2), av));

		}

		return crudeAverage;

	}

	/**
	 * Instead of returning the absolute maximum (which can be an outlier!) of the
	 * temperature, this overriden method calculates the (absolute) maximum of the
	 * degraded curve by calling {@code crudeAverage} using the default reduction
	 * factor {@value REDUCTION_FACTOR}.
	 * 
	 * @see pulse.problem.statements.Problem.estimateSignalRange(ExperimentalData)
	 */

	@Override
	public double maxAdjustedSignal() {
		var degraded = crudeAverage(REDUCTION_FACTOR);
		return (max(degraded, pointComparator)).getY();
	}

	/**
	 * Calculates the approximate half-rise time used for crude estimation of
	 * thermal diffusivity.
	 * <p>
	 * This uses the {@code crudeAverage} method by applying the default reduction
	 * factor of {@value REDUCTION_FACTOR}. The calculation is based on finding the
	 * approximate value corresponding to the half-maximum of the temperature. The
	 * latter is calculated using the degraded heating curve. The index
	 * corresponding to the closest temperature value available in the degraded
	 * {@code HeatingCurve} is used to retrieve the half-rise time (which also has
	 * the same index).
	 * </p>
	 * 
	 * @return A double, representing the half-rise time (in seconds).
	 */

	public double halfRiseTime() {
		var degraded = crudeAverage(REDUCTION_FACTOR);
		double max = (max(degraded, pointComparator)).getY();
		baseline.fitTo(this);

		double halfMax = (max + baseline.valueAt(0)) / 2.0;

		int index = IndexRange.closest(halfMax, 
				degraded.stream().map(point -> point.getY()).collect(Collectors.toList()) 
				); 

		if (index < 1) {
			System.err.println(Messages.getString("ExperimentalData.HalfRiseError"));
			return max(time) / FAIL_SAFE_FACTOR;
		}

		return degraded.get(index).getX();

	}

	/**
	 * Retrieves the {@code Metadata} object for this {@code ExperimentalData}.
	 * 
	 * @return the linked {@code Metadata}
	 */

	public Metadata getMetadata() {
		return metadata;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ExperimentalData))
			return false;

		var other = (ExperimentalData) o;

		if (!this.metadata.equals(other.getMetadata()))
			return false;

		return super.equals(o);

	}

	/**
	 * Checks if the acquisition time used to collect this {@code ExperimentalData}
	 * is sensible.
	 * <p>
	 * The acquisition time is essentially the last element in the
	 * {@code time List}. By default, it is deemed sensible if that last element is
	 * less than {@value CUTOFF_FACTOR}*{@code halfRiseTime}.
	 * </p>
	 * 
	 * @return {@code true} if the acquisition time is below the truncation
	 *         threshold, {@code false} otherwise.
	 */

	public boolean isAcquisitionTimeSensible() {
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR * halfMaximum;
		return time.get(count - 1) < cutoff;
	}

	/**
	 * Performs truncation of this {@code ExperimentalData} by cutting off the time
	 * and temperature data <b>above</b> a certain threshold.
	 * <p>
	 * The threshold is calculated based on the {@code halfRiseTime} value and is
	 * set by default to {@value CUTOFF_FACTOR}*{@code halfRiseTime}. A
	 * {@code DataEvent} will be created and passed to the {@code dataListeners} (if
	 * any) with the {@code DataEventType.TRUNCATED} as argument.
	 * </p>
	 * 
	 * @see halfRiseTime
	 * @see DataEvent
	 */

	public void truncate() {
		double halfMaximum = halfRiseTime();
		double cutoff = CUTOFF_FACTOR * halfMaximum;

		this.range.setUpperBound(derive(UPPER_BOUND, cutoff));
		this.indexRange.set(time, range);

		fireDataChanged( new DataEvent(TRUNCATED, this) );
	}

	/**
	 * Sets a new {@code Metadata} object for this {@code ExperimentalData}.
	 * <p>
	 * The {@code pulseWidth} property recorded in {@code Metadata} will be used to
	 * set the time domain for the reverse problem solution. Whenever this property
	 * is changed in the {@code metadata}, a listener will ensure the
	 * {@code fittingStartIndex} and/or {@code fittingEndIndex} of this
	 * {@code ExperimentalData} are kept updated.
	 * </p>
	 * 
	 * @param metadata the new Metadata object
	 */

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
		metadata.setParent(this);
		doSetMetadata();
	}
	
	private void doSetMetadata() {

		if (range != null)
			range.process(metadata);

		metadata.addListener(event -> {

			if (event.getProperty() instanceof NumericProperty) {
				var p = (NumericProperty) event.getProperty();

				if (p.getType() == PULSE_WIDTH)
					range.process(metadata);

			}

		});
		
	}

	public List<Double> getTimeSequence() {
		return time;
	}

	public double getEffectiveStartTime() {
		return time.get(indexRange.getLowerBound());
	}

	public double getEffectiveEndTime() {
		return time.get(indexRange.getUpperBound());
	}

	public Range getRange() {
		return range;
	}

	public IndexRange getIndexRange() {
		return indexRange;
	}

	public void setRange(Range range) {
		this.range = range;
		range.setParent(this);
		doSetRange();
	}
	
	private void doSetRange() {
		indexRange.set(time, range);

		addHierarchyListener(l -> {
			if (l.getSource() == range)
				indexRange.set(time, range);
		});
		
		if(metadata != null)
			range.process(metadata);
	}

}