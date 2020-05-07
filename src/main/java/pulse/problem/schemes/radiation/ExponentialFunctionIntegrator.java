package pulse.problem.schemes.radiation;

import org.apache.commons.math3.util.CombinatoricsUtils;

public class ExponentialFunctionIntegrator extends Integrator {

	private final static int EXP_INT_PRECISION = 256;
	private final static int NUM_PARTITIONS = 10000;
	private final static double CUTOFF = 9.2; //corresponds to a precision of about 10^{-5}
	private final static int APPROXIMATION_TERMS = 5;

	private static ExponentialFunctionIntegrator expIntegral = new ExponentialFunctionIntegrator();
	
	//private final static double LOWER_CUTOFF = 0.1;
	
	private IntegrationMethod method; 
	
	public ExponentialFunctionIntegrator() { 
		super(CUTOFF, NUM_PARTITIONS, EXP_INT_PRECISION);
		setMethod(IntegrationMethod.MIDPOINT);
		init();
	}
	
	/*
	 * Calculates the integral using mean rectangles
	 */
	
	@Override
	public double integrate(int order, double... params) {	
		switch(method) {
			case MIDPOINT : return integrateMidpoint(order, params[0], 0, 1); 
			case SIMPSON : return integrateSimpson(order, params[0], 0, 1);
			default : throw new IllegalStateException("Integration method undefined: " + method);
		}
	}
	
	public double e1_Approximation(double t) {
		final double gamma = 0.5772156649;
		double result = -gamma - MathUtils.fastLog(t);
		
		for(int i = 1; i < APPROXIMATION_TERMS; i++) 
			result -= MathUtils.fastPowLoop(-t, i)/(i*CombinatoricsUtils.factorial(i));
		
		return result;
		
	}
	
	public double e_Approximation(int order, double t) {
		double[] eApprox = new double[order];
		
		eApprox[0] = e1_Approximation(t);
		
		for(int i = 1; i < order; i++) 
			eApprox[i] = (MathUtils.fastExp(-t) - t*eApprox[i-1])/i;
	
		return eApprox[order-1];
		
	}
	
	public double e_Ramanujan(int order, double t) {
		double[] eApprox = new double[order];
		
		eApprox[0] = e1_Ramanujan(t);
		
		for(int i = 1; i < order; i++) 
			eApprox[i] = (MathUtils.fastExp(-t) - t*eApprox[i-1])/i;
	
		return eApprox[order-1];
		
	}
	
	public double e1_Ramanujan(double t) {
		final double gamma = 0.5772156649;
		double result = -gamma - MathUtils.fastLog(t);
		double sum1 = 0;
		double sum2 = 0;
		
		for(int i = 1; i < APPROXIMATION_TERMS; i++) {
			sum2 = 0;
			for(int k = 0, kindex = (int)( (i-1)/2.0 ) + 1; k < kindex; k++) 
				sum2 += 1.0/(2.0*k + 1.0); 
			
			sum1 += sum2*MathUtils.fastPowSgn(i-1)*MathUtils.fastPowLoop(t, i)/
					(MathUtils.fastPowInt(2, i-1)*CombinatoricsUtils.factorial(i));
		}
			
		return result + sum1*MathUtils.fastExp(t/2.0);
		
	}
	
	public double e1_SwameeOhija(double t) {
		double a = MathUtils.fastLog( (0.56146/t + 0.65)*(1 + t) );
		double b = MathUtils.fastPowLoop(t, 4)*MathUtils.fastExp(t*7.7)*MathUtils.fastPowGeneral(2 + t, 3.7);
		
		return MathUtils.fastPowGeneral( (MathUtils.fastPowGeneral(a, -7.7) + b), -0.13);
	}
	
	public double e2_SwameeOhija(double t) {
		return (MathUtils.fastExp(-t) - t*this.e1_SwameeOhija(t));
	}
	
	public double e3_SwameeOhija(double t) {
		return (MathUtils.fastExp(-t) - t*this.e_SwameeOhija(2, t))/2.0;
	}
	
	public double e_SwameeOhija(int order, double t) {
		if(order == 1) 
			return e1_SwameeOhija(t);
		else
			return (MathUtils.fastExp(-t) - t*this.e_SwameeOhija(order - 1, t))/(order - 1);
	}
	
	public double integrateMidpoint(int order, double t, double a, double b) {	
		double h = (b - a) / EXP_INT_PRECISION;     // step size
		double sum = 0;
		for(int i = 0; i < EXP_INT_PRECISION; i++) 
			sum += integrand( order, (i + 0.5)*h, t)*h;
		return sum;
	}
	
	/**********************************************************************
	* Integrate f from a to b using Simpson's rule.
	* Source: https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
	**********************************************************************/
	
	public double integrateSimpson(int order, double t, double a, double b) {
	   double h = (b - a) / EXP_INT_PRECISION;     // step size
	 
	   final double EPS = 1E-5;
		
	   double fa = t < EPS ? integralAtZero(order) : integrand( order, a, t);
	   double fb = t < EPS ? integralAtZero(order) : integrand( order, b, t);
	      
	   // 1/3 terms
	   double sum = (fa + fb);

	   double x = 0;
	   double y = 0;
	   
	   // 4/3 terms
	   for (int i = 1; i < EXP_INT_PRECISION; i += 2) 
	      x += integrand( order, a + h * i, t);
	   
	   //2/3
	   for (int i = 2; i < EXP_INT_PRECISION; i += 2) 
		  y += integrand( order, a + h * (i+1), t);

	   sum += x*4.0 + y*2.0;
	   	   
	   return sum * h/3.0;
	}
	
	public double integralAtZero(int order) {
		return 1.0/(order - 1);
	}
	/*
	@Override
	public double integralAt(double t, int n) {
		if(t < 1E-10) 
			return integralAtZero(n);
		else if(t < LOWER_CUTOFF) //use approximation
			return this.e_Approximation(n, t);
		else //use table
			return super.integralAt(t, n);
	}
	*/
	@Override
	public double integrand(int order, double... params) {
		return MathUtils.fastPowLoop(params[0], order - 2)*Math.exp(-params[1]/params[0]);
	}
	
	public IntegrationMethod getMethod() {
		return method;
	}

	public void setMethod(IntegrationMethod method) {
		this.method = method;
	}

	public enum IntegrationMethod {
		SIMPSON, MIDPOINT;
	}

	public static ExponentialFunctionIntegrator getDefaultIntegrator() {
		return expIntegral;
	}
	
	public static void main(String[] args) {
		var e = new ExponentialFunctionIntegrator();
		System.out.println(e.e1_SwameeOhija(0.1) + " ; " + e.integralAt(0.1, 1) + " ; " + e.integrate(1, 0.1) + " ; " + e.e1_Approximation(0.1) + " ; " + e.e1_Ramanujan(0.1));
		System.out.println(e.e3_SwameeOhija(0.25) + " ; " + e.integrate(3, 0.25) + " ; " + e.e_Approximation(3, 0.25));	
		System.out.println(e.integralAt(0.5, 3) + " ; " + e.integrate(3, 0.5) + " ; " + e.e_Approximation(3, 0.5));	
	}
	
}