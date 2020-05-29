package pulse.problem.schemes.rte;

import static pulse.problem.schemes.rte.MathUtils.fastPowLoop;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.NonlinearProblem;

public class EmissionFunction {

	protected double tFactor;
	protected double hx;
	
	public EmissionFunction(NonlinearProblem p, Grid grid) {
		init(p);
		this.hx = grid.getXStep();
	}
	
	public EmissionFunction(double tFactor, double hx) {
		this.tFactor = tFactor;
		this.hx = hx;
	}
	
	public void init(NonlinearProblem p) {
		tFactor = p.maximumHeating()/( (double)p.getTestTemperature().getValue() );
	}
	
	public double power(double t) {
		return 0.25/tFactor*fastPowLoop(1.0 + t*tFactor, 4);
	}
	
	public double radiance(double t) {
		return power(t)/Math.PI;
	}
	
	public double functionRelative(double t) {
		return 0.25/tFactor*(fastPowLoop(1.0 + t*tFactor, 4) - 1.0);
	}
	
	public double firstDerivative(double[] U, int uIndex) {
		return (power(U[uIndex + 1]) - power(U[uIndex - 1]))/(2.0*hx); 
	}
	
	public double firstDerivative_1(double t) {
		return fastPowLoop(1.0 + t*tFactor, 3);
	}
	
	public double secondDerivative(double[] U, int uIndex) {
		return (power(U[uIndex + 1]) - 2.0*power(U[uIndex]) + power(U[uIndex - 1]))/(hx*hx); 
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
	
	@Override
	public String toString() {
		return "[" +getClass().getSimpleName() + ": Rel. heating = " + tFactor + "]"; 
	}
	
}