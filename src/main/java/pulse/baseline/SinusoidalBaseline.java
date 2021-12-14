package pulse.baseline;

import static java.lang.Math.sin;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.BASELINE_AMPLITUDE;
import static pulse.properties.NumericPropertyKeyword.BASELINE_FREQUENCY;
import static pulse.properties.NumericPropertyKeyword.BASELINE_PHASE_SHIFT;

import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * A simple sinusoidal baseline.
 * <p>
 * It is given by the expression <math><i>y</i> = <i>y</i><sub>0</sub> +
 * <i>A</i> sin(2&pi;<i>f t</i> + &phi;) </math>, where <i>f</i> is the
 * frequency (in Hz), <i>A</i> is the amplitude, &phi; is the phase shift.
 * Extends the {@code FlatBaseline} class and thus inherits the
 * {@code BASELINE_INTERCEPT} property. The sinusoidal baseline is useful to
 * mitigate electromagnetic interferences, with the frequencies usually in the
 * range of 25 to 60 Hz.
 * </p>
 *
 */
public class SinusoidalBaseline extends AdjustableBaseline {

    private double frequency;
    private double phaseShift;
    private double amplitude;
    private final static double _2PI = 2.0 * Math.PI;

    /**
     * Creates a sinusoidal baseline with default properties.
     */
    public SinusoidalBaseline() {
        super(0.0);
        setFrequency(def(BASELINE_FREQUENCY));
        setAmplitude(def(BASELINE_AMPLITUDE));
        setPhaseShift(def(BASELINE_PHASE_SHIFT));
    }

    @Override
    public double valueAt(double x) {
        var intercept = (double) getIntercept().getValue();
        return intercept + amplitude * sin(_2PI * x * frequency + phaseShift);
    }

    /**
     * Listed properties include the frequency, amplitude, phase shift, and
     * intercept.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(BASELINE_FREQUENCY);
        set.add(BASELINE_AMPLITUDE);
        set.add(BASELINE_PHASE_SHIFT);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {

        switch (type) {
            case BASELINE_FREQUENCY:
                setFrequency(property);
                break;
            case BASELINE_PHASE_SHIFT:
                setPhaseShift(property);
                break;
            case BASELINE_AMPLITUDE:
                setAmplitude(property);
                break;
            default:
                super.set(type, property);
        }

    }

    public NumericProperty getFrequency() {
        return derive(BASELINE_FREQUENCY, frequency);
    }

    public NumericProperty getAmplitude() {
        return derive(BASELINE_AMPLITUDE, amplitude);
    }

    public NumericProperty getPhaseShift() {
        return derive(BASELINE_PHASE_SHIFT, phaseShift);
    }

    public void setFrequency(NumericProperty frequency) {
        requireType(frequency, BASELINE_FREQUENCY);
        this.frequency = (double) frequency.getValue();
        firePropertyChanged(this, frequency);
    }

    public void setAmplitude(NumericProperty amplitude) {
        requireType(amplitude, BASELINE_AMPLITUDE);
        this.amplitude = (double) amplitude.getValue();
        firePropertyChanged(this, amplitude);
    }

    public void setPhaseShift(NumericProperty phaseShift) {
        requireType(phaseShift, BASELINE_PHASE_SHIFT);
        this.phaseShift = (double) phaseShift.getValue();
        firePropertyChanged(this, phaseShift);
    }

    /**
     * The optimisation vector can include the amplitude, frequency and phase
     * shift of a sinusoid, and a baseline intercept value of the superclass.
     */
    @Override
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);

        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            switch (key) {
                case BASELINE_FREQUENCY:
                    output.set(i, frequency, BASELINE_FREQUENCY);                  
                    break;
                case BASELINE_PHASE_SHIFT:
                    output.set(i, phaseShift, BASELINE_PHASE_SHIFT);
                    break;
                case BASELINE_AMPLITUDE:
                    output.set(i, amplitude, BASELINE_AMPLITUDE);
                    break;
                default:
                    continue;
            }
            
            output.setTransform(i, new StickTransform(output.getParameterBounds(i)));

        }

    }

    @Override
    public void assign(ParameterVector params) {
        super.assign(params);

        for (int i = 0, size = params.dimension(); i < size; i++) {

            switch (params.getIndex(i)) {
                case BASELINE_FREQUENCY:
                    setFrequency(derive(BASELINE_FREQUENCY, params.inverseTransform(i)));
                    break;
                case BASELINE_PHASE_SHIFT:
                    setPhaseShift(derive(BASELINE_PHASE_SHIFT, params.inverseTransform(i)));
                    break;
                case BASELINE_AMPLITUDE:
                    setAmplitude(derive(BASELINE_AMPLITUDE, params.inverseTransform(i)));
                    break;
                default:
                    break;
            }

        }

    }

    @Override
    public Baseline copy() {
        var baseline = new SinusoidalBaseline();
        baseline.setIntercept(this.getIntercept());
        baseline.amplitude = this.amplitude;
        baseline.frequency = this.frequency;
        baseline.phaseShift = this.phaseShift;
        return baseline;
    }

    @Override
    protected void doFit(List<Double> x, List<Double> y, int size) {
        var flatBaseline = new FlatBaseline();
        flatBaseline.doFit(x, y, size);
        //TODO Fourier transform
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }    

}
