package pulse.baseline;

import pulse.math.FFTTransformer;
import pulse.math.Harmonic;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import pulse.DiscreteInput;
import pulse.Response;
import pulse.input.IndexRange;
import pulse.input.Range;
import pulse.math.ParameterVector;
import pulse.math.ZScore;
import pulse.math.filters.Filter;
import pulse.math.filters.OptimisedRunningAverage;
import pulse.math.filters.Randomiser;
import pulse.math.filters.RunningAverage;
import pulse.properties.Flag;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.BASELINE_AMPLITUDE;
import static pulse.properties.NumericPropertyKeyword.BASELINE_FREQUENCY;
import static pulse.properties.NumericPropertyKeyword.BASELINE_INTERCEPT;
import static pulse.properties.NumericPropertyKeyword.BASELINE_PHASE_SHIFT;
import static pulse.properties.NumericPropertyKeyword.BASELINE_SLOPE;
import pulse.search.SimpleOptimisationTask;
import pulse.search.SimpleResponse;
import pulse.search.direction.ActiveFlags;
import pulse.search.statistics.SumOfSquares;
import pulse.util.Group;
import static pulse.properties.NumericPropertyKeyword.MAX_HIGH_FREQ_WAVES;
import static pulse.properties.NumericPropertyKeyword.MAX_LOW_FREQ_WAVES;

/**
 * A multiple-harmonic baseline. Replaces the Sinusoidal baseline in previous
 * version.
 *
 */
public class SinusoidalBaseline extends LinearBaseline {

    private static final long serialVersionUID = -6858521208790195992L;
    private List<Harmonic> hiFreq;
    private List<Harmonic> loFreq;
    private List<Harmonic> active;

    private int maxHighFreqHarmonics;
    private int maxLowFreqHarmonics;

    private final static double FREQUENCY_THRESHOLD = 400;

    /**
     * Creates a sinusoidal baseline with default properties.
     */
    public SinusoidalBaseline() {
        super(0.0, 0.0);
        maxHighFreqHarmonics = (int) def(MAX_HIGH_FREQ_WAVES).getValue();
        maxLowFreqHarmonics = (int) def(MAX_LOW_FREQ_WAVES).getValue();
        hiFreq = new ArrayList<>();
        active = new ArrayList<>();
        loFreq = new ArrayList<>();
    }

    @Override
    public double valueAt(double x) {
        return super.valueAt(x)
                + active.stream().mapToDouble(h -> h.valueAt(x)).sum();
    }

    @Override
    public Baseline copy() {
        var baseline = new SinusoidalBaseline();
        baseline.setIntercept(this.getIntercept());
        baseline.setSlope(this.getSlope());
        baseline.hiFreq = new ArrayList<>();
        baseline.maxHighFreqHarmonics = this.maxHighFreqHarmonics;
        baseline.maxLowFreqHarmonics = this.maxLowFreqHarmonics;
        for (Harmonic h : active) {
            var newH = new Harmonic(h);
            baseline.active.add(newH);
            newH.setParent(baseline);
        }
        for (Harmonic h : hiFreq) {
            baseline.hiFreq.add(new Harmonic(h));
        }
        for (Harmonic h : loFreq) {
            baseline.loFreq.add(new Harmonic(h));
        }
        return baseline;
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        active.forEach(h -> h.optimisationVector(output));
    }

    @Override
    public void assign(ParameterVector output) {
        super.assign(output);
        active.forEach(h
                -> h.assign(output)
        );
    }

