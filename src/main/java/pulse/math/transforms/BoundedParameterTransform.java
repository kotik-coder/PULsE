package pulse.math.transforms;

import pulse.math.Segment;

/**
 * An abstract {@code Transformable} where the bounds of the parameter is manually set.
 * Subclasses can be bounded from either on or both sides.
 *
 */

public abstract class BoundedParameterTransform implements Transformable {

	private Segment bounds;

	public BoundedParameterTransform(Segment bounds) {
		setBounds(bounds);
	}
	
	public Segment getBounds() {
		return bounds;
	}

	public void setBounds(Segment bounds) {
		this.bounds = bounds;
	}
	
}
