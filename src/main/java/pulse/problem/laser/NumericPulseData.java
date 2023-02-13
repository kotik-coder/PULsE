package pulse.problem.laser;

import java.util.List;
import pulse.AbstractData;
import pulse.DiscreteInput;
import pulse.input.IndexRange;
import pulse.input.Range;

/**
 * An instance of the {@code AbstractData} class, which also declares an
 * {@code externalID}. Use to store numeric data of the pulse for each
 * measurement imported from an external source.
 *
 */
public class NumericPulseData extends AbstractData implements DiscreteInput {

    private static final long serialVersionUID = 8142129124831241206L;
    private final int externalID;

    /**
     * Stores {@code id} and calls super-constructor
     *
     * @param id an external ID defined in the imported file
     */
    public NumericPulseData(int id) {
        super();
        this.externalID = id;
    }

    /**
     * Copies everything, including the id number.
     *
     * @param data another object
     */
    public NumericPulseData(NumericPulseData data) {
        super(data);
        this.externalID = data.externalID;
    }

    /**
     * Adds a data point to the internal storage and increments counter.
     */
    @Override
    public void addPoint(double time, double power) {
        super.addPoint(time, power);
        super.incrementCount();
    }

    /**
     * Gets the external ID usually specified in the experimental files. Note
     * this is not a {@code NumericProperty}
     *
     * @return an integer, representing the external ID
     */
    public int getExternalID() {
        return externalID;
    }

    public double pulseWidth() {
        return super.timeLimit();
    }

    @Override
    public List<Double> getX() {
        return getTimeSequence();
    }

    @Override
    public List<Double> getY() {
        return getSignalData();
    }

    @Override
    public IndexRange getIndexRange() {
        return new IndexRange(this.getTimeSequence(), Range.UNLIMITED);
    }

}
