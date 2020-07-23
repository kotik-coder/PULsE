package pulse.problem.schemes.rte.exact;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;

public class NonscatteringAnalyticalDerivatives extends NonscatteringRadiativeTransfer {

	private double FD[], FDP[];

	public NonscatteringAnalyticalDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
	}

	@Override
	public void reinitArrays(int N) {
		super.reinitArrays(N);
		FD = new double[N + 1];
		FDP = new double[N + 1];
	}

	public double getStoredFluxDerivative(int index) {
		return FDP[index];
	}

	@Override
	public double getFluxDerivative(int index) {
		return FD[index];
	}

	@Override
	public double getFluxDerivativeRear() {
		return FD[this.getExternalGridDensity()];
	}

	@Override
	public double getFluxDerivativeFront() {
		return FD[0];
	}

	@Override
	public void store() {
		super.store();
		System.arraycopy(FD, 0, FDP, 0, this.getExternalGridDensity() + 1); // store previous results
	}

	@Override
	public double getFluxMeanDerivative(int uIndex) {
		return 0.5 * (FD[uIndex] + FDP[uIndex]);
	}

	@Override
	public double getFluxMeanDerivativeFront() {
		return 0.5 * (FD[0] + FDP[0]);
	}

	@Override
	public double getFluxMeanDerivativeRear() {
		int N = this.getExternalGridDensity();
		return 0.5 * (FD[N] + FDP[N]);
	}

	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		radiosities();
		fluxDerivativeFront(U);
		for (int i = 1, N = this.getExternalGridDensity(); i < N; i++) {
                    fluxDerivative(U, i);
                }
		fluxDerivativeRear(U);
		boundaryFluxes();
		return RTECalculationStatus.NORMAL;
	}

	/*
	 * -dF/d\tau
	 *
	 * = 2 R_1 E_2(y \tau_0) + 2 R_2 E_2( (1 - y) \tau_0 ) - \pi J*(y 'tau_0)
	 *
	 */

	private void fluxDerivative(double U[], int uIndex) {
		double t = hx * uIndex * tau0;

		double value = r1 * simpleIntegrator.integralAt(t, 2) + r2 * simpleIntegrator.integralAt(tau0 - t, 2)
				- 2.0 * emissionFunction.powerAt(t) + integrateFirstOrder(t);

		FD[uIndex] = 2.0 * value;

	}

	private void fluxDerivativeFront(double[] U) {
		double value = r1 * simpleIntegrator.integralAt(0, 2) + r2 * simpleIntegrator.integralAt(tau0, 2)
				- 2.0 * emissionFunction.powerAt(0.0) + integrateFirstOrderFront();

		FD[0] = 2.0 * value;

	}

	private void fluxDerivativeRear(double[] U) {
		int N = this.getExternalGridDensity();
		double t = hx * N * tau0;

		double value = r1 * simpleIntegrator.integralAt(t, 2) + r2 * simpleIntegrator.integralAt(tau0 - t, 2)
				- 2.0 * emissionFunction.powerAt(t) + integrateFirstOrderRear();

		FD[N] = 2.0 * value;
	}

	private double integrateFirstOrder(double y) {
		double integral = 0;

		complexIntegrator.setRange(0, y);
		integral += Double.compare(y, 0) == 0 ? 0 : complexIntegrator.integrate(1, y, -1);

		complexIntegrator.setRange(y, tau0);
		integral += Double.compare(y, tau0) == 0 ? 0 : complexIntegrator.integrate(1, -y, 1);

		return integral;

	}

	private double integrateFirstOrderFront() {
		complexIntegrator.setRange(0, tau0);
		return complexIntegrator.integrate(1, 0, 1);
	}

	private double integrateFirstOrderRear() {
		complexIntegrator.setRange(0, tau0);
		return complexIntegrator.integrate(1, tau0, -1);
	}

}