package pulse.math.transforms;

import pulse.math.Segment;

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