    private void guessHarmonics(double[] x, double[] y) {
        var fft = new FFTTransformer(y);
        fft.transform();
        double[] sampling = fft.sampling(x);

        var amplitude = fft.getAmpltiudeSpectrum();
        var phase = fft.getPhaseSpectrum();

        var zscore = new ZScore();
        zscore.process(amplitude);

        var signals = zscore.getSignals();
        double maxAmp = 0;

        hiFreq = new ArrayList<>();

        double span = x[x.length - 1] - x[0];
        double lowerFrequency = 4.0 / span;

        for (int i = 0; i < sampling.length; i++) {
            if (signals[i] > 0) {
                if (sampling[i] < FREQUENCY_THRESHOLD && sampling[i] > lowerFrequency) {
                    var h = new Harmonic(amplitude[i], sampling[i], phase[i]);
                    hiFreq.add(h);
                    maxAmp = Math.max(maxAmp, amplitude[i]);
                }
            }
        }

        active.addAll(sort(hiFreq, maxHighFreqHarmonics));
    }

    private List<Harmonic> sort(List<Harmonic> hs, int limit) {
        var tmp = new ArrayList<>(hs);
        tmp.sort(null);
        Collections.reverse(tmp);
        //leave out a maximum of n harmonics
        return new ArrayList<>(tmp.subList(0, Math.min(tmp.size(), limit)));
    }

    private void labelActive() {
        for (int i = 0, size = active.size(); i < size; i++) {
            active.get(i).setRank(i);
            active.get(i).setParent(this);
        }
    }

    private void fitHarmonics(DiscreteInput input) {

        var sos = new SumOfSquares() {

            @Override
            public void calculateResiduals(DiscreteInput reference, Response estimate) {
                int min = 0;
                int max = reference.getX().size();
                calculateResiduals(reference, estimate, min, max);
            }

        };

        SimpleResponse response = new SimpleResponse(sos) {

            @Override
            public double evaluate(double t) {
                return valueAt(t);
            }

        };

        var task = new SimpleOptimisationTask(this, input) {

            @Override
            public Response getResponse() {
                return response;
            }

        };

        //adjust optimisation flags
        var flagList = new ArrayList<Flag>();
        flagList.add(new Flag(BASELINE_AMPLITUDE, false));
        flagList.add(new Flag(BASELINE_FREQUENCY, true));
        flagList.add(new Flag(BASELINE_PHASE_SHIFT, true));
        flagList.add(new Flag(BASELINE_INTERCEPT, false));
        flagList.add(new Flag(BASELINE_SLOPE, true));

        var oldState = ActiveFlags.storeState();
        ActiveFlags.loadState(flagList);

        CompletableFuture.runAsync(task).thenRun(() -> {
            flagList.stream().filter(f -> f.getType() == BASELINE_AMPLITUDE)
                    .findFirst().get().setValue(true);
            task.run();
            ActiveFlags.loadState(oldState);
        }
        );

    }

