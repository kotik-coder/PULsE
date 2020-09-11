package pulse.problem.schemes.rte;

import pulse.properties.NumericProperty;

public class FluxesAndExplicitDerivatives extends Fluxes {

	private double fd[];
	private double fdStored[];
	
	public FluxesAndExplicitDerivatives(NumericProperty gridDensity, NumericProperty opticalThickness) {
		super(gridDensity, opticalThickness);
	}
	
	@Override 
	public void setDensity(NumericProperty gridDensity) { 
		super.setDensity(gridDensity);
		fd = new double[getDensity() + 1];
		fdStored = new double[getDensity() + 1];
	}
	
	@Override
	public double fluxDerivative(int index) {
		return fd[index];
	}

	@Override
	public double fluxDerivativeRear() {
		return fd[getDensity()];
	}

	@Override
	public double fluxDerivativeFront() {
		return fd[0];
	}

	@Override
	public void store() {
		super.store();
		System.arraycopy(fd, 0, fdStored, 0, fd.length); // store previous results
	}

	@Override
	public double meanFluxDerivative(int uIndex) {
		return 0.5 * (fd[uIndex] + fdStored[uIndex]);
	}

	@Override
	public double meanFluxDerivativeFront() {
		return 0.5 * (fd[0] + fdStored[0]);
	}

	@Override
	public double meanFluxDerivativeRear() {
		return 0.5 * (fd[getDensity()] + fdStored[getDensity()]);
	}
	
	public double getStoredFluxDerivative(int index) {
		return fdStored[index];
	}
	
	public double getFluxDerivative(int i) {
		return fd[i];
	}

	public void setFluxDerivative(int i, double f) {
		fd[i] = f;
	}

}