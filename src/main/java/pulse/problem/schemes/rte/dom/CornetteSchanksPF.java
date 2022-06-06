package pulse.problem.schemes.rte.dom;

import static java.lang.Math.sqrt;

import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.model.ThermoOpticalProperties;

/**
 * The single-parameter Cornette-Schanks scattering phase function.
 * It converges to the Rayleigh phase function as 〈μ〉 → 0 and approaches 
 * the Henyey–Greenstein phase function as |〈μ〉| → 1
 * @see https://doi.org/10.1364/ao.31.003152
 *
 */
public class CornetteSchanksPF extends PhaseFunction {

    private double anisoFactor;
    private double onePlusGSq;
    private double g2;

    public CornetteSchanksPF(ThermoOpticalProperties top, Discretisation intensities) {
        super(top, intensities);
    }

    @Override
    public void init(ThermoOpticalProperties top) {
        super.init(top);
        final double anisotropy = getAnisotropyFactor();
        g2 = 2.0 * anisotropy;
        final double aSq = anisotropy * anisotropy;
        onePlusGSq = 1.0 + aSq;
        anisoFactor = 1.5*(1.0 - aSq)/(2.0 + aSq);
    }

    @Override
    public double function(final int i, final int k) {
        double cosine = cosineTheta(i,k);
        final double f = onePlusGSq - g2 * cosine;
        return anisoFactor * (1.0 + cosine*cosine) / (f * sqrt(f));
    }

}