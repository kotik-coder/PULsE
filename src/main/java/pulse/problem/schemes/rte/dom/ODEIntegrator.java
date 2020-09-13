package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class ODEIntegrator extends PropertyHolder implements Reflexive {

	private Discretisation discretisation;
	private PhaseFunction pf;
	private BlackbodySpectrum spectrum;

	public ODEIntegrator(Discretisation intensities) {
		setDiscretisation(intensities);
	}

	public abstract RTECalculationStatus integrate();

	protected void init(ParticipatingMedium problem) {
		discretisation.setEmissivity(problem.getEmissivity());
		discretisation.setGrid(new StretchedGrid((double) problem.getOpticalThickness().getValue()));
		setEmissionFunction(new BlackbodySpectrum(problem));
	}

	protected void treatZeroIndex() {
		var ordinates = discretisation.getOrdinates();

		if (ordinates.hasZeroNode()) {

			var grid = discretisation.getGrid();
			var quantities = discretisation.getQuantities();
			double denominator = 0;
			final double halfAlbedo = pf.getHalfAlbedo();

			// loop through the spatial indices
			for (int j = 0; j < grid.getDensity() + 1; j++) {

				// solve I_k = S_k for mu[k] = 0
				denominator = 1.0 - halfAlbedo * ordinates.getWeight(0) * pf.function(0, 0);
				quantities.setIntensity(j, 0,
						(emission(grid.getNode(j)) + halfAlbedo * pf.sumExcludingIndex(0, j, 0)) / denominator);

			}
		}

	}

	public double derivative(int i, int j, double t, double I) {
		return 1.0 / discretisation.getOrdinates().getNode(i) * (source(i, j, t, I) - I);
	}

	public double derivative(int i, double t, double[] out, double[] in, int l1, int l2) {
		return 1.0 / discretisation.getOrdinates().getNode(i) * (source(i, out, in, t, l1, l2) - out[i - l1]);
	}

	public double partial(int i, double t, double[] inward, int l1, int l2) {
		return (emission(t) + pf.getHalfAlbedo() * pf.inwardPartialSum(i, inward, l1, l2))
				/ discretisation.getOrdinates().getNode(i);
	}

	public double partial(int i, int j, double t, int l1, int l2) {
		return (emission(t) + pf.getHalfAlbedo() * pf.partialSum(i, j, l1, l2))
				/ discretisation.getOrdinates().getNode(i);
	}

	public double source(int i, int j, double t, double I) {
		return emission(t) + pf.getHalfAlbedo()
				* (pf.sumExcludingIndex(i, j, i) + pf.function(i, i) * discretisation.getOrdinates().getWeight(i) * I);
	}

	public double source(final int i, final double[] iOut, final double[] iIn, final double t, final int l1,
			final int l2) {

		double sumOut = 0;
		final var ordinates = discretisation.getOrdinates();

		for (int l = l1; l < l2; l++) {
			// sum over the OUTWARD intensities iOut
			sumOut += iOut[l - l1] * ordinates.getWeight(l) * pf.function(i, l);
		}

		double sumIn = 0;

		for (int start = ordinates.getTotalNodes() - l2, l = start, end = ordinates.getTotalNodes()
				- l1; l < end; l++) {
			// sum over the INWARD
			// intensities iIn
			sumIn += iIn[l - start] * ordinates.getWeight(l) * pf.function(i, l);
		}

		return emission(t) + pf.getHalfAlbedo() * (sumIn + sumOut); // contains sum over the incoming rays

	}

	public double emission(double t) {
		return (1.0 - 2.0 * pf.getHalfAlbedo()) * spectrum.radianceAt(t);
	}

	public PhaseFunction getPhaseFunction() {
		return pf;
	}

	protected void setPhaseFunction(PhaseFunction pf) {
		this.pf = pf;
	}

	@Override
	public String getDescriptor() {
		return "Numeric integrator";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public Discretisation getDiscretisation() {
		return discretisation;
	}

	public void setDiscretisation(Discretisation discretisation) {
		this.discretisation = discretisation;
		discretisation.setParent(this);
	}

	public BlackbodySpectrum getEmissionFunction() {
		return spectrum;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.spectrum = emissionFunction;
	}

}