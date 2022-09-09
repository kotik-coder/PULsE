package pulse.input;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.UPPER_BOUND;

import java.util.ArrayList;
import java.util.List;

import pulse.AbstractData;
import pulse.DiscreteInput;
import pulse.input.listeners.DataEvent;
import pulse.input.listeners.DataEventType;
import pulse.input.listeners.DataListener;
import pulse.math.filters.HalfTimeCalculator;
import pulse.util.PropertyHolderListener;

/**
 * <p>
 * An {@code ExperimentalData} object is essentially a {@code AbstractData} with
 * adjustable range and linked {@code Metadata}. It is used to store
 * experimental data points loaded with one of the available
 * {@code CurveReader}s. Any manipulation (e.g. truncation) of the data triggers
 * an event associated with this {@code ExperimentalData}.
 */
public class ExperimentalData extends AbstractData implements DiscreteInput {

    private HalfTimeCalculator calculator;
    private Metadata metadata;
    private IndexRange indexRange;
    private Range range;
    private List<DataListener> dataListeners;

    /**
     * This is the cutoff factor which is used as a criterion for data
     * truncation. Described in Lunev, A., &amp; Heymer, R. (2020). Review of
     * Scientific Instruments, 91(6), 064902.
     */
    public final static double CUTOFF_FACTOR = 7.2;

    /**
     * Constructs an {@code ExperimentalData} object using the superclass
     * constructor and creating a new list of data listeners. The number of
     * points is set to zero by default, and a new {@code IndexRange} is
     * initialized.
     *
     */
    public ExperimentalData() {
        super();
        dataListeners = new ArrayList<>();
        setPrefix("RawData");
        setNumPoints(derive(NUMPOINTS, 0));
        indexRange = new IndexRange(0,0);
        this.addDataListener((DataEvent e) -> {
            if (e.getType() == DataEventType.DATA_LOADED) {
                preprocess();
            }
        });
    }

    public final void addDataListener(DataListener listener) {
        dataListeners.add(listener);
    }

    public final void clearDataListener() {
        dataListeners.clear();
    }

    public final void fireDataChanged(DataEvent dataEvent) {
        dataListeners.stream().forEach(l -> l.onDataChanged(dataEvent));
    }

    /**
     * Calls reset for both the {@code IndexRange} and {@code Range} objects
     * using the current time sequence.
     *
     * @see pulse.input.Range.reset()
     * @see pulse.input.IndexRange.reset()
     */
    public final void resetRanges() {
        indexRange.reset(getTimeSequence());
        range.reset(indexRange, getTimeSequence());
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Experimental data ");
        if (metadata.getSampleName() != null) {
            sb.append("for " + metadata.getSampleName() + " ");
        }
        sb.append("(").append(metadata.numericProperty(TEST_TEMPERATURE).formattedOutput()).append(")");
        return sb.toString();
    }

    /**
     * Adds {@code time} and {@code temperature} to the respective
     * {@code List}s. Increments the counter of points. Note that no baseline
     * correction is performed.
     *
     * @param time the next time value
     * @param signal the next signal value
     */
    @Override
    public void addPoint(double time, double signal) {
        super.addPoint(time, signal);
        incrementCount();
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
        if (!super.equals(o)) {
            return false;
        }

        if (!(o instanceof ExperimentalData)) {
            return false;
        }

        var other = (ExperimentalData) o;
        return this.metadata.equals(other.getMetadata());
    }

    /**
     * Checks if the acquisition time used to collect this
     * {@code ExperimentalData} is sensible.
     * <p>
     * The acquisition time is essentially the last element in the
     * {@code time List}. By default, it is deemed sensible if that last element
     * is less than {@value CUTOFF_FACTOR}*{@code halfRiseTime}.
     * </p>
     *
     * @return {@code true} if the acquisition time is below the truncation
     * threshold, {@code false} otherwise.
     */
    public boolean isAcquisitionTimeSensible() {
        final double cutoff = CUTOFF_FACTOR * calculator.getHalfTime();
        final int count = (int) getNumPoints().getValue();
        double d = getTimeSequence().get(count - 1);
        return getTimeSequence().get(count - 1) < cutoff;
    }