    /**
     * @return a set containing {@code BASELINE_INTERCEPT} and
     * {@code BASELINE_SLOPE} keywords
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(MAX_HIGH_FREQ_WAVES);
        set.add(MAX_LOW_FREQ_WAVES);
        return set;
    }

    @Override
    public List<Group> subgroups() {
        return getHarmonics() == null ? new ArrayList<>()
                : getHarmonics().stream().map(h -> (Group) h).collect(Collectors.toList());
    }

    public List<Harmonic> getHarmonics() {
        return active;
    }

    public NumericProperty getHiFreqMax() {
        return derive(MAX_HIGH_FREQ_WAVES, maxHighFreqHarmonics);
    }

    public void setHiFreqMax(NumericProperty maxHarmonics) {
        NumericProperty.requireType(maxHarmonics, MAX_HIGH_FREQ_WAVES);
        int oldValue = this.maxHighFreqHarmonics;

        if ((int) maxHarmonics.getValue() != oldValue) {

            var lowFreq = new ArrayList<Harmonic>();
            int size = active.size();

            if (maxHighFreqHarmonics < size) {
                lowFreq = new ArrayList<>(active.subList(maxHighFreqHarmonics, size));
            }

            this.maxHighFreqHarmonics = (int) maxHarmonics.getValue();
            active.clear();
            active.addAll(sort(hiFreq, maxHighFreqHarmonics));
            active.addAll(lowFreq);
            this.labelActive();
            this.firePropertyChanged(this, maxHarmonics);

        }

    }

    public NumericProperty getLowFreqMax() {
        return derive(MAX_LOW_FREQ_WAVES, maxLowFreqHarmonics);
    }

    public void setLowFreqMax(NumericProperty maxHarmonics) {
        NumericProperty.requireType(maxHarmonics, MAX_LOW_FREQ_WAVES);
        int oldValue = this.maxLowFreqHarmonics;
        if ((int) maxHarmonics.getValue() != oldValue) {
            this.maxLowFreqHarmonics = (int) maxHarmonics.getValue();
            active = new ArrayList<>(active.subList(0, maxHighFreqHarmonics));
            active.addAll(this.sort(loFreq, maxLowFreqHarmonics));
            this.labelActive();
            this.firePropertyChanged(this, maxHarmonics);
        }
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);

        switch (type) {

            case MAX_HIGH_FREQ_WAVES:
                setHiFreqMax(property);
                break;
            case MAX_LOW_FREQ_WAVES:
                setLowFreqMax(property);
                break;
            default:
        }

    }

    @Override
    public void fitTo(DiscreteInput input) {
        //fit the linear part first
        super.fitTo(input);
        //then fit the harmonics -- full range is needed here

        DiscreteInputImpl filtered = (DiscreteInputImpl) filter(input);

        var x = filtered.getXasArray();
        var y = filtered.getYasArray();

        active.clear();
        guessHarmonics(x, y);
        labelActive();
        fitHarmonics(new DiscreteInputImpl(x, y));
        addLowFreq(input);
        labelActive();
    }

    private DiscreteInput filter(DiscreteInput full) {
        var x = full.getX().stream().mapToDouble(d -> d).toArray();
        var y = full.getY().stream().mapToDouble(d -> d).toArray();

        Filter f = new OptimisedRunningAverage();
        Filter fr = new Randomiser(1.0);
        var runningAverage = fr.process(f.process(full));

        var xAv = runningAverage.stream().mapToDouble(p -> p.getX()).toArray();
        var yAv = runningAverage.stream().mapToDouble(p -> p.getY()).toArray();

        var spline = new SplineInterpolator();
        var interp = spline.interpolate(xAv, yAv);

        for (int i = 0; i < x.length; i++) {
            y[i] -= interp.value(x[i]);
            //System.err.println(x[i] + " " + interp.value(x[i]) + " " + y[i]);
        }

        return new DiscreteInputImpl(x, y);

    }

    private void addLowFreq(DiscreteInput input) {
        double amp = !hiFreq.isEmpty()
                ? (double) hiFreq.get(0).getAmplitude().getValue()
                : Collections.max(input.getY()) / 2.0;

        double span = input.getX().get(input.getX().size() - 1) - input.getX().get(0);
        double freq = RunningAverage.DEFAULT_BINS / span;

        loFreq.clear();

        /*
        These harmonics are inaccessible by FFT
         */
        for (double f = freq; f > 1.0 / (2.0 * span); f /= 2.0) {
            loFreq.add(new Harmonic(amp, f, 0.0));
        }

        active.addAll(loFreq.subList(0, Math.min(loFreq.size(), maxLowFreqHarmonics)));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    private class DiscreteInputImpl implements DiscreteInput {

        private final double[] x;
        private final double[] y;

        public DiscreteInputImpl(double[] x, double[] y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public List<Double> getX() {
            return convert(x);
        }

        @Override
        public List<Double> getY() {
            return convert(y);
        }

        public double[] getXasArray() {
            return x;
        }

        public double[] getYasArray() {
            return y;
        }

        private List<Double> convert(double[] a) {
            return DoubleStream.of(a).boxed().collect(Collectors.toList());
        }

        @Override
        public IndexRange getIndexRange() {
            return new IndexRange(getX(), Range.UNLIMITED);
        }
    }

}
