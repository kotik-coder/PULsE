package pulse.problem.laser;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;

import pulse.input.ExperimentalData;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;

import pulse.baseline.FlatBaseline;

/**
 * A numeric pulse is given by a set of discrete {@code NumericPulseData}
 * measured independently using a pulse diode.
 *
 * @see pulse.problem.laser.NumericPulseData
 *
 */
public class NumericPulse extends PulseTemporalShape {

    private NumericPulseData pulseData;
    private UnivariateFunction interpolation;
    private double adjustedPulseWidth;

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
     * @see normalise()
     *
     */
    @Override
    public void init(ExperimentalData data, DiscretePulse pulse) {
        pulseData = data.getMetadata().getPulseData();
        
        //subtracts a horizontal baseline from the pulse data
        var baseline = new FlatBaseline();
        baseline.fitNegative(pulseData);
        
        for(int i = 0, size = pulseData.getTimeSequence().size(); i < size; i++)
            pulseData.setSignalAt(i, 
                    pulseData.signalAt(i) - baseline.valueAt(pulseData.timeAt(i)));
      
        var problem = ((SearchTask) data.getParent()).getCurrentCalculation().getProblem();
        setPulseWidth(problem);

        double timeFactor = problem.getProperties().timeFactor();

        super.init(data, pulse);

        doInterpolation(timeFactor);

        normalise(problem);
    }

    /**
     * Checks that the area of the pulse curve is unity (within a small error
     * margin). If this is {@code false}, re-scales the numeric data using
     * {@code 1/area} as the scaling factor.
     *
     * @param problem defines the {@code timeFactor} needed for re-building the
     * interpolation
     * @see pulse.problem.laser.NumericPulseData.scale()
     */
    public void normalise(Problem problem) {

        final double EPS = 1E-2;
        double timeFactor = problem.getProperties().timeFactor();

        for (double area = area(); Math.abs(area - 1.0) > EPS; area = area()) {
            pulseData.scale(1.0 / area);
            doInterpolation(timeFactor);
        }

    }

    private void setPulseWidth(Problem problem) {
        var timeSequence = pulseData.getTimeSequence();
        double pulseWidth = timeSequence.get(timeSequence.size() - 1);

        var pulseObject = problem.getPulse();
        pulseObject.setPulseWidth(derive(PULSE_WIDTH, pulseWidth));

    }

    private void doInterpolation(double timeFactor) {
        var interpolator = new AkimaSplineInterpolator();

        var timeList = pulseData.getTimeSequence().stream().mapToDouble(d -> d / timeFactor).toArray();
        adjustedPulseWidth = timeList[timeList.length - 1];
        var powerList = pulseData.getSignalData();

        interpolation = interpolator.interpolate(timeList,
                powerList.stream().mapToDouble(d -> d).toArray());

    }

    /**
     * If the argument is less than the pulse width, uses a spline to
     * interpolate the pulse data at {@code time}. Otherwise returns zero.
     */
    @Override
    public double evaluateAt(double time) {
        return time > adjustedPulseWidth ? 0.0 : interpolation.value(time);
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

}
