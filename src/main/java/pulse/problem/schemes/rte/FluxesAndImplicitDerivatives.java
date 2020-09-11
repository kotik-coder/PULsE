package pulse.problem.schemes.rte;

import pulse.properties.NumericProperty;

public class FluxesAndImplicitDerivatives extends Fluxes {
	
	public FluxesAndImplicitDerivatives(NumericProperty gridDensity, NumericProperty opticalThickness) {
		super(gridDensity, opticalThickness);
	}

	@Override
	public double meanFluxDerivative(int uIndex) {
		double f = (getFlux(uIndex - 1) - getFlux(uIndex + 1))
				+ (getStoredFlux(uIndex - 1) - getStoredFlux(uIndex + 1));
		return f * 0.25 / getOpticalGridStep();
	}

	@Override
	public double meanFluxDerivativeFront() {
		double f = (getFlux(0) - getFlux(1)) + (getStoredFlux(0) - getStoredFlux(1));
		return f * 0.5 / getOpticalGridStep();
	}

	@Override
	public double meanFluxDerivativeRear() {
		final int N = this.getDensity();
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
		final int N = this.getDensity();
		return (getFlux(N - 1) - getFlux(N)) / getOpticalGridStep();
	}

}