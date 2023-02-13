package pulse.math;

import static java.lang.Math.cos;
import java.util.Set;
import pulse.math.transforms.PeriodicTransform;
import pulse.math.transforms.StandardTransformations;
import pulse.math.transforms.StickTransform;
import pulse.math.transforms.Transformable;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import static pulse.properties.NumericProperty.requireType;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.BASELINE_AMPLITUDE;
import static pulse.properties.NumericPropertyKeyword.BASELINE_FREQUENCY;
import static pulse.properties.NumericPropertyKeyword.BASELINE_PHASE_SHIFT;
import pulse.search.Optimisable;
import pulse.util.PropertyHolder;

/**
 *
 * Harmonic class.
 * <p>
 * It is given by the expression <math><i>y</i> = <i>y</i><sub>0</sub> +
 * <i>A</i> cos(2&pi;<i>f t</i> + &phi;) </math>, where <i>f</i> is the
 * frequency (in Hz), <i>A</i> is the amplitude, &phi; is the phase shift.
 * </p>
 *
 *
 */
public class Harmonic extends PropertyHolder implements Optimisable, Comparable<Harmonic> {

    private static final long serialVersionUID = 3732379391172485157L;

    private int rank = -1;

    private double amplitude;
    private double frequency;
    private double phaseShift;

    private final static double _2PI = 2.0 * Math.PI;

    public Harmonic() {
        setFrequency(def(BASELINE_FREQUENCY));
        setAmplitude(def(BASELINE_AMPLITUDE));
        setPhaseShift(def(BASELINE_PHASE_SHIFT));
    }

    public Harmonic(double amplitude, double frequency, double phaseShift) {
        this.amplitude = amplitude;
        this.frequency = frequency;
        this.phaseShift = phaseShift;
    }

    public Harmonic(Harmonic h) {
        this.amplitude = h.amplitude;
        this.frequency = h.frequency;
        this.phaseShift = h.phaseShift;
        this.rank = h.rank;
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

    public final void setFrequency(NumericProperty frequency) {
        requireType(frequency, BASELINE_FREQUENCY);
        this.frequency = (double) frequency.getValue();
        firePropertyChanged(this, frequency);
    }

    public final void setAmplitude(NumericProperty amplitude) {
        requireType(amplitude, BASELINE_AMPLITUDE);
        this.amplitude = (double) amplitude.getValue();
        firePropertyChanged(this, amplitude);
    }

    public final void setPhaseShift(NumericProperty phaseShift) {
        requireType(phaseShift, BASELINE_PHASE_SHIFT);
        this.phaseShift = (double) phaseShift.getValue();
        firePropertyChanged(this, phaseShift);
    }

    /**
     * Amplitude form of the Fourier harmonic
     *
     * @param x
     * @return
     */
    public double valueAt(double x) {
        return amplitude * cos(_2PI * x * frequency + phaseShift);
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
        }

    }

    /**
     * The optimisation vector can include the amplitude, frequency and phase
     * shift of a sinusoid, and a baseline intercept value of the superclass.
     */
    @Override
    public void optimisationVector(ParameterVector output) {

        var params = output.getParameters();

        for (int i = 0, size = params.size(); i < size; i++) {

            var p = params.get(i);
            var id = p.getIdentifier();
            var bounds = Segment.boundsFrom(id.getKeyword());

            double value;

            Transformable transform = null;

            switch (id.getKeyword()) {
                case BASELINE_FREQUENCY:
                    value = frequency;
                    transform = StandardTransformations.ABS;
                    break;
                case BASELINE_PHASE_SHIFT:
                    value = phaseShift;
                    transform = new PeriodicTransform(bounds);
                    break;
                case BASELINE_AMPLITUDE:
                    value = amplitude;
                    transform = new StickTransform(bounds);
                    break;
                default:
                    continue;
            }

            var newId = new ParameterIdentifier(id.getKeyword(), rank);

            if (id.getIndex() == rank) {
                p.setBounds(bounds);
                p.setTransform(transform);
                p.setValue(value);
            } else if (rank > -1) {

                boolean matchFound = output.getParameters().stream().anyMatch(pp -> {
                    var key = pp.getIdentifier().getKeyword();
                    int index = pp.getIdentifier().getIndex();
                    return key == id.getKeyword() && rank == index;
                });

                if (!matchFound) {

                    var newParam = new Parameter(newId, transform, bounds);
                    newParam.setValue(value);
                    params.add(newParam);

                }

            }

        }

    }

    @Override
    public void assign(ParameterVector params) {

        for (Parameter p : params.getParameters()) {

            var id = p.getIdentifier();

            if (id.getIndex() == rank) {

                switch (id.getKeyword()) {
                    case BASELINE_FREQUENCY:
                        setFrequency(derive(BASELINE_FREQUENCY, p.inverseTransform()));
                        break;
                    case BASELINE_PHASE_SHIFT:
                        setPhaseShift(derive(BASELINE_PHASE_SHIFT, p.inverseTransform()));
                        break;
                    case BASELINE_AMPLITUDE:
                        setAmplitude(derive(BASELINE_AMPLITUDE, p.inverseTransform()));
                        break;
                    default:
                        break;
                }

            }

        }

    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public Harmonic increaseAmplitudeBy(int amplitudeFactor) {
        var h = new Harmonic(this);
        h.amplitude *= amplitudeFactor;
        return h;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Harmonic)) {
            return false;
        }

        Harmonic oH = (Harmonic) o;

        final double tolerance = 1E-3;

        return Math.abs(oH.amplitude - this.amplitude)
                / Math.abs(oH.amplitude + this.amplitude) < tolerance
                && Math.abs(oH.frequency - this.frequency)
                / Math.abs(oH.frequency + this.frequency) < tolerance
                && Math.abs(oH.phaseShift - this.phaseShift)
                / Math.abs(oH.phaseShift + this.phaseShift) < tolerance;
    }

    @Override
    public int compareTo(Harmonic o) {
        return this.getAmplitude().compareTo(o.getAmplitude());
    }

    @Override
    public String toString() {
        return String.format("[%1d]: f = %3.2f, A = %3.2f, phi = %3.2f",
                rank, frequency, amplitude, phaseShift);
    }

}
