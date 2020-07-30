package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class NumericIntegrator extends PropertyHolder implements Reflexive {

	protected DiscreteIntensities intensities;
	protected int nNegativeStart;
	protected int nPositiveStart;
	protected int nH;

	protected PhaseFunction pf;
	private double albedo;
	private double halfAlbedo;

	public DiscreteIntensities getIntensities() {
		return intensities;
	}

	public void setIntensities(DiscreteIntensities intensities) {
		this.intensities = intensities;
	}

	protected BlackbodySpectrum emissionFunction;

	public BlackbodySpectrum getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.emissionFunction = emissionFunction;
	}

	public NumericIntegrator(ParticipatingMedium medium, DiscreteIntensities intensities, BlackbodySpectrum ef,
			PhaseFunction ipf) {
		this.intensities = intensities;
		this.emissionFunction = ef;
		this.pf = ipf;

		medium.addListener(e -> {

			Property p = e.getProperty();

			if (p instanceof NumericProperty) {
				var np = (NumericProperty) p;
				if (np.getType() == NumericPropertyKeyword.SCATTERING_ALBEDO)
					setAlbedo((double) np.getValue());
			}

		});

	}

	public double getAlbedo() {
		return albedo;
	}

	public void init(ParticipatingMedium problem, Grid grid) {
		this.emissionFunction = new BlackbodySpectrum(problem);
		nNegativeStart = intensities.ordinates.getFirstNegativeNode();
		nPositiveStart = intensities.ordinates.getFirstPositiveNode();
		nH = nNegativeStart - nPositiveStart;
	}

	public abstract RTECalculationStatus integrate();

	public void setAlbedo(double albedo) {
		this.albedo = albedo;
		this.halfAlbedo = 0.5 * albedo;
	}

	public void treatZeroIndex() {

		if (intensities.ordinates.hasZeroNode()) {

			double denominator = 0;

			// loop through the spatial indices
			for (int j = 0; j < intensities.grid.getDensity() + 1; j++) {

				// solve I_k = S_k for mu[k] = 0
				denominator = 1.0 - halfAlbedo * intensities.ordinates.w[0] * pf.function(0, 0);
				intensities.I[j][0] = (emission(intensities.grid.getNode(j))
						+ halfAlbedo * pf.sumExcludingIndex(0, j, 0)) / denominator;

			}

		}

	}

	public double derivative(int i, int j, double t, double I) {
		return 1.0 / intensities.ordinates.mu[i] * (source(i, j, t, I) - I);
	}

	public double derivative(int i, double t, double[] out, double[] in, int l1, int l2) {
		return 1.0 / intensities.ordinates.mu[i] * (source(i, out, in, t, l1, l2) - out[i - l1]);
	}

	public double partial(int i, double t, double[] inward, int l1, int l2) {
		return (emission(t) + halfAlbedo * pf.inwardPartialSum(i, inward, l1, l2)) / intensities.ordinates.mu[i];
	}

	public double partial(int i, int j, double t, int l1, int l2) {
		return (emission(t) + halfAlbedo * pf.partialSum(i, j, l1, l2)) / intensities.ordinates.mu[i];
	}

	public double source(int i, int j, double t, double I) {
		return emission(t)
				+ halfAlbedo * (pf.sumExcludingIndex(i, j, i) + pf.function(i, i) * intensities.ordinates.w[i] * I);
	}

	public double source(int i, double[] iOut, double[] iIn, double t, int l1, int l2) {

		double sumOut = 0;

		for (int l = l1; l < l2; l++) {
			// sum over the OUTWARD intensities iOut
			sumOut += iOut[l - l1] * intensities.ordinates.w[l] * pf.function(i, l);
		}

		double sumIn = 0;

		for (int start = intensities.ordinates.total - l2, l = start, end = intensities.ordinates.total
				- l1; l < end; l++) {
			// sum over the INWARD
			// intensities iIn
			sumIn += iIn[l - start] * intensities.ordinates.w[l] * pf.function(i, l);
		}

		return emission(t) + halfAlbedo * (sumIn + sumOut); // contains sum over the incoming rays

	}

	public double emission(double t) {
		return (1.0 - albedo) * emissionFunction.radianceAt(t);
	}

	public PhaseFunction getPhaseFunction() {
		return pf;
	}

	public void setPhaseFunction(PhaseFunction pf) {
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

	@Override
	public boolean ignoreSiblings() {
		return true;
	}

}