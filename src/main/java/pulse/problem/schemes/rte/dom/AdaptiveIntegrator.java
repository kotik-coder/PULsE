package pulse.problem.schemes.rte.dom;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.ATOL;
import static pulse.properties.NumericPropertyKeyword.GRID_SCALING_FACTOR;
import static pulse.properties.NumericPropertyKeyword.RTE_INTEGRATION_TIMEOUT;
import static pulse.properties.NumericPropertyKeyword.RTOL;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import pulse.math.linear.Vector;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * An ODE integrator with an adaptive step size.
 *
 */

public abstract class AdaptiveIntegrator extends ODEIntegrator {

	private HermiteInterpolator hermite;

	private double atol;
	private double rtol;
	private double scalingFactor;

	private boolean firstRun;
	private boolean rescaled;

	private Instant start;
	private double timeThreshold;

	public AdaptiveIntegrator(Discretisation intensities) {
		super(intensities);
		atol = (double) theDefault(ATOL).getValue();
		rtol = (double) theDefault(RTOL).getValue();
		scalingFactor = (double) theDefault(GRID_SCALING_FACTOR).getValue();
		timeThreshold = (double) theDefault(RTE_INTEGRATION_TIMEOUT).getValue();
		hermite = new HermiteInterpolator();
	}

	@Override
	public RTECalculationStatus integrate() {
		Vector[] v;
		final var intensities = getDiscretisation();
		final var quantities = intensities.getQuantities();
		
		int N = intensities.getGrid().getDensity();
		final int total = intensities.getOrdinates().getTotalNodes();
		rescaled = false;
		
		final int nPositiveStart = intensities.getOrdinates().getFirstPositiveNode();
		final int nNegativeStart = intensities.getOrdinates().getFirstNegativeNode();
		final int halfLength = nNegativeStart - nPositiveStart;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;
		
		for (double error = 1.0, relFactor = 0.0, i0Max = 0, i1Max = 0; (error > atol + relFactor * rtol)
				&& status == RTECalculationStatus.NORMAL; N = intensities.getGrid()
						.getDensity(), status = sanityCheck()) {

			start = Instant.now();
			error = 0;

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.intensitiesLeftBoundary(getEmissionFunction()); // initial value for tau = 0
			i0Max = (new Vector(quantities.getIntensities()[0])).maxAbsComponent();

			firstRun = true;

			for (int j = 0; j < N && error < atol + relFactor * rtol; j++) {

				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, quantities.getIntensities()[j + 1], nPositiveStart, halfLength);

				i1Max = (new Vector(quantities.getIntensities()[j + 1])).maxAbsComponent();
				relFactor = Math.max(i0Max, i1Max);
				i0Max = i1Max;

				error = v[1].maxAbsComponent();
			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.intensitiesRightBoundary(getEmissionFunction()); // initial value for tau = tau_0
			i0Max = (new Vector(quantities.getIntensities()[N])).maxAbsComponent();

			firstRun = true;

			for (int j = N; j > 0 && error < atol + relFactor * rtol; j--) {

				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, quantities.getIntensities()[j - 1], nNegativeStart, halfLength);

				i1Max = (new Vector(quantities.getIntensities()[j - 1])).maxAbsComponent();
				relFactor = Math.max(i0Max, i1Max);
				i0Max = i1Max;

				error = v[1].maxAbsComponent();
			}

			// store derivatives for Hermite interpolation
			for (int i = 0; i < total; i++) {
				quantities.setDerivative(N, i, quantities.getDerivative(N - 1, i));
				quantities.setDerivative(0, i, quantities.getDerivative(1, i));
			}

			if (error > atol + relFactor * rtol) {
				reduceStepSize();
				hermite.clear();
			}

		}

		return status;

	}

	private RTECalculationStatus sanityCheck() {
		if (!NumericProperty.isValueSensible(NumericProperty.theDefault(NumericPropertyKeyword.DOM_GRID_DENSITY),
				getDiscretisation().getGrid().getDensity()))
			return RTECalculationStatus.GRID_TOO_LARGE;
		else if (Duration.between(Instant.now(), start).toSeconds() > timeThreshold)
			return RTECalculationStatus.INTEGRATOR_TIMEOUT;
		return RTECalculationStatus.NORMAL;
	}

	public abstract Vector[] step(final int j, final double sign);

	public void reduceStepSize() {
		var intensities = getDiscretisation();
		final int nNew = (roundEven(scalingFactor * intensities.getGrid().getDensity()));
		generateGrid(nNew);
		intensities.getQuantities().init(intensities.getGrid().getDensity(), intensities.getOrdinates().getTotalNodes());
		rescaled = true;
	}

	public boolean wasRescaled() {
		return rescaled;
	}

	/**
	 * Generates a <b>uniform</b> grid using the argument as the density.
	 * 
	 * @param nNew new grid density
	 */

	public void generateGrid(int nNew) {
		getDiscretisation().getGrid().generateUniformBase(nNew, true);
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
		List<Property> list = super.listedTypes();
		list.add(def(RTOL));
		list.add(def(ATOL));
		list.add(def(GRID_SCALING_FACTOR));
		list.add(def(RTE_INTEGRATION_TIMEOUT));
		return list;
	}

	@Override
	public String toString() {
		return super.toString() + " : " + this.getRelativeTolerance() + " ; " + this.getAbsoluteTolerance() + " ; "
				+ this.getGridScalingFactor();
	}

	public NumericProperty getTimeThreshold() {
		return NumericProperty.derive(RTE_INTEGRATION_TIMEOUT, (double) timeThreshold);
	}

	public void setTimeThreshold(NumericProperty timeThreshold) {
		if (timeThreshold.getType() == RTE_INTEGRATION_TIMEOUT)
			this.timeThreshold = ((Number) timeThreshold.getValue()).longValue();
	}

	public boolean isFirstRun() {
		return firstRun;
	}

	public void setFirstRun(boolean firstRun) {
		this.firstRun = firstRun;
	}

	public HermiteInterpolator getHermiteInterpolator() {
		return hermite;
	}

}