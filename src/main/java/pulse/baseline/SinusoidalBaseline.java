package pulse.baseline;

import static java.lang.Math.sin;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.BASELINE_AMPLITUDE;
import static pulse.properties.NumericPropertyKeyword.BASELINE_FREQUENCY;
import static pulse.properties.NumericPropertyKeyword.BASELINE_PHASE_SHIFT;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * A simple sinusoidal baseline. <p>It is given by the expression <math><<i>y</i> = <i>y</i><sub>0</sub> + <i>A</i> sin(2&pi;<i>f t</i> + &phi;) </math>,
 * where <i>f</i> is the frequency (in Hz), <i>A</i> is the amplitude, &phi; is the phase shift. Extends the {@code FlatBaseline} class and 
 * thus inherits the {@code BASELINE_INTERCEPT} property. The sinusoidal baseline is useful to mitigate electromagnetic interferences, with 
 * the frequencies usually in the range of 25 to 60 Hz.</p>  
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
		setFrequency(theDefault(BASELINE_FREQUENCY));
		setAmplitude(theDefault(BASELINE_AMPLITUDE));
		setPhaseShift(theDefault(BASELINE_PHASE_SHIFT));
	}

	@Override
	public double valueAt(double x) {
		var intercept = (double) getIntercept().getValue();
		return intercept + amplitude * sin(_2PI * (x * frequency + phaseShift));
	}

	/**
	 * Listed properties include the frequency, amplitude, phase shift, and intercept.
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
			return;
		}

		this.firePropertyChanged(this, property);

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
	}

	public void setAmplitude(NumericProperty amplitude) {
		requireType(amplitude, BASELINE_AMPLITUDE);
		this.amplitude = (double) amplitude.getValue();
	}

	public void setPhaseShift(NumericProperty phaseShift) {
		requireType(phaseShift, BASELINE_PHASE_SHIFT);
		this.phaseShift = (double) phaseShift.getValue();
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {

			switch (output[0].getIndex(i)) {
			case BASELINE_FREQUENCY:
				output[0].set(i, frequency);
				output[1].set(i, 10);
				break;
			case BASELINE_PHASE_SHIFT:
				output[0].set(i, phaseShift);
				output[1].set(i, 1.0);
				break;
			case BASELINE_AMPLITUDE:
				output[0].set(i, amplitude);
				output[1].set(i, 1.0);
				break;
			default:
				break;
			}

		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {

			switch (params.getIndex(i)) {
			case BASELINE_FREQUENCY:
				setFrequency(NumericProperty.derive(BASELINE_FREQUENCY, params.get(i)));
				break;
			case BASELINE_PHASE_SHIFT:
				setPhaseShift(NumericProperty.derive(BASELINE_PHASE_SHIFT, params.get(i)));
				break;
			case BASELINE_AMPLITUDE:
				setAmplitude(NumericProperty.derive(BASELINE_AMPLITUDE, params.get(i)));
				break;
			default:
				break;
			}

		}

	}

}