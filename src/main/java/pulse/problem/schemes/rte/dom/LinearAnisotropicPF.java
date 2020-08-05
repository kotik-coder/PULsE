package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;

public class LinearAnisotropicPF extends PhaseFunction {

	private double g;

	public LinearAnisotropicPF(ParticipatingMedium medium, Discretisation intensities) {
		super(medium, intensities);
	}
	
	@Override
	public void init(ParticipatingMedium medium) {
		super.init(medium);
		g = 3.0 * getAnisotropyFactor();
	}

	@Override
	public double partialSum(final int i, final int j, final int n1, final int n2Exclusive) {
		final var intensities = getDiscreteIntensities();
		return intensities.incidentRadiation(j, n1, n2Exclusive) + g * intensities.getOrdinates().getNode(i) * intensities.flux(j, n1, n2Exclusive);
	}

	@Override
	public double function(final int i, final int k) {
		final var ordinates = getDiscreteIntensities().getOrdinates();
		return 1.0 + g * ordinates.getNode(i) * ordinates.getNode(k);
	}

}