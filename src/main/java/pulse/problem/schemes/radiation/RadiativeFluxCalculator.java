package pulse.problem.schemes.radiation;

import java.util.List;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class RadiativeFluxCalculator extends PropertyHolder implements Reflexive {

	private static String selectedDescriptor;
	
	protected int N;
	protected double hx;
	private double emissivity, doubleReflectivity;
	protected double tau0;
	
	protected EmissionFunction emissionFunction;
	private EmissionFunctionIntegrator emissionIntegrator;
	protected SpecialIntegrator complexIntegrator;
	
	protected static ExponentialFunctionIntegrator simpleIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	
	protected double F[], FP[];
	protected double r1, r2;
	
	protected double HX_TAU0;
	
	private QuadratureType quadratureType = QuadratureType.CHANDRASEKHAR;
	
	public RadiativeFluxCalculator(Grid grid) {
		setQuadratureType(quadratureType);
		this.hx = grid.getXStep();	
		this.N = (int)grid.getGridDensity().getValue();	
		F = new double[N+1]; 
		FP = new double[N+1];
		emissionFunction = new EmissionFunction(0.0,0.0);
		complexIntegrator.setEmissionFunction(emissionFunction);
		emissionIntegrator = new EmissionFunctionIntegrator();
		emissionIntegrator.emissionFunction = emissionFunction;
	}
	
	public void init(AbsorbingEmittingProblem p, Grid grid) {
		this.hx = grid.getXStep();	
		this.N = (int)grid.getGridDensity().getValue();	
		
		F = new double[N+1];	
		FP = new double[N+1];
		
		emissivity = (double)p.getEmissivity();
		doubleReflectivity = 2.0*(1.0 - emissivity);
		
		emissionFunction.tFactor = p.maximumHeating()/( (double)p.getTestTemperature().getValue() );
		
		setGridStep(hx);

		tau0 = (double)p.getOpticalThickness().getValue();
		HX_TAU0 = hx*tau0;
		complexIntegrator.setOpticalThickness(tau0);
	}
	
	private void setGridStep(double hx) {
		emissionFunction.hx = hx;
		emissionIntegrator.hx = hx;
		complexIntegrator.hx = hx;
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
	
	public void storeFluxes() {
		System.arraycopy(F, 0, FP, 0, N + 1); //store previous results 
	}
	
	public void boundaryFluxes(double[] U) {
		complexIntegrator.U = U;
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
	
	public abstract void compute(double U[]);
	
	protected double flux(double U[], int uIndex) {
		double t = HX_TAU0*uIndex;
			
		complexIntegrator.setRange(0, t);
		double I_1 = complexIntegrator.integrate(2, t, -1.0);
		
		complexIntegrator.setRange(t, tau0);
		double I_2 = complexIntegrator.integrate(2, -t, 1.0);
		
		double result = r1*simpleIntegrator.integralAt(t, 3) 
					    - r2*simpleIntegrator.integralAt(tau0 - t, 3)
						+ I_1 - I_2;
		
		return result*2.0;	
	}
	
	/*
	 * Radiosities of front and rear surfaces respectively in the assumption of diffuse and opaque boundaries
	 */
	
	public void radiosities(double[] U) {
		complexIntegrator.U = U;
		double _b = b();
		double sq = 1.0 -_b*_b;
		r1 = ( a1(U) + _b*a2(U) )/sq;
		r2 = ( a2(U) + _b*a1(U) )/sq;
	}
	
	/*
	 * Coefficient b
	 */
	
	private double b() {
		return doubleReflectivity*simpleIntegrator.integralAt(tau0, 3);
	}
	
	/*
	 * Coefficient a1
	 *
	 * a1 = \varepsilon*J*(0) + 
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a1(double[] U) {
		return emissivity*emissionFunction.function(U[0]) +  doubleReflectivity*integrateSecondOrder(U, 0.0, 1.0);
	}
	
	/*
	 * Coefficient a2
     *
	 * a2 = \varepsilon*J*(0) + ...
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a2(double[] U) {
		return emissivity*emissionFunction.function(U[N]) + doubleReflectivity*integrateSecondOrder(U, tau0, -1.0);
	}
	
	/*
	 * Source function J*(t) = (1 + @U[i]*tFactor)^4,
	 * where i = t/hx
	 * tFactor = (tMax/t0)
	 */
	
	private double integrateSecondOrder(double[] U, double a, double b) {
		complexIntegrator.setRange(0, tau0);
		return complexIntegrator.integrate(2, a, b);
	}
		
	public SpecialIntegrator getQuadrature() {
		return complexIntegrator;
	}
	
	public void setQuadrature(SpecialIntegrator specialIntegrator) {
		this.complexIntegrator = specialIntegrator;
	}
	
	public QuadratureType getQuadratureType() {
		return quadratureType;
	}
	
	public void setQuadratureType(QuadratureType type) {
		
		switch(type) {
		case CHANDRASEKHAR :
			complexIntegrator = new ChandrasekharsQuadrature();
			break;
		case SIMPSON : 
			complexIntegrator = new SpecialIntegrator();
			break;
		default :
			throw new IllegalArgumentException("Unkown quadrature type: " + type);
		}
		
		complexIntegrator.setXStep(hx);
		complexIntegrator.setParent(this);
		complexIntegrator.emissionFunction = emissionFunction;
		
	}

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

//	public void setEmissionFunction(EmissionFunction emissionFunction) {
//		this.emissionFunction = emissionFunction;
//	}
//
//	public double fluxFrontThin(double U[]) {
//		emissionIntegrator.setRange(0, tau0);
//		emissionIntegrator.U = U;
//		return emissionFunction.function(U[0]) - emissionFunction.function(U[N])*(1.0 - 2*tau0) 
//				- 2.0*emissionIntegrator.integrate(1);
//	} 
//	
//	public double fluxRearThin(double U[]) {
//		emissionIntegrator.setRange(0, tau0);
//		emissionIntegrator.U = U;;
//		return emissionFunction.function(U[0])*(1.0 - 2*tau0) - emissionFunction.function(U[N]) 
//				+ 2.0*emissionIntegrator.integrate(1);
//	}
//	
//	/*
//	 * Black-body approximation for tau0 << 1
//	 */
//	
//	public double fluxDerivativeThin(double[] U, int uIndex) {
//		return 2.0*(emissionFunction.function(U[0]) 
//					+ emissionFunction.function(U[N]) 
//					- 2.0*emissionFunction.function(U[uIndex]) ); 
//	}
//	
//	public double fluxRosseland(double U[], int uIndex) {
//		return (-1.333333333)/tau0*emissionFunction.firstDerivative(U, uIndex);
//	}
//	
//	/*
//	 * Rosseland approximation for tau0 >> 1
//	 */
//	
//	public double fluxDerivativeRosseland(double[] U, int uIndex) {
//		return 1.333333333/(fastPowLoop(tau0, 2))*emissionFunction.secondDerivative(U, uIndex);
//	}
	
	public enum QuadratureType implements EnumProperty {
		CHANDRASEKHAR, SIMPSON;
		
		@Override
		public Object getValue() {
			return this;
		}	

		@Override
		public String getDescriptor(boolean addHtmlTags) {
			return "Quadrature";
		}

		@Override
		public EnumProperty evaluate(String string) {
			return valueOf(string);
		}
		
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(QuadratureType.CHANDRASEKHAR);
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		//intentionally left blank
	}
	
	@Override
	public boolean ignoreSiblings() {
		return true;
	}
	
	public abstract double getFluxMeanDerivative(int uIndex);
	public abstract double getFluxMeanDerivativeFront();
	public abstract double getFluxMeanDerivativeRear();
	public abstract double getFluxDerivative(int uIndex);
	
	public static String getSelectedDescriptor() {
		return selectedDescriptor;
	}

	public static void setSelectedDescriptor(String selectedDescriptor) {
		RadiativeFluxCalculator.selectedDescriptor = selectedDescriptor;
	}
	
	@Override
	public String toString() {
		return "( " + getQuadratureType() + " : " + emissionFunction + " ; " + emissionIntegrator + " )";
	}
	
}