package pulse.problem.schemes.radiation;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import pulse.problem.schemes.radiation.ExponentialFunctionIntegrator;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.problem.statements.Pulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class RTESolverNonlinear {

	private double hx;
	private double emissivity;
	private double tau0;
	
	private ComplexIntegrator complexIntegrator;
	private static ExponentialFunctionIntegrator simpleIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	
	private final static int INT_PRECISION = 32;

	private double r1, r2;
	
	private EmissionFunction emissionFunction;
	
	public RTESolverNonlinear(AbsorbingEmittingProblem p, double hx) {
		tau0 = (double)p.getOpticalThickness().getValue();
		emissivity = (double)p.getEmissivity().getValue();
		double t0 = (double)p.getInitialTemperature().getValue();		
		this.hx = hx;
		emissionFunction = new EmissionFunction(p.maximumHeating()/t0, hx);
		complexIntegrator = new ComplexIntegrator(emissionFunction, tau0, hx);
	}
	
	/*
	 * F*(0) = R_1 - 2R_2 E_3(\tau_0) + ...
	 */

	
	public double fluxFront(double U[]) {		
		return r1 - 2.0*r2*simpleIntegrator.integralAt(tau0, 3) - 2.0*integrateSecondOrder(U, 0.0, 1.0);
	}

	
	/*
	 * F*(1) = -R_2 + 2R_1 E_3(\tau_0) + 2 int 
	 */
	
	public double fluxRear(double[] U) {
		return -r2 + 2.0*r1*simpleIntegrator.integralAt(tau0, 3) + 2.0*integrateSecondOrder(U, tau0, -1.0);
	}
	
	public double flux(double U[], int uIndex) {
		double t = hx*uIndex*tau0;
			
		complexIntegrator.setExpIntPrecision(INT_PRECISION);
		
		complexIntegrator.setRange(0, t);
		double I_1 = complexIntegrator.integrate(2, t, -1.0);
		
		complexIntegrator.setRange(t, tau0);
		double I_2 = complexIntegrator.integrate(2, -t, 1.0);
		
		double result = r1*simpleIntegrator.integralAt(t, 3) 
					    - r2*simpleIntegrator.integralAt(tau0 - t, 3)
						+ I_1 - I_2;
		
		return result*2.0;	
	}
	
	public double fluxRosseland(double U[], int uIndex) {
		return (-4.0/3.0)/tau0*emissionFunction.sourceFirstDerivative(U, uIndex);
	}
	
	public double fluxDerivative(double[] U, int uIndex) {
		/*if(tau0 > 10) 
			return fluxDerivativeRosseland(U, uIndex);
		else*/
			return fluxDerivativeDiscrete(U, uIndex);
	}
	
	public double fluxDerivativeDiscrete(double[] U, int uIndex) {
		double h = 2.0*hx*tau0;
		return - ( flux(U, uIndex + 1) - flux(U, uIndex - 1) )/h;
	}
	
	/*
	 * Rosseland approximation for tau0 >> 1
	 */
	
	public double fluxDerivativeRosseland(double[] U, int uIndex) {
		return -(4.0/3.0)/Math.pow(tau0, 2)*emissionFunction.sourceSecondDerivative(U, uIndex);
	}
	
	/*
	 * Black-body approximation for tau0 << 1
	 */
	
	public double fluxDerivativeThin(double[] U, int uIndex) {
		return 2.0*(emissionFunction.source(U[0]) 
					+ emissionFunction.source(U[U.length - 1]) 
					- 2.0*emissionFunction.source(U[uIndex]) ); 
	}
	
	/*
	 * -dF/d\tau
	 *
	 * = 2 R_1 E_2(y \tau_0) + 2 R_2 E_2( (1 - y) \tau_0 ) - \pi J*(y 'tau_0) 
	 *
	 */
	
	public double fluxDerivativeAnalytical(double U[], int uIndex) {
		double t = hx*uIndex*tau0;
				
		return 2.0*r1*simpleIntegrator.integralAt(t, 2) + 2.0*r2*simpleIntegrator.integralAt(tau0 - t, 2)
			   - 4.0*emissionFunction.source(U[uIndex]) + 2.*integrateFirstOrder(U, t);		
	}
	
	/*
	 * Radiosities of front and rear surfaces respectively in the assumption of diffuse and opaque boundaries
	 */
	
	public void radiosities(double[] U) {
		complexIntegrator.setTemperatureArray(U);
		double _b = b();
		double sq = 1.0 -_b*_b;
		r1 = ( a1(U) + _b*a2(U) )/sq;
		r2 = ( a2(U) + _b*a1(U) )/sq;
	}
	
	/*
	 * Coefficient b
	 */
	
	private double b() {
		return 2.0*(1.0 - emissivity)*simpleIntegrator.e_SwameeOhija(3, tau0);
	}
	
	/*
	 * Coefficient a1
	 *
	 * a1 = \varepsilon*J*(0) + 
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a1(double[] U) {
		return emissivity*emissionFunction.source(U[0]) +  2.*(1.0 - emissivity)*integrateSecondOrder(U, 0.0, 1.0);
	}
	
	/*
	 * Coefficient a2
     *
	 * a2 = \varepsilon*J*(0) + ...
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a2(double[] U) {
		return emissivity*emissionFunction.source(U[U.length - 1]) + 2.*(1.0 - emissivity)*integrateSecondOrder(U, tau0, -1.0);
	}
	
	/*
	 * Source function J*(t) = (1 + @U[i]*tFactor)^4,
	 * where i = t/hx
	 * tFactor = (tMax/t0)
	 */
	
	private double integrateSecondOrder(double[] U, double a, double b) {
		complexIntegrator.setRange(0, tau0);
		complexIntegrator.setExpIntPrecision(INT_PRECISION*2);
		return complexIntegrator.integrate(2, a, b);
	}
	
	private double integrateFirstOrder(double[] U, double y) {
		double integral = 0;
		
		complexIntegrator.setExpIntPrecision(INT_PRECISION);
		complexIntegrator.setRange(0, y);
		
		integral += complexIntegrator.integrate(1, y, -1);

		complexIntegrator.switchBounds();
		complexIntegrator.setUpperBound(tau0);

		integral += complexIntegrator.integrate(1, -y, 1);

		return integral;
		
	}
	
	public static void main(String[] args) {
		final String dest = "/test/TestSolution.dat"; 
		URI f = null;
		
		try {
			f = RTESolverNonlinear.class.getResource(dest).toURI();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		double[] u = null;
		
		try {
		    u = (new ArrayList<>(Files.readAllLines(Paths.get(f))) ).
		    		stream().mapToDouble(s -> Double.parseDouble(s)).toArray();
		}
		catch (IOException e) {
		    e.printStackTrace();
		}
		
		double[] uIsothermic = new double[u.length];
		
		for(int i = 0; i < uIsothermic.length; i++) {
			uIsothermic[i] = 0.335;
		}
		
		//System.out.println("Loaded test array of " + u.length + " elements");
		//System.out.println("Check: first element = " + String.format("%3.2f", u[0]) + " ; last element = " + String.format("%3.2f", u[u.length - 1]));
		
		double hx = 1.0 / (u.length - 1);
		//System.out.println("hx = " + String.format("%1.5f", hx));
		
		var problem = new AbsorbingEmittingProblem(); 
		final double T0 = 800; 
		problem.setInitialTemperature(NumericProperty.derive(NumericPropertyKeyword.TEST_TEMPERATURE, T0));
		
		double testEmissivity = 1.0;
		problem.setEmissivity(NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, testEmissivity));
		double tau0 = 1000;
		problem.setOpticalThickness(NumericProperty.derive(NumericPropertyKeyword.OPTICAL_THICKNESS, tau0));
	
		System.out.println("...");
		System.out.println("Created sample heat problem with the following parameters:");
		System.out.println(problem.getOpticalThickness());
		System.out.println(problem.getPlanckNumber());
		System.out.println(problem.getInitialTemperature());
		System.out.println(problem.getEmissivity());
	
		double rho = 2200;
		double cp = 1000;
		
		problem.setSpecificHeat(NumericProperty.derive(NumericPropertyKeyword.SPECIFIC_HEAT, cp ));
		problem.setDensity(NumericProperty.derive(NumericPropertyKeyword.SPECIFIC_HEAT, rho ));
		/*
		System.out.println(problem.getSpecificHeat());
		System.out.println(problem.getDensity());
		
		System.out.println("...");
		*/
		Pulse p = new Pulse();
		problem.setPulse(p);
		/*
		System.out.println("Created pulse: " + p.toString());
		
		System.out.println("...");
		*/
		var solver = new RTESolverNonlinear(problem, hx);

		solver.radiosities(uIsothermic);
		
		System.out.println("Isothermic radiosities: " + String.format("%1.2e", solver.r1) + " ; " + String.format("%1.2e", solver.r2));
		
		System.out.printf("#1 Isothermic flux derivatives: [1] = %3.5f [15] = %3.5f [39] = %3.5f [60] = %3.5f [77] = %3.5f", 
		solver.fluxDerivativeAnalytical(uIsothermic, 1),
		solver.fluxDerivativeAnalytical(uIsothermic, 15),
		solver.fluxDerivativeAnalytical(uIsothermic, 39),
		solver.fluxDerivativeAnalytical(uIsothermic, 60),
		solver.fluxDerivativeAnalytical(uIsothermic, 77));
		
		System.out.printf("%n#2 Isothermic flux derivatives: [1] = %3.5f [15] = %3.5f [39] = %3.5f [60] = %3.5f [77] = %3.5f", 
		solver.fluxDerivativeDiscrete(uIsothermic, 1),
		solver.fluxDerivativeDiscrete(uIsothermic, 15),
		solver.fluxDerivativeDiscrete(uIsothermic, 39),
		solver.fluxDerivativeDiscrete(uIsothermic, 60),
		solver.fluxDerivativeDiscrete(uIsothermic, 77));
		
		System.out.printf("%n#3 Isothermic flux derivatives: [1] = %3.5f [15] = %3.5f [39] = %3.5f [60] = %3.5f [77] = %3.5f", 
		solver.fluxDerivativeRosseland(uIsothermic, 1),
		solver.fluxDerivativeRosseland(uIsothermic, 15),
		solver.fluxDerivativeRosseland(uIsothermic, 39),
		solver.fluxDerivativeRosseland(uIsothermic, 60),
		solver.fluxDerivativeRosseland(uIsothermic, 77));
		
		System.out.printf("%nIsothermic front-surface flux: %1.4e and rear-surface flux: %1.4e", 
		solver.fluxFront(uIsothermic), solver.fluxRear(uIsothermic));
		System.out.printf("%n#2 Isothermic front-surface flux: %1.4e and rear-surface flux: %1.4e", 
		solver.flux(uIsothermic, 0), solver.flux(uIsothermic, u.length - 1));		
		
		System.out.println("...");
		
		solver.radiosities(u);
		System.out.printf("%nNon-isothermic radiosities: %1.2e ; %1.2e", solver.r1, solver.r2);
		
		System.out.printf("%n#1 Non-isothermic flux derivatives: [1] = %3.2f [39] = %3.2f [78] = %3.2f", 
		solver.fluxDerivativeAnalytical(u, 1),
		solver.fluxDerivativeAnalytical(u, 39),
		solver.fluxDerivativeAnalytical(u, 78));
		
		System.out.printf("%n#2 Non-isothermic flux derivatives: [1] = %3.2f [39] = %3.2f [78] = %3.2f", 
		solver.fluxDerivativeDiscrete(u, 1),
		solver.fluxDerivativeDiscrete(u, 39),
		solver.fluxDerivativeDiscrete(u, 78));
		
		System.out.printf("%n#3 Non-isothermic flux derivatives: [1] = %3.2f [39] = %3.2f [78] = %3.2f", 
		solver.fluxDerivativeRosseland(u, 1),
		solver.fluxDerivativeRosseland(u, 39),
		solver.fluxDerivativeRosseland(u, 78));

		System.out.printf("%n#1 Non-isothermic front-surface flux: %1.2e and rear-surface flux: %1.2e", 
		solver.fluxFront(u), solver.fluxRear(u));
		
		System.out.printf("%n#2 Non-isothermic front-surface flux: %1.2e and rear-surface flux: %1.2e", 
		solver.flux(u,0), solver.flux(u,79));
		
		System.out.printf("%n#2 Non-isothermic flux[15]: %1.2e and [39]: %1.2e", 
		solver.flux(u,15), solver.flux(u,39));
		
		System.out.printf("%n#3 Non-isothermic flux[15]: %1.2e and [39]: %1.2e", 
		solver.fluxRosseland(u,15), solver.fluxRosseland(u,39));
		
	}
			
}