package pulse.problem.laser;

import static java.lang.Math.signum;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

/**
 * The simplest pulse shape defined as <math>0.5*(1 +
 * sgn(<i>t</i><sub>pulse</sub> - <i>t</i>))</math>, where <math>sgn(...)</math>
 * is the signum function, <sub>pulse</sub> is the pulse width.
 * 
 * @see java.lang.Math.signum(double)
 */

public class RectangularPulse extends PulseTemporalShape {

	/**
	 * @param time the time measured from the start of the laser pulse.
	 */
	
	@Override
	public double evaluateAt(double time) {
		var width = getPulseWidth();
		return 0.5 / width * (1 + signum(width - time));
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// intentionally blak
	}

}