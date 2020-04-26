package pulse.problem.schemes.radiation;

public class EmissionFunction {

	private double tFactor;
	private double hx;
	
	public EmissionFunction(double tFactor, double hx) {
		this.tFactor = tFactor;
		this.hx = hx;
	}
	
	public double source(double t) {
		return 0.25/tFactor*Math.pow(1.0 + t*tFactor, 4);
	}
	
	public double sourceFirstDerivative(double[] U, int uIndex) {
		return (source(U[uIndex + 1]) - source(U[uIndex - 1]))/(2.0*hx); 
	}
	
	public double sourceSecondDerivative(double[] U, int uIndex) {
		return (source(U[uIndex + 1]) - 2.0*source(U[uIndex]) + source(U[uIndex - 1]))/(hx*hx); 
	}
	
}
