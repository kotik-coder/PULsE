package pulse.problem.schemes.rte.exact;

import static java.lang.Double.compare;

import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;

public class NonscatteringAnalyticalDerivatives extends NonscatteringRadiativeTransfer {

	private FunctionWithInterpolation ei2 = ExponentialIntegrals.get(2);
	private double fd[];
	private double fdStored[];

	public NonscatteringAnalyticalDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
	}

	public double getStoredFluxDerivative(int index) {
		return fdStored[index];
	}
	
	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		fluxes();
		for (int i = 0, N = this.getExternalGridDensity(); i < N; i++) {
			evalFluxDerivative(i);
		}
		return RTECalculationStatus.NORMAL;
	}
	
	@Override
	protected void reinitFluxes(int N) {
		super.reinitFluxes(N);
		fd = new double[N + 1];
		fdStored = new double[N + 1];
	}

	@Override
	public double fluxDerivative(int index) {
		return fd[index];
	}

	@Override
	public double fluxDerivativeRear() {
		return fd[this.getExternalGridDensity()];
	}

	@Override
	public double fluxDerivativeFront() {
		return fd[0];
	}

	@Override
	public void store() {
		super.store();
		System.arraycopy(fd, 0, fdStored, 0, this.getExternalGridDensity() + 1); // store previous results
	}

	@Override
	public double fluxMeanDerivative(int uIndex) {
		return 0.5 * (fd[uIndex] + fdStored[uIndex]);
	}

	@Override
	public double fluxMeanDerivativeFront() {
		return 0.5 * (fd[0] + fdStored[0]);
	}

	@Override
	public double fluxMeanDerivativeRear() {
		int N = this.getExternalGridDensity();
		return 0.5 * (fd[N] + fdStored[N]);
	}

	/*
	 * -dF/d\tau
	 *
	 * = 2 R_1 E_2(y \tau_0) + 2 R_2 E_2( (1 - y) \tau_0 ) - \pi J*(y 'tau_0)
	 *
	 */

	private void evalFluxDerivative(int uIndex) {
		double t = opticalCoordinateAt(uIndex);

		double value = getRadiosityFront() * ei2.valueAt(t) + getRadiosityRear() * ei2.valueAt(getOpticalThickness() - t)
				- 2.0 * getEmissionFunction().powerAt(t) + integrateFirstOrder(t);

		fd[uIndex] = 2.0 * value;
	}

	private double integrateFirstOrder(double y) {
		double integral = 0;
		double tau0 = getOpticalThickness();
		var quadrature = getQuadrature();
		
		setForIntegration(0, y);
		quadrature.setCoefficients(y, -1);
		integral += compare(y, 0) == 0 ? 0 : quadrature.integrate();

		setForIntegration(y, tau0);
		quadrature.setCoefficients(-y, 1);
		integral += compare(y, tau0) == 0 ? 0 : quadrature.integrate();

		return integral;
	}

	
	/**
	 * This will set integration bounds by creating a segment using {@code x} and {@code y} values.
	 * Note this ignores the order of arguments, as the lower and upper bound will be equal to 
	 * {@code min(x,y)} and {@code max(x,y)} respectively. The order of integration is set to unity.
	 * @param x lower bound
	 * @param y upper bound
	 */
	
	private void setForIntegration(double x, double y) {
		var quadrature = getQuadrature();
		quadrature.setBounds(new Segment(x, y));
		quadrature.setOrder(1);
	}

}