    /**
     * Truncates the {@code range} and {@code indexRange} of this
     * {@code ExperimentalData} above a certain threshold, NOT removing any data
     * elements.
     * <p>
     * The threshold is calculated based on the {@code halfRiseTime} value and
     * is set by default to {@value CUTOFF_FACTOR}*{@code halfRiseTime}. A
     * {@code DataEvent} will be created and passed to the {@code dataListeners}
     * (if any) with the {@code DataEventType.TRUNCATED} as argument.
     * </p>
     *
     * @see halfRiseTime
     * @see DataEvent
     * @see fireDataChanged
     */
    public void truncate() {
        final double cutoff = CUTOFF_FACTOR * calculator.getHalfTime();
        this.range.setUpperBound(derive(UPPER_BOUND, cutoff));
    }

    /**
     * Sets a new {@code Metadata} object for this {@code ExperimentalData}.
     * <p>
     * The {@code pulseWidth} property recorded in {@code Metadata} will be used
     * to set the time range for the reverse problem solution. Whenever this
     * property is changed in the {@code metadata}, a listener will ensure an
     * updated range is used.
     * </p>
     *
     * @param metadata the new Metadata object
     * @see PropertyHolderListener
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
        metadata.setParent(this);
        doSetMetadata();
    }

    private void doSetMetadata() {

        if (range != null) {
            range.updateMinimum(metadata.numericProperty(PULSE_WIDTH));
        }

    }

    /**
     * Gets the time sequence element corresponding to the lower bound of the
     * index range
     *
     * @return the time (in seconds) associated with
     * {@code indexRange.getLowerBound()}
     */
    public double getEffectiveStartTime() {
        return getTimeSequence().get(indexRange.getLowerBound());
    }

    /**
     * Gets the time sequence element corresponding to the upper bound of the
     * index range
     *
     * @return the time (in seconds) associated with
     * {@code indexRange.getUpperBound()}
     */
    public double getEffectiveEndTime() {
        return getTimeSequence().get(indexRange.getUpperBound());
    }

    /**
     * Gets the dimensional time {@code Range} of this data.
     *
     * @return the range
     */
    public Range getRange() {
        return range;
    }

    /**
     * Gets the index range of this data.
     *
     * @return the index range
     */
    @Override
    public IndexRange getIndexRange() {
        return indexRange;
    }

    /**
     * Sets the range, assigning {@code this} to its parent, and forcing changes
     * to the {@code indexRange}.
     *
     * @param range the range
     */
    public void setRange(Range range) {
        this.range = range;
        range.setParent(this);
        doSetRange();
    }

    private void doSetRange() {
        indexRange.set(time, range);

        addHierarchyListener(l -> {
            if (l.getSource() == range) {
                indexRange.set(time, range);
                this.fireDataChanged(new DataEvent(DataEventType.RANGE_CHANGED, this));
            }
        });

        if (metadata != null) {
            range.updateMinimum(metadata.numericProperty(PULSE_WIDTH));
        }
    }

    /**
     * Retrieves the time limit.
     *
     * @see pulse.problem.schemes.DifferenceScheme
     * @return a double, equal to the last element of the {@code time List}.
     */
    @Override
    public double timeLimit() {
        return timeAt(indexRange.getUpperBound());
    }

    public HalfTimeCalculator getHalfTimeCalculator() {
        return calculator;
    }

    public void preprocess() {
        if (calculator == null) {
            calculator = new HalfTimeCalculator(this);
        }

        calculator.calculate();
    }

    @Override
    public List<Double> getX() {
        return this.getTimeSequence();
    }

    @Override
    public List<Double> getY() {
        return this.getSignalData();
    }

}