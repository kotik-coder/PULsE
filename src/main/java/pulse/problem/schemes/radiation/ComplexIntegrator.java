package pulse.problem.schemes.radiation;

public class ComplexIntegrator extends Integrator {

	private double min;
	private double max;
	private double rMax;
	private double rMin;
	
	private double[] U;
	private double tau0;
	private double hx;

	private double stepSize;
	
	private final static int DEFAULT_PRECISION = 16;
	
	private final static int A_INDEX = 0;
	private final static int B_INDEX = 1;
	private final static int T_INDEX = 2;
	
	private double CUTOFF = 3.5;
	
	private static ExponentialFunctionIntegrator expIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	private EmissionFunction emissionFunction;
	
	public ComplexIntegrator(EmissionFunction function, double tau0, double hx) {
		super(Double.POSITIVE_INFINITY, 0, DEFAULT_PRECISION);
		this.tau0 = tau0;
		this.hx = hx;
		emissionFunction = function;
	}
	
	public void switchBounds() {
		double tempMin = min;
		this.min = max;
		this.max = tempMin;
	}
	
	public void setUpperBound(double value) {
		this.max = value;
	}
	
	public void setLowerBound(double value) {
		this.min = value;
	}
	
	public void setRange(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	public void setTemperatureArray(double[] U) {
		this.U = U;
	}
	
	public double[] getTemperatureArray() {
		return U;
	}
	
	/*
	 * integral = int_0^tau_0{J*(t) E_2(a + b t) dt}
	 * A , B as params
	 */
	
	@Override
	public double integrate(int order, double... params) {
		if(max - min < 1E-10)
			return 0;
		
		rMax = params[B_INDEX] < 0 ? max : Math.min( (CUTOFF - params[A_INDEX])/params[B_INDEX], max );
		rMin = params[B_INDEX] < 0 ? Math.max( (CUTOFF - params[A_INDEX])/params[B_INDEX], min ) : min;
		
		stepSize = (rMax - rMin)/segmentPartitions;
		
		return integrateSimpson(order, params);
		
	}
	
	public double integrateMidpoint(int order, double... params) {	
		double integral = 0;
		
		for(int i = 0; i < segmentPartitions; i++) 
			integral += integrand(order, params[A_INDEX], params[B_INDEX], rMin + (i + 0.5)*stepSize);
		
		return integral*stepSize;
	}
	
	/**********************************************************************
	* Integrate f from a to b using Simpson's rule.
	* Source: https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
	**********************************************************************/
	
	public double integrateSimpson(int order, double... params) {
	   double fa = integrand(order, params[0], params[1], rMin);
	   double fb = rMax < tau0 ? 
			   	   integrand(order, params[0], params[1], rMax) :
		   		   integrandNoInterpolation(order, params[0], params[1], rMax);
	  
	   // 1/3 terms
	   double sum = (fa + fb);
	  
	   // 4/3 terms
	   for (int i = 1; i < segmentPartitions; i += 2) 
		   sum += 4.0 * integrand( order, params[0], params[1], rMin + stepSize * i);

	   // 2/3 terms
	   for (int i = 2; i < segmentPartitions; i += 2) 
	      sum += 2.0 * integrand( order, params[0], params[1], rMin + stepSize * i);

	   return sum * stepSize/3.0;
	}

	/*
	 * a - params[0]
	 * b - params[1]
	 * t - params[2]
	 */
	
	@Override
	public double integrand(int order, double... params) {
		double tdim = params[T_INDEX]/tau0;
		
		int floor = (int) ( tdim/hx ); //floor index
		double alpha = tdim/hx - floor;
		
		return emissionFunction.source( (1.0 - alpha)*U[floor] + alpha*U[floor+1] )  
			    * expIntegrator.integralAt( params[A_INDEX] + params[B_INDEX]*params[T_INDEX], order );
	}
	
	public double integrandNoInterpolation(int order, double... params) {
		double tdim = params[T_INDEX]/tau0;
		int floor = (int) ( tdim/hx ); //floor index
		
		return emissionFunction.source( U[floor] )  
			    * expIntegrator.integralAt( params[A_INDEX] + params[B_INDEX]*params[T_INDEX], order );
	}

	public double getLowerBound() {
		return min;
	}

	public double getUpperBound() {
		return max;
	}
	
}