package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.model.ThermoOpticalProperties;

/**
 * The linear-anisotropic scattering phase function.
 *
 */
public class LinearAnisotropicPF extends PhaseFunction {

    private double g;

    public LinearAnisotropicPF(ThermoOpticalProperties top, Discretisation intensities) {
        super(top, intensities);
    }

    @Override
    public void init(ThermoOpticalProperties top) {
        super.init(top);
        g = 3.0 * getAnisotropyFactor();
    }

    @Override
    public double partialSum(final int i, final int j, final int n1, final int n2Exclusive) {
        final var intensities = getDiscreteIntensities();
        return intensities.incidentRadiation(j, n1, n2Exclusive) + g * intensities.getOrdinates().getNode(i) * intensities.flux(j, n1, n2Exclusive);
    }

    @Override
    public double function(final int i, final int k) {
        return 1.0 + g * cosineTheta(i,k);
    }

}
