package pulse.math.transforms;

import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;

import pulse.math.Segment;

/**
 * Hyper-tangent parameter transform.
 */

public class AtanhTransform extends BoundedParameterTransform {

	public AtanhTransform(Segment bounds) {
		super(bounds);
	}

	@Override
	public double transform(double a) {
		return atanh(2.0 * a / getBounds().getMaximum() - 1.0);
	}

	@Override
	public double inverse(double t) {
		return 0.5 * getBounds().getMaximum() * (tanh(t) + 1.0);
	}
	
}