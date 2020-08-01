package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.List;

import pulse.math.linear.Vector;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public abstract class AdaptiveIntegrator extends NumericIntegrator {

	private double atol;
	private double rtol;
	private double scalingFactor;

	private double[][] f;
	private double[][] Ik;
	private double[][] fk;
	private double[] qLast;

	protected boolean firstRun;
	private boolean rescaled;
	private long calculationStartingTime;

	private long timeThreshold;

	public AdaptiveIntegrator(ParticipatingMedium medium, DiscreteIntensities intensities, BlackbodySpectrum ef,
			PhaseFunction ipf) {
		super(medium, intensities, ef, ipf);
		atol = (double) NumericProperty.theDefault(NumericPropertyKeyword.ATOL).getValue();
		rtol = (double) NumericProperty.theDefault(NumericPropertyKeyword.RTOL).getValue();
		scalingFactor = (double) NumericProperty.theDefault(NumericPropertyKeyword.GRID_SCALING_FACTOR).getValue();
		f = new double[intensities.getGrid().getDensity() + 1][intensities.getOrdinates().getTotalNodes()];
		timeThreshold = ((Number) NumericProperty.theDefault(NumericPropertyKeyword.RTE_INTEGRATION_TIMEOUT).getValue())
				.longValue();
	}

	protected void init() {
		final int total = getIntensities().getOrdinates().getTotalNodes();
		qLast = new double[total];
		int N = getIntensities().getGrid().getDensity();
		f = new double[N + 1][total]; // first index - spatial steps, second index - quadrature
															// points
		rescaled = false;
	}

	public void storeIteration() {
		final var intensities = getIntensities();
		final int density = intensities.getGrid().getDensity();
		final int total = intensities.getOrdinates().getTotalNodes();
		Ik = new double[density + 1][total];
		fk = new double[density + 1][total];

		// store k-th components
		for (int j = 0; j < Ik.length; j++) {
			System.arraycopy(intensities.getIntensities()[j], 0, Ik[j], 0, Ik[0].length);
			System.arraycopy(f[j], 0, fk[j], 0, fk[0].length);
		}

	}

	public void successiveOverrelaxation(double W) {

		final var intensities = getIntensities();
		final int density = intensities.getGrid().getDensity();
		final int total = intensities.getOrdinates().getTotalNodes();
		
		double ONE_MINUS_W = 1.0 - W;

		for (int i = 0; i < density + 1; i++) {
			for (int j = 0; j < total; j++) {
				intensities.setIntensity(i, j, ONE_MINUS_W * Ik[i][j] + W * intensities.getIntensity(i,j));
				f[i][j] = ONE_MINUS_W * fk[i][j] + W * f[i][j];
			}
		}

	}

	@Override
	public RTECalculationStatus integrate() {
		Vector[] v;
		final var intensities = getIntensities();
		int N = intensities.getGrid().getDensity();
		final int total = intensities.getOrdinates().getTotalNodes();
		init();
		
		final int nPositiveStart = intensities.getOrdinates().getFirstPositiveNode();
		final int nNegativeStart = intensities.getOrdinates().getFirstNegativeNode();
		final int halfLength = nNegativeStart - nPositiveStart;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;

		for (double error = 1.0, relFactor = 0.0, i0Max = 0, i1Max = 0; (error > atol + relFactor * rtol)
				&& status == RTECalculationStatus.NORMAL; N = intensities.getGrid().getDensity(), status = sanityCheck()) {

			calculationStartingTime = System.nanoTime();
			error = 0;

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(getEmissionFunction()); // initial value for tau = 0
			i0Max = (new Vector(intensities.getIntensities()[0])).maxAbsComponent();

			firstRun = true;

			for (int j = 0; j < N && error < atol + relFactor * rtol; j++) {

				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.getIntensities()[j + 1], nPositiveStart, halfLength);

				i1Max = (new Vector(intensities.getIntensities()[j + 1])).maxAbsComponent();
				relFactor = Math.max(i0Max, i1Max);
				i0Max = i1Max;

				error = v[1].maxAbsComponent();
			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(getEmissionFunction()); // initial value for tau = tau_0
			i0Max = (new Vector(intensities.getIntensities()[N])).maxAbsComponent();

			firstRun = true;

			for (int j = N; j > 0 && error < atol + relFactor * rtol; j--) {

				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.getIntensities()[j - 1], nNegativeStart, halfLength);

				i1Max = (new Vector(intensities.getIntensities()[j - 1])).maxAbsComponent();
				relFactor = Math.max(i0Max, i1Max);
				i0Max = i1Max;

				error = v[1].maxAbsComponent();
			}

			// store derivatives for Hermite interpolation
			for (int i = 0; i < total; i++) {
				f[N][i] = f[N - 1][i];
				f[0][i] = f[1][i];
			}

			if (error > atol + relFactor * rtol) {
				reduceStepSize();
				HermiteInterpolator.clear();
			}

		}

		return status;

	}

	private RTECalculationStatus sanityCheck() {
		if (!NumericProperty.isValueSensible(NumericProperty.theDefault(NumericPropertyKeyword.DOM_GRID_DENSITY),
				getIntensities().getGrid().getDensity()))
			return RTECalculationStatus.GRID_TOO_LARGE;
		else if (System.nanoTime() - calculationStartingTime > timeThreshold)
			return RTECalculationStatus.INTEGRATOR_TIMEOUT;
		return RTECalculationStatus.NORMAL;
	}

	public abstract Vector[] step(final int j, final double sign);

	public void reduceStepSize() {
		var intensities = getIntensities();
		final int nNew = (roundEven(scalingFactor * intensities.getGrid().getDensity()));
		generateGrid(nNew);
		intensities.reinitInternalArrays();
		intensities.clearBoundaryFluxes();
		rescaled = true;
		int N = intensities.getGrid().getDensity();
		f = new double[N + 1][intensities.getOrdinates().getTotalNodes()]; // first index - spatial steps, second index - quadrature
															// points
	}

	public boolean wasRescaled() {
		return rescaled;
	}

	public void generateGrid(int nNew) {
		getIntensities().getGrid().generateUniform(nNew, true);
	}

	private int roundEven(double a) {
		return (int) (a / 2 * 2);
	}

	public NumericProperty getRelativeTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.RTOL, rtol);
	}

	public NumericProperty getAbsoluteTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.ATOL, atol);
	}

	public NumericProperty getGridScalingFactor() {
		return NumericProperty.derive(NumericPropertyKeyword.GRID_SCALING_FACTOR, scalingFactor);
	}

	public void setRelativeTolerance(NumericProperty p) {
		if (p.getType() != NumericPropertyKeyword.RTOL)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.rtol = (double) p.getValue();
	}

	public void setAbsoluteTolerance(NumericProperty p) {
		if (p.getType() != NumericPropertyKeyword.ATOL)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.atol = (double) p.getValue();
	}

	public void setGridScalingFactor(NumericProperty p) {
		if (p.getType() != NumericPropertyKeyword.GRID_SCALING_FACTOR)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.scalingFactor = (double) p.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case RTOL:
			setRelativeTolerance(property);
			break;
		case ATOL:
			setAbsoluteTolerance(property);
			break;
		case GRID_SCALING_FACTOR:
			setGridScalingFactor(property);
			break;
		case RTE_INTEGRATION_TIMEOUT:
			setTimeThreshold(property);
			break;
		default:
			return;
		}

		firePropertyChanged(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(NumericProperty.def(NumericPropertyKeyword.RTOL));
		list.add(NumericProperty.def(NumericPropertyKeyword.ATOL));
		list.add(NumericProperty.def(NumericPropertyKeyword.GRID_SCALING_FACTOR));
		list.add(NumericProperty.def(NumericPropertyKeyword.RTE_INTEGRATION_TIMEOUT));
		return list;
	}

	@Override
	public String toString() {
		return super.toString() + " : " + this.getRelativeTolerance() + " ; " + this.getAbsoluteTolerance() + " ; "
				+ this.getGridScalingFactor();
	}

	public NumericProperty getTimeThreshold() {
		return NumericProperty.derive(NumericPropertyKeyword.RTE_INTEGRATION_TIMEOUT, (double) timeThreshold);
	}

	public void setTimeThreshold(NumericProperty timeThreshold) {
		if (timeThreshold.getType() == NumericPropertyKeyword.RTE_INTEGRATION_TIMEOUT)
			this.timeThreshold = ((Number) timeThreshold.getValue()).longValue();
	}
	
	public double[][] getDerivatives() {
		return f;
	}
	
	public boolean isFirstRun() {
		return firstRun;
	}
	
	public void setFirstRun(boolean firstRun) {
		this.firstRun = firstRun;
	}

	public double getQLast(int i) {
		return qLast[i];
	}

	public void setQLast(int i, double q) {
		this.qLast[i] = q;
	}
	
	public double getDerivative(int i, int j) {
		return f[i][j];
	}

	public void setDerivative(int i, int j, double f) {
		this.f[i][j] = f;
	}

}