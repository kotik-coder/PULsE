package pulse.math.transforms;

import pulse.problem.statements.model.ExtendedThermalProperties;

/**
 * A transform that simply divides the value by the squared length of the
 * sample.
 */
public class InvDiamTransform implements Transformable {

    private double d;

    public InvDiamTransform(ExtendedThermalProperties etp) {
        d = (double) etp.getSampleDiameter().getValue();
    }

    @Override
    public double transform(double value) {
        return value / d;
    }

    @Override
    public double inverse(double t) {
        return t * d;
    }

}
