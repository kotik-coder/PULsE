package pulse.problem.schemes.rte;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.ParticipatingMedium;
import pulse.util.Descriptive;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class RadiativeTransferSolver extends PropertyHolder implements Reflexive, Descriptive {

	private double[] fluxes;
	private double[] storedFluxes;
	private int N;

	private double _h;
	private double _2h;
	private double _05h;

	private double h;
	private double opticalThickness;

	public RadiativeTransferSolver(ParticipatingMedium problem, Grid grid) {
		reinitArrays((int) grid.getGridDensity().getValue());
	}

	public void reinitArrays(int N) {
		this.N = N;
		this.h = opticalThickness / N;
		fluxes = new double[N + 1];
		storedFluxes = new double[N + 1];
	}

	public void init(ParticipatingMedium p, Grid grid) {
		reinitArrays((int) grid.getGridDensity().getValue());

		this.opticalThickness = (double) p.getOpticalThickness().getValue();
		this.h = opticalThickness / N;

		h = opticalThickness / N;
		_h = 1. / (2.0 * h);
		_2h = _h / 2.0;
		_05h = 2.0 * _h;
	}

	public double getFluxMeanDerivative(int uIndex) {
		double f = (getFlux(uIndex - 1) - getFlux(uIndex + 1))
				+ (getStoredFlux(uIndex - 1) - getStoredFlux(uIndex + 1));
		return f * _2h;
	}

	public double getFluxMeanDerivativeFront() {
		double f = (getFlux(0) - getFlux(1)) + (getStoredFlux(0) - getStoredFlux(1));
		return f * _h;
	}

	public double getFluxMeanDerivativeRear() {
		double f = (getFlux(N - 1) - getFlux(N)) + (getStoredFlux(N - 1) - getStoredFlux(N));
		return f * _h;
	}

	public double getFluxDerivative(int uIndex) {
		return (getFlux(uIndex - 1) - getFlux(uIndex + 1)) * _h;
	}

	public double getFluxDerivativeFront() {
		return (getFlux(0) - getFlux(1)) * _05h;
	}

	public double getFluxDerivativeRear() {
		return (getFlux(N - 1) - getFlux(N)) * _05h;
	}

	public double getFlux(int i) {
		return fluxes[i];
	}

	public void setFlux(int i, double value) {
		this.fluxes[i] = value;
	}

	public double getStoredFlux(int i) {
		return storedFluxes[i];
	}

	public abstract void compute(double[] temperatureArray);

	public int getExternalGridDensity() {
		return N;
	}

	public void store() {
		System.arraycopy(fluxes, 0, storedFluxes, 0, N + 1); // store previous results
	}

	public double getOpticalGridStep() {
		return h;
	}

	public double getOpticalThickness() {
		return opticalThickness;
	}

	@Override
	public boolean ignoreSiblings() {
		return true;
	}

	@Override
	public String getPrefix() {
		return "RTE Solver";
	}

}