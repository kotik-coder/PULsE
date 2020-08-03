package pulse.problem.schemes.rte.dom;

import static java.lang.Math.sqrt;

import pulse.problem.statements.ParticipatingMedium;

public class HenyeyGreensteinPF extends PhaseFunction {

	private double a1;
	private double a2;
	private double b1;

	public HenyeyGreensteinPF(ParticipatingMedium medium, DiscreteIntensities intensities) {
		super(medium, intensities);
	}

	@Override
	public void init(ParticipatingMedium problem) {
		super.init(problem);
		final double anisotropy = getAnisotropyFactor();
		b1 = 2.0 * anisotropy;
		final double aSq = anisotropy * anisotropy;
		a1 = 1.0 - aSq;
		a2 = 1.0 + aSq;
	}

	@Override
	public double function(final int i, final int k) {
		final var ordinates = getDiscreteIntensities().getOrdinates();
		final double theta = ordinates.getNode(k) * ordinates.getNode(i);
		final double f = a2 - b1 * theta;
		return a1 / (f * sqrt(f));
	}

}