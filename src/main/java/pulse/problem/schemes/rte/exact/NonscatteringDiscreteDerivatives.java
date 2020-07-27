package pulse.problem.schemes.rte.exact;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;

public class NonscatteringDiscreteDerivatives extends NonscatteringRadiativeTransfer {

	public NonscatteringDiscreteDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
	}

	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		fluxes();
		return RTECalculationStatus.NORMAL;
	}

	@Override
	public double fluxMeanDerivative(int uIndex) {
		double f = (getFlux(uIndex - 1) - getFlux(uIndex + 1))
				+ (getStoredFlux(uIndex - 1) - getStoredFlux(uIndex + 1));
		return f * 0.25 / getOpticalGridStep();
	}

	@Override
	public double fluxMeanDerivativeFront() {
		double f = (getFlux(0) - getFlux(1)) + (getStoredFlux(0) - getStoredFlux(1));
		return f * 0.5 / getOpticalGridStep();
	}

	@Override
	public double fluxMeanDerivativeRear() {
		final int N = this.getExternalGridDensity();
		double f = (getFlux(N - 1) - getFlux(N)) + (getStoredFlux(N - 1) - getStoredFlux(N));
		return f * 0.5 / getOpticalGridStep();
	}

	@Override
	public double fluxDerivative(int uIndex) {
		return (getFlux(uIndex - 1) - getFlux(uIndex + 1)) * 0.5 / getOpticalGridStep();
	}

	@Override
	public double fluxDerivativeFront() {
		return (getFlux(0) - getFlux(1)) / getOpticalGridStep();
	}

	@Override
	public double fluxDerivativeRear() {
		final int N = this.getExternalGridDensity();
		return (getFlux(N - 1) - getFlux(N)) / getOpticalGridStep();
	}

}