package pulse.problem.schemes.radiation;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.radiation.ExponentialFunctionIntegrator;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.util.Group;

public class RadiativeTransfer extends Group {

	private int N;
	private double hx;
	private double emissivity;
	private double tau0;
	
	private EmissionFunction emissionFunction;
	private EmissionFunctionIntegrator emissionIntegrator;
	private ComplexIntegrator complexIntegrator;
	
	private static ExponentialFunctionIntegrator simpleIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	
	private double F[], FP[];
	private double r1, r2;
	
	public RadiativeTransfer(Grid grid) {
		complexIntegrator = new ComplexIntegrator();
		this.hx = grid.getXStep();	
		this.N = (int)grid.getGridDensity().getValue();	
		complexIntegrator.setXStep(hx);
		complexIntegrator.setParent(this);
	}
	
	public void init(AbsorbingEmittingProblem p, Grid grid) {
		this.hx = grid.getXStep();	
		this.N = (int)grid.getGridDensity().getValue();	
		F = new double[N+1];	
		FP = new double[N+1];
		
		tau0 = (double)p.getOpticalThickness().getValue();
		
		emissivity = (double)p.getEmissivity();
		emissionFunction = new EmissionFunction(p.maximumHeating()/( (double)p.getTestTemperature().getValue() ), hx);
		emissionIntegrator = new EmissionFunctionIntegrator(emissionFunction, hx);
		
		complexIntegrator.setXStep(hx);
		complexIntegrator.setEmissionFunction(emissionFunction);
		complexIntegrator.setOpticalThickness(tau0);
	}
	
	/*
	 * Assumes radiosities have already been calculated using radiosities()
	 * F*(1) = -R_2 + 2R_1 E_3(\tau_0) + 2 int 
	 */
	
	public double fluxRear(double[] U) {
		return -r2 + 2.0*r1*simpleIntegrator.integralAt(tau0, 3) + 2.0*integrateSecondOrder(U, tau0, -1.0);
	}
	
	public double getFlux(int index) {
		return F[index];
	}
	
	public double getStoredFlux(int index) {
		return FP[index];
	}
	
	public double fluxRearThin(double U[]) {
		emissionIntegrator.setRange(0, tau0);
		emissionIntegrator.setTemperatureArray(U);
		return emissionFunction.function(U[0])*(1.0 - 2*tau0) - emissionFunction.function(U[N]) 
				+ 2.0*emissionIntegrator.integrate(1);
	}
	
	public void boundaryFluxes(double[] U) {
		F[0] = fluxFront(U);
		F[N] = fluxRear(U);
	}
	
	/*
	 * Assumes radiosities have already been calculated using radiosities()
	 * F*(0) = R_1 - 2R_2 E_3(\tau_0) + ...
	 */
	
	public double fluxFront(double U[]) {		
		return r1 - 2.0*r2*simpleIntegrator.integralAt(tau0, 3) - 2.0*integrateSecondOrder(U, 0.0, 1.0);
	}
	
	public double fluxFrontThin(double U[]) {
		emissionIntegrator.setRange(0, tau0);
		emissionIntegrator.setTemperatureArray(U);
		return emissionFunction.function(U[0]) - emissionFunction.function(U[N])*(1.0 - 2*tau0) 
				- 2.0*emissionIntegrator.integrate(1);
	} 

	public void storeFluxes() {
		System.arraycopy(F, 0, FP, 0, N + 1); //store previous results 
	}
	
	public void fluxes(double U[]) {
		F[0] = fluxFront(U);
		for(int i = 1; i < N; i++)
			F[i] = flux(U, i);
		F[N] = fluxRear(U);
	}
	
	public double flux(double U[], int uIndex) {
		double t = hx*uIndex*tau0;
			
		complexIntegrator.setTemperatureArray(U);
		
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
		return (-4.0/3.0)/tau0*emissionFunction.firstDerivative(U, uIndex);
	}
		
	public double fluxDerivative(double[] U, int uIndex) {
		double h = 2.0*hx*tau0;
		return -(flux(U, uIndex+1) - flux(U, uIndex-1))/h;		
	}
	
	public double fluxMeanDerivative(int uIndex) {
		double f = ( F[uIndex - 1] - F[uIndex + 1] ) + ( FP[uIndex - 1] - FP[uIndex + 1] );
		return f/(4.0*hx*tau0);
	}
	
	public double fluxMeanDerivativeFront() {
		double f = ( F[0] - F[1] ) + ( FP[0] - FP[1] );
		return f/(2.0*hx*tau0);
	}
	
	public double fluxMeanDerivativeRear() {
		double f = ( F[N-1] - F[N] ) + ( FP[N-1] - FP[N] );
		return f/(2.0*hx*tau0);	}
	
	public double fluxDerivative(int uIndex) {
		return fluxDerivativeDiscrete(uIndex);		
	}
	
	public double fluxDerivativeFront() {
		return (F[0] - F[1])/(tau0*hx);
	}
	
	public double fluxDerivativeRear() {
		return (F[N-1] - F[N])/(tau0*hx);
	}
	
	public double fluxDerivativeDiscrete(int uIndex) {
		double h = 2.0*hx*tau0;
		return ( F[uIndex - 1] - F[uIndex + 1] )/h;
	}
	
	/*
	 * Rosseland approximation for tau0 >> 1
	 */
	
	public double fluxDerivativeRosseland(double[] U, int uIndex) {
		return 4.0/(3.0*Math.pow(tau0, 2))*emissionFunction.secondDerivative(U, uIndex);
	}
	
	/*
	 * Black-body approximation for tau0 << 1
	 */
	
	public double fluxDerivativeThin(double[] U, int uIndex) {
		return 2.0*(emissionFunction.function(U[0]) 
					+ emissionFunction.function(U[N]) 
					- 2.0*emissionFunction.function(U[uIndex]) ); 
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
			   - 4.0*emissionFunction.function(U[uIndex]) + 2.*integrateFirstOrder(U, t);		
	}
	
	/*
	 * Radiosities of front and rear surfaces respectively in the assumption of diffuse and opaque boundaries
	 */
	
	public void radiosities(double[] U) {
		double _b = b();
		double sq = 1.0 -_b*_b;
		r1 = ( a1(U) + _b*a2(U) )/sq;
		r2 = ( a2(U) + _b*a1(U) )/sq;
	}
	
	/*
	 * Coefficient b
	 */
	
	private double b() {
		return 2.0*(1.0 - emissivity)*simpleIntegrator.integralAt(tau0, 3);
	}
	
	/*
	 * Coefficient a1
	 *
	 * a1 = \varepsilon*J*(0) + 
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a1(double[] U) {
		return emissivity*emissionFunction.function(U[0]) +  2.*(1.0 - emissivity)*integrateSecondOrder(U, 0.0, 1.0);
	}
	
	/*
	 * Coefficient a2
     *
	 * a2 = \varepsilon*J*(0) + ...
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a2(double[] U) {
		return emissivity*emissionFunction.function(U[N]) + 2.*(1.0 - emissivity)*integrateSecondOrder(U, tau0, -1.0);
	}
	
	/*
	 * Source function J*(t) = (1 + @U[i]*tFactor)^4,
	 * where i = t/hx
	 * tFactor = (tMax/t0)
	 */
	
	private double integrateSecondOrder(double[] U, double a, double b) {
		complexIntegrator.setRange(0, tau0);
		complexIntegrator.setTemperatureArray(U);
		return complexIntegrator.integrate(2, a, b);
	}
	
	private double integrateFirstOrder(double[] U, double y) {
		double integral = 0;
		
		complexIntegrator.setTemperatureArray(U);
		
		complexIntegrator.setRange(0, y);
		integral += complexIntegrator.integrate(1, y, -1);

		complexIntegrator.setRange(y, tau0);
		integral += complexIntegrator.integrate(1, -y, 1);

		return integral;
		
	}
	
	public ComplexIntegrator getComplexIntegrator() {
		return complexIntegrator;
	}

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(EmissionFunction emissionFunction) {
		this.emissionFunction = emissionFunction;
	}
		
}