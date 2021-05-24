package pulse.baseline;

import static java.lang.Math.sin;
import static pulse.math.transforms.StandardTransformations.SQRT;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.BASELINE_AMPLITUDE;
import static pulse.properties.NumericPropertyKeyword.BASELINE_FREQUENCY;
import static pulse.properties.NumericPropertyKeyword.BASELINE_PHASE_SHIFT;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

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

public class SinusoidalBaseline extends FlatBaseline {

	private double frequency;
	private double phaseShift;
	private double amplitude;
	private final static double _2PI = 2.0 * Math.PI;

	/**
	 * Creates a sinusoidal baseline with default properties.
	 */

	public SinusoidalBaseline() {
		super();
		setFrequency(def(BASELINE_FREQUENCY));
		setAmplitude(def(BASELINE_AMPLITUDE));
		setPhaseShift(def(BASELINE_PHASE_SHIFT));
	}

	@Override
	public double valueAt(double x) {
		var intercept = (double) getIntercept().getValue();
		return intercept + amplitude * sin(_2PI * (x * frequency + phaseShift));
	}

	/**
	 * Listed properties include the frequency, amplitude, phase shift, and
	 * intercept.
	 */

	@Override
	public List<Property> listedTypes() {
		var list = super.listedTypes();
		list.add(def(BASELINE_FREQUENCY));
		list.add(def(BASELINE_AMPLITUDE));
		list.add(def(BASELINE_PHASE_SHIFT));
		return list;
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
	 * The optimisation vector can include the amplitude, frequency and phase shift of a sinusoid, and
	 * a baseline intercept value of the superclass.
	 */
	
	@Override
	public void optimisationVector(ParameterVector output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output.dimension(); i < size; i++) {

			var key = output.getIndex(i);
			
			switch (key) {
			case BASELINE_FREQUENCY:
				output.set(i, frequency);
				output.setParameterBounds(i, new Segment(0, 200));
				break;
			case BASELINE_PHASE_SHIFT:
				output.set(i, phaseShift);
				output.setParameterBounds(i, new Segment(-3.14, 3.14) );
				break;
			case BASELINE_AMPLITUDE:
				output.setTransform(i, SQRT);
				output.set(i, amplitude);
				output.setParameterBounds(i, new Segment( 0.0, 10.0 ) );
				break;
			default:
				break;
			}

		}

	}

	@Override
	public void assign(ParameterVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {

			switch (params.getIndex(i)) {
			case BASELINE_FREQUENCY:
				setFrequency(derive(BASELINE_FREQUENCY, params.get(i)));
				break;
			case BASELINE_PHASE_SHIFT:
				setPhaseShift(derive(BASELINE_PHASE_SHIFT, params.get(i)));
				break;
			case BASELINE_AMPLITUDE:
				setAmplitude(derive(BASELINE_AMPLITUDE, params.inverseTransform(i) ));
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


}