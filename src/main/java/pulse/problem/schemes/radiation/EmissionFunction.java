package pulse.problem.schemes.radiation;

import static pulse.problem.schemes.radiation.MathUtils.fastPowLoop;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.NonlinearProblem;

public class EmissionFunction {

	protected double tFactor;
	protected double hx;
	
	public EmissionFunction(NonlinearProblem p, Grid grid) {
		tFactor = p.maximumHeating()/( (double)p.getTestTemperature().getValue() );
		this.hx = grid.getXStep();
	}
	
	public EmissionFunction(double tFactor, double hx) {
		this.tFactor = tFactor;
		this.hx = hx;
	}
	
	public double function(double t) {
		return 0.25/tFactor*fastPowLoop(1.0 + t*tFactor, 4);
	}
	
	public double functionRelative(double t) {
		return 0.25/tFactor*(fastPowLoop(1.0 + t*tFactor, 4) - 1.0);
	}
	
	public double firstDerivative(double[] U, int uIndex) {
		return (function(U[uIndex + 1]) - function(U[uIndex - 1]))/(2.0*hx); 
	}
	
	public double firstDerivative_1(double t) {
		return fastPowLoop(1.0 + t*tFactor, 3);
	}
	
	public double secondDerivative(double[] U, int uIndex) {
		return (function(U[uIndex + 1]) - 2.0*function(U[uIndex]) + function(U[uIndex - 1]))/(hx*hx); 
	}

	public double getReductionFactor() {
		return tFactor;
	}

	public void setReductionFactor(double tFactor) {
		this.tFactor = tFactor;
	}

	public double getGridStep() {
		return hx;
	}

	public void setGridStep(double hx) {
		this.hx = hx;
	}
	
}