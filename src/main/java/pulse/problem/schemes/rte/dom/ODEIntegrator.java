package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class ODEIntegrator extends PropertyHolder implements Reflexive {

	private static InstanceDescriptor<PhaseFunction> phaseFunctionSelector = new InstanceDescriptor<PhaseFunction>(
			"Phase function selector", PhaseFunction.class);

	private DiscreteIntensities intensities;
	private PhaseFunction pf;
	private BlackbodySpectrum spectrum;

	public ODEIntegrator(DiscreteIntensities intensities, BlackbodySpectrum ef) {
		setIntensities(intensities);
		this.spectrum = ef;
		phaseFunctionSelector.setSelectedDescriptor(HenyeyGreensteinPF.class.getSimpleName());
	}

	public abstract RTECalculationStatus integrate();

	protected void init(ParticipatingMedium problem) {
		intensities.setEmissivity(problem.getEmissivity());
		intensities.setGrid(new StretchedGrid((double) problem.getOpticalThickness().getValue()));
		setPhaseFunction(problem);
		phaseFunctionSelector.addListener(() -> setPhaseFunction(problem));
		setEmissionFunction(new BlackbodySpectrum(problem));
	}

	protected void treatZeroIndex() {

		var ordinates = intensities.getOrdinates();
		double denominator = 0;
		final double halfAlbedo = pf.getHalfAlbedo();

		// loop through the spatial indices
		for (int j = 0; j < intensities.getGrid().getDensity() + 1; j++) {

			// solve I_k = S_k for mu[k] = 0
			denominator = 1.0 - halfAlbedo * ordinates.getWeight(0) * pf.function(0, 0);
			intensities.setIntensity(j, 0,
					(emission(intensities.getGrid().getNode(j)) + halfAlbedo * pf.sumExcludingIndex(0, j, 0))
							/ denominator);

		}

	}

	public double derivative(int i, int j, double t, double I) {
		return 1.0 / intensities.getOrdinates().getNode(i) * (source(i, j, t, I) - I);
	}

	public double derivative(int i, double t, double[] out, double[] in, int l1, int l2) {
		return 1.0 / intensities.getOrdinates().getNode(i) * (source(i, out, in, t, l1, l2) - out[i - l1]);
	}

	public double partial(int i, double t, double[] inward, int l1, int l2) {
		return (emission(t) + pf.getHalfAlbedo() * pf.inwardPartialSum(i, inward, l1, l2))
				/ intensities.getOrdinates().getNode(i);
	}

	public double partial(int i, int j, double t, int l1, int l2) {
		return (emission(t) + pf.getHalfAlbedo() * pf.partialSum(i, j, l1, l2)) / intensities.getOrdinates().getNode(i);
	}

	public double source(int i, int j, double t, double I) {
		return emission(t) + pf.getHalfAlbedo()
				* (pf.sumExcludingIndex(i, j, i) + pf.function(i, i) * intensities.getOrdinates().getWeight(i) * I);
	}

	public double source(final int i, final double[] iOut, final double[] iIn, final double t, final int l1,
			final int l2) {

		double sumOut = 0;
		final var ordinates = intensities.getOrdinates();

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

	@Override
	public List<Property> listedTypes() {
		return new ArrayList<Property>(Arrays.asList(phaseFunctionSelector));
	}

	public double emission(double t) {
		return (1.0 - 2.0 * pf.getHalfAlbedo()) * spectrum.radianceAt(t);
	}

	public PhaseFunction getPhaseFunction() {
		return pf;
	}

	public InstanceDescriptor<PhaseFunction> getPhaseFunctionSelector() {
		return phaseFunctionSelector;
	}

	private void setPhaseFunction(ParticipatingMedium problem) {
		this.pf = phaseFunctionSelector.newInstance(PhaseFunction.class, problem, intensities);
		pf.init(problem);
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

	public DiscreteIntensities getIntensities() {
		return intensities;
	}

	public void setIntensities(DiscreteIntensities intensities) {
		this.intensities = intensities;
		intensities.setParent(this);
	}

	public BlackbodySpectrum getEmissionFunction() {
		return spectrum;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.spectrum = emissionFunction;
	}

}