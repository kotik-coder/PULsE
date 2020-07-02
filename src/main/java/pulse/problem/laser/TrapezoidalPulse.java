package pulse.problem.laser;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.TRAPEZOIDAL_FALL_PERCENTAGE;
import static pulse.properties.NumericPropertyKeyword.TRAPEZOIDAL_RISE_PERCENTAGE;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class TrapezoidalPulse extends PulseTemporalShape {

	private double rise;
	private double fall;
	private double h;

	public TrapezoidalPulse() {
		rise = (int) def(TRAPEZOIDAL_RISE_PERCENTAGE).getValue() / 100.0;
		fall = (int) def(TRAPEZOIDAL_FALL_PERCENTAGE).getValue() / 100.0;
		h = height();
	}
	
	@Override
	public void init(DiscretePulse pulse) {
		super.init(pulse);
		h = height();
	}

	private double height() {
		return 2.0 / (getPulseWidth() * (2.0 - rise - fall));
	}

	@Override
	public double evaluateAt(double time) {
		final var reducedTime = time / getPulseWidth();

		double result = 0;

		if (reducedTime < rise) { // triangular
			result = reducedTime * h / rise;
		} else if (reducedTime < 1.0 - fall) { // rectangular
			result = h;
		} else if (reducedTime < 1.0) { // triangular
			final var t2 = (reducedTime - (1.0 - fall));
			result = (fall - t2) * h / fall;
		}

		return result;

	}

	@Override
	public List<Property> listedTypes() {
		var list = super.listedTypes();
		list.add(def(TRAPEZOIDAL_RISE_PERCENTAGE));
		list.add(def(TRAPEZOIDAL_FALL_PERCENTAGE));
		return list;
	}

	public NumericProperty getTrapezoidalRise() {
		return derive(TRAPEZOIDAL_RISE_PERCENTAGE, (int) (rise * 100));
	}

	public NumericProperty getTrapezoidalFall() {
		return derive(TRAPEZOIDAL_FALL_PERCENTAGE, (int) (fall * 100));
	}

	public void setTrapezoidalRise(NumericProperty p) {
		requireType(p, TRAPEZOIDAL_RISE_PERCENTAGE);
		this.rise = (int) p.getValue() / 100.0;
		h = height();
	}

	public void setTrapezoidalFall(NumericProperty p) {
		requireType(p, TRAPEZOIDAL_FALL_PERCENTAGE);
		this.fall = (int) p.getValue() / 100.0;
		h = height();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case TRAPEZOIDAL_RISE_PERCENTAGE:
			setTrapezoidalRise(property);
			break;
		case TRAPEZOIDAL_FALL_PERCENTAGE:
			setTrapezoidalFall(property);
			break;
		default:
			break;
		}
		firePropertyChanged(this, property);
	}

}