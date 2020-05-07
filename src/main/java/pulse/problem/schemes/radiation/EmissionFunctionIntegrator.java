package pulse.problem.schemes.radiation;

import pulse.problem.schemes.Grid;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class EmissionFunctionIntegrator extends Integrator {

	protected double[] U;
	
	private double stepSize;
	protected double hx;
	
	private double min;
	private double max;
	
	protected EmissionFunction emissionFunction;
	
	public EmissionFunctionIntegrator(EmissionFunction function, Grid grid) {
		this(function, grid.getXStep());
	}
	
	public EmissionFunctionIntegrator(EmissionFunction function, double hx) {
		super(Double.POSITIVE_INFINITY, 0, (int)NumericProperty.def(NumericPropertyKeyword.INTEGRATION_SEGMENTS).getValue());
		emissionFunction = function;
		this.hx = hx;
	}
	
	protected EmissionFunctionIntegrator() {
		super(Double.POSITIVE_INFINITY, 0, (int)NumericProperty.def(NumericPropertyKeyword.INTEGRATION_SEGMENTS).getValue());
	}
	
	public void setTemperatureArray(double[] U) {
		this.U = U;
	}
	
	public double[] getTemperatureArray() {
		return U;
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
	
	@Override
	public double integrate(int order, double... params) {
		stepSize = Math.abs(max - min)/integrationSegments;
		return integrateSimpson(order, params);
	}

	@Override
	public double integrand(int order, double... params) {
		int floor = (int) ( params[0]/hx ); //floor index
		double alpha = params[0]/hx - floor;
		return emissionFunction.function( (1.0 - alpha)*U[floor] + alpha*U[floor+1] );
	}
	
	public double integrateMidpoint(int order, double... params) {	
		double integral = 0;
		
		for(int i = 0; i < integrationSegments; i++) 
			integral += integrand(order, min + (i + 0.5)*stepSize);
		
		return integral*stepSize;
	}
	
	/**********************************************************************
	* Integrate f from a to b using Simpson's rule.
	* Source: https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
	**********************************************************************/
	
	public double integrateSimpson(int order, double... params) {
	   double fa = integrand(order, min);
	   double fb = integrand(order, max);	  
	   // 1/3 terms
	   double sum = (fa + fb);
	  
	   // 4/3 terms
	   for (int i = 1; i < integrationSegments; i += 2) 
		   sum += 4.0 * integrand( order, min + stepSize * i);

	   // 2/3 terms
	   for (int i = 2; i < integrationSegments; i += 2) 
	      sum += 2.0 * integrand( order, min + stepSize * i);

	   return sum * stepSize/3.0;
	}

	public double getGridStep() {
		return hx;
	}

	public void setGridStep(double hx) {
		this.hx = hx;
	}

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(EmissionFunction emissionFunction) {
		this.emissionFunction = emissionFunction;
	}

}
