package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;

public class LinearAnisotropicPF extends PhaseFunction {

	private double g;

	public LinearAnisotropicPF(ParticipatingMedium medium, DiscreteIntensities intensities) {
		super(medium, intensities);
		g = 3.0 * A1;
	}

	@Override
	public double partialSum(final int i, final int j, final int n1, final int n2Exclusive) {
		return intensities.incidentRadiation(j, n1, n2Exclusive) + g * intensities.getOrdinates().getNode(i) * intensities.flux(j, n1, n2Exclusive);
	}

	@Override
	public double function(final int i, final int k) {
		var ordinates = intensities.getOrdinates();
		return 1.0 + g * ordinates.getNode(i) * ordinates.getNode(k);
	}

	@Override
	public void setAnisotropyFactor(final double a1) {
		super.setAnisotropyFactor(a1);
		g = 3.0 * A1;
	}

}