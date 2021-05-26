package pulse.math.transforms;

import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;

import pulse.math.Segment;

/**
 * Hyper-tangent parameter transform allowing to set an upper bound for a parameter.
 */

public class AtanhTransform extends BoundedParameterTransform {

	/**
	 * Only the upper bound of the argument is used.
	 * @param bounds the {@code bounda.getMaximum()} is used in the transforms
	 */
	
	public AtanhTransform(Segment bounds) {
		super(bounds);
	}
	
	/**
	 * @see pulse.math.MathUtils.atanh()
	 * @see pulse.math.Segment.getBounds()
	 */

	@Override
	public double transform(double a) {
		return atanh(2.0 * a / getBounds().getMaximum() - 1.0);
	}
	
	/**
	 * @see pulse.math.MathUtils.tanh()
	 * @see pulse.math.Segment.getBounds()
	 */

	@Override
	public double inverse(double t) {
		return 0.5 * getBounds().getMaximum() * (tanh(t) + 1.0);
	}
	
}