package pulse.problem.laser;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

import pulse.baseline.FlatBaseline;
import pulse.tasks.Calculation;
import pulse.util.FunctionSerializer;

/**
 * A numeric pulse is given by a set of discrete {@code NumericPulseData}
 * measured independently using a pulse diode.
 *
 * @see pulse.problem.laser.NumericPulseData
 *
 */
public class NumericPulse extends PulseTemporalShape {

    private static final long serialVersionUID = 6088261629992349844L;
    private NumericPulseData pulseData;
    private transient UnivariateFunction interpolation;

    private final static int MIN_POINTS = 20;

    public NumericPulse() {
        //intentionally blank
    }

    /**
     * Copy constructor
     *
     * @param pulse another numeric pulse, the data of which will be copied
     */
    public NumericPulse(NumericPulse pulse) {
        super(pulse);
        this.pulseData = new NumericPulseData(pulseData);
    }

    /**
     * Defines the pulse width as the last element of the time sequence
     * contained in {@code NumericPulseData}. Calls {@code super.init}, then
     * interpolates the input pulse using spline functions and normalises the
     * output.
     *
     * @param data
     * @see normalise()
     *
     */
    @Override
    public void init(ExperimentalData data, DiscretePulse pulse) {
        //generate baseline-subtracted numeric data from ExperimentalData
        baselineSubtractedFrom(data);

        //notify host pulse object of a new pulse width
        var problem = ((Calculation) ((SearchTask) data.getParent())
                .getResponse()).getProblem();
        setPulseWidthOf(problem);

        //convert to dimensionless time and interpolate
        double timeFactor = problem.getProperties().characteristicTime();
        doInterpolation(timeFactor);
    }

    /**
     * Copies the numeric pulse from metadata and subtracts a horizontal
     * baseline from the data points assigned to {@code pulseData}.
     *
     * @param data the experimental data containing the metadata with numeric
     * pulse data.
     */
    private void baselineSubtractedFrom(ExperimentalData data) {
        pulseData = new NumericPulseData(data.getMetadata().getPulseData());

        //subtracts a horizontal baseline from the pulse data
        var baseline = new FlatBaseline();
        baseline.fitTo(pulseData);

        for (int i = 0, size = pulseData.getTimeSequence().size(); i < size; i++) {
            pulseData.setSignalAt(i,
                    pulseData.signalAt(i) - baseline.valueAt(pulseData.timeAt(i)));
        }
    }

    private void setPulseWidthOf(Problem problem) {
        var timeSequence = pulseData.getTimeSequence();
        double pulseWidth = timeSequence.get(timeSequence.size() - 1);

        var pulseObject = problem.getPulse();
        pulseObject.setPulseWidth(derive(PULSE_WIDTH, pulseWidth));

    }

    private void doInterpolation(double timeFactor) {
        var interpolator = new AkimaSplineInterpolator();

        var timeList = pulseData.getTimeSequence().stream().mapToDouble(d -> d / timeFactor).toArray();
        var powerList = pulseData.getSignalData();

        this.setPulseWidth(timeList[timeList.length - 1]);

        interpolation = interpolator.interpolate(timeList,
                powerList.stream().mapToDouble(d -> d).toArray());
    }

    /**
     * If the argument is less than the pulse width, uses a spline to
     * interpolate the pulse data at {@code time}. Otherwise returns zero.
     */
    @Override
    public double evaluateAt(double time) {
        return time > getPulseWidth() ? 0.0 : interpolation.value(time);
    }

    @Override
    public PulseTemporalShape copy() {
        return new NumericPulse();
    }

    /**
     * Does not define any property.
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        // TODO Auto-generated method stub
    }

    public NumericPulseData getData() {
        return pulseData;
    }

    public void setData(NumericPulseData pulseData) {
        this.pulseData = pulseData;

    }

    public UnivariateFunction getInterpolation() {
        return interpolation;
    }

    @Override
    public int getRequiredDiscretisation() {
        return MIN_POINTS;
    }

    /*
    Serialization
     */
    private void writeObject(ObjectOutputStream oos)
            throws IOException {
        // default serialization 
        oos.defaultWriteObject();
        // write the object
        FunctionSerializer.writeSplineFunction((PolynomialSplineFunction) interpolation, oos);
    }

    private void readObject(ObjectInputStream ois)
            throws ClassNotFoundException, IOException {
        // default deserialization
        ois.defaultReadObject();
        this.interpolation = FunctionSerializer.readSplineFunction(ois);
    }

}
