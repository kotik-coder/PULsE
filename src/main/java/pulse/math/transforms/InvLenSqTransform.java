package pulse.math.transforms;

import pulse.problem.statements.model.ThermalProperties;

/**
 * A transform that simply divides the value by the squared length of the
 * sample.
 */
public class InvLenSqTransform implements Transformable {

    private double l;

    public InvLenSqTransform(ThermalProperties tp) {
        this.l = (double) tp.getSampleThickness().getValue();
    }

    @Override
    public double transform(double value) {
        return Math.abs(value) / (l * l);
    }

    @Override
    public double inverse(double t) {
        return Math.abs(t) * (l * l);
    }

}
