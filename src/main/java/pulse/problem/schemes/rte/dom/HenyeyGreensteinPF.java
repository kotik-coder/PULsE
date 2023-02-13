package pulse.problem.schemes.rte.dom;

import static java.lang.Math.sqrt;

import pulse.problem.statements.model.ThermoOpticalProperties;

/**
 * The single-parameter Henyey-Greenstein scattering phase function.
 *
 */
public class HenyeyGreensteinPF extends PhaseFunction {

    private static final long serialVersionUID = -2196189314681832809L;
    private double a1;
    private double a2;
    private double b1;

    public HenyeyGreensteinPF(ThermoOpticalProperties properties, Discretisation intensities) {
        super(properties, intensities);
    }

    @Override
    public void init(ThermoOpticalProperties properties) {
        super.init(properties);
        final double anisotropy = getAnisotropyFactor();
        b1 = 2.0 * anisotropy;
        final double aSq = anisotropy * anisotropy;
        a1 = 1.0 - aSq;
        a2 = 1.0 + aSq;
    }

    @Override
    public double function(final int i, final int k) {
        final double f = a2 - b1 * cosineTheta(i, k);
        return a1 / (f * sqrt(f));
    }

}
