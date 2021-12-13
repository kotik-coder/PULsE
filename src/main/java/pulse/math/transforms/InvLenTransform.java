package pulse.math.transforms;

import pulse.problem.statements.model.ThermalProperties;

/**
 * A transform that simply divides the value by the length of the sample.
 */
public class InvLenTransform implements Transformable {

    private double l;

    public InvLenTransform(ThermalProperties tp) {
        l = (double) tp.getSampleThickness().getValue();
    }

    @Override
    public double transform(double value) {
        return value / l;
    }

    @Override
    public double inverse(double t) {
        return t * l;
    }

}
