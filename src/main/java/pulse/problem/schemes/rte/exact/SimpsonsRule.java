package pulse.problem.schemes.rte.exact;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.IntStream;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.schemes.rte.Integrator;

/**
 * A class with simple quadrature methods for evaluating the definite integral $\int_a^b{f(x) E_n (\alpha + \beta x) dx}$
 *
 */

public class SimpsonsRule extends Integrator {

	protected double min;
	protected double max;
	protected double rMax;
	protected double rMin;
	
	protected double[] U;
	protected double tau0;
	protected double hx;

	private double stepSize;
		
	protected final static int A_INDEX = 0;
	protected final static int B_INDEX = 1;
	protected final static int T_INDEX = 2;
	
	protected static ExponentialFunctionIntegrator expIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	protected EmissionFunction emissionFunction;

	private final static double DEFAULT_CUTOFF = 6.5;
	private final static int DEFAULT_PRECISION = 64;
		
	public SimpsonsRule() {
		super(DEFAULT_CUTOFF, 0, DEFAULT_PRECISION);
	}
	
	public SimpsonsRule(double cutoff, int segments) {
		super(cutoff, 0, segments);
	}
	
	public void setOpticalThickness(double tau0) {
		this.tau0 = tau0;
	}
	
	public void setXStep(double hx) {
		this.hx = hx;
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
	
	public void adjustRange(double alpha, double beta) {
		double bound = (cutoff - alpha)/beta;
		
		double a = 0.5 - beta/2;
		double b = 1. - a;
		
		rMax = max * a + Math.min( bound, max ) * b;
		rMin = Math.max( bound, min ) * a + min * b;
	}
	
	@Override
	public double integrate(int order, double... params) {
		adjustRange(params[A_INDEX], params[B_INDEX]);				
		stepSize = (rMax - rMin)/integrationSegments;
		return integrateSimpson(order, params);
	}
	
	public double integrateMidpoint(int order, double... params) {	
		double integral = 0;
		
		for(int i = 0; i < integrationSegments; i++) 
			integral += integrand(order, params[A_INDEX], params[B_INDEX], rMin + (i + 0.5)*stepSize);
		
		return integral*stepSize;
	}
	
	/**********************************************************************
	* Integrate f from a to b using Simpson's rule.
	* Source: https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
	**********************************************************************/
	
	public double integrateSimpson(int order, double... params) {
	   double fa = integrand(order, params[0], params[1], rMin);
	   double fb = integrandNoInterpolation(order, params[0], params[1], rMax);
	  
	   // 1/3 terms
	   double sum = (fa + fb);
	  
	   double x = 0;
	   double y = 0;
	   
	   // 4/3 terms
	   for (int i = 1; i < integrationSegments; i += 2) { 
		   x += integrand( order, params[0], params[1], rMin + stepSize * i);
	   }
		   
	   // 2/3 terms
	   for (int i = 2; i < integrationSegments; i += 2) 
	      y += integrand( order, params[0], params[1], rMin + stepSize * i);

	   sum += x*4.0 + y*2.0;
	   
	   return sum * stepSize/3.0;
	}
	
	public double integrateBoole(int order, double... params) {
		   double fa = integrand(order, params[0], params[1], rMin);
		   double fb = integrandNoInterpolation(order, params[0], params[1], rMax);
			   		   
		   // 1/90 terms
		   double sum = (fa + fb);
		   
		   //1/32 terms
		   sum += 32.*IntStream.range(0, integrationSegments ).mapToDouble(i -> 
		   				integrand( order, params[0], params[1], rMin + stepSize*(i + 0.25) ) +
				   	    integrand( order, params[0], params[1], rMin + stepSize*(i + 0.75)) ) .sum();
		   
		   //1/12 terms
		   sum += 12.*IntStream.range(0, integrationSegments )
				   .mapToDouble(i -> integrand( order, params[0], params[1], 
				   rMin + stepSize*(i + 0.5) ) ).sum();

		   //1/14 terms
		   sum += 14.*IntStream.range(1, integrationSegments )
				   .mapToDouble(i -> integrand( order, params[0], params[1], 
				   rMin + stepSize*i ) ).sum();
		   
		   return sum * stepSize/90.;
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
		
		return emissionFunction.power( (1.0 - alpha)*U[floor] + alpha*U[floor+1] )  
			    * expIntegrator.integralAt( params[A_INDEX] + params[B_INDEX]*params[T_INDEX], order );
	}
	
	public double integrandNoInterpolation(int order, double... params) {
		double tdim = params[T_INDEX]/tau0;
		int floor = (int) ( tdim/hx ); //floor index
		
		return emissionFunction.power( U[floor] )  
			    * expIntegrator.integralAt( params[A_INDEX] + params[B_INDEX]*params[T_INDEX], order );
	}

	public double getLowerBound() {
		return min;
	}

	public double getUpperBound() {
		return max;
	}

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(EmissionFunction emissionFunction) {
		this.emissionFunction = emissionFunction;
	}
	
	public static void main(String[] args) { 
		
		var integrator = new SimpsonsRule();
		var cIntegrator = new ChandrasekharsQuadrature();

		integrator.setRange(0.0, 0.123);
		cIntegrator.setRange(0.0, 0.123);
		
		double alpha = 0.2;
		double beta = -1.0;
		
		File f = null;
		try {
			f = new File(SimpsonsRule.class.getResource("/test/TestSolution.dat").toURI());
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		var data = new ArrayList<Double>();
		
		try (Scanner scanner = new Scanner(f)) {
		    while (scanner.hasNextLine()) {
		        data.add(Double.parseDouble(scanner.nextLine()));
		    }
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double[] U = data.stream().mapToDouble(x -> x).toArray();
		
		integrator.setTemperatureArray(U);
		cIntegrator.setTemperatureArray(U);
		
		double hx = 1./(U.length-1);
		double tFactor = 0.1/800;
		
		integrator.setXStep(hx);
		cIntegrator.setXStep(hx);
		
		var ef = new EmissionFunction(tFactor, hx);
		integrator.setEmissionFunction(ef);
		integrator.setOpticalThickness(integrator.max);
		cIntegrator.setEmissionFunction(ef);
		cIntegrator.setOpticalThickness(integrator.max);
		
		long time = -System.currentTimeMillis();
		
		double integral = 0;
		
		for(int i = 0; i < 10000; i++)
			integral = cIntegrator.integrate(2, alpha, beta);
		
		time += System.currentTimeMillis();
		
		long time2 = -System.currentTimeMillis();
		
		double sum = 0;
		
		for(int i = 0; i < 10000; i++)
			sum = integrator.integrate(2, alpha, beta);
		
		time2 += System.currentTimeMillis();
		
		System.out.printf("%nInt (quadrature): %2.5f. Time taken: %5d", integral, time);
		System.out.printf("%nInt (numerical): %2.5f. Time taken: %5d", sum, time2);
			
	}

	@Override
	public String getPrefix() {
		return "Numerical Quadrature";
	}
	
}