package pulse.problem.schemes.rte.exact;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;

public abstract class NonscatteringRadiativeTransfer extends RadiativeTransferSolver {
	
	protected double hx;
	private double emissivity, doubleReflectivity;
	protected double tau0;
	
	protected EmissionFunction emissionFunction;
	private EmissionFunctionIntegrator emissionIntegrator;
	protected SimpsonsRule complexIntegrator;
	
	protected static ExponentialFunctionIntegrator simpleIntegrator = ExponentialFunctionIntegrator.getDefaultIntegrator();
	
	protected double r1, r2;
		
	private static InstanceDescriptor<SimpsonsRule> instanceDescriptor = new InstanceDescriptor<SimpsonsRule>("Quadrature Selector", SimpsonsRule.class);
	
	static {
		instanceDescriptor.setSelectedDescriptor( ChandrasekharsQuadrature.class.getSimpleName() );	
	}
	
	protected NonscatteringRadiativeTransfer(ParticipatingMedium problem, Grid grid) {
		super(problem,grid);
		this.hx = grid.getXStep();	
		emissionFunction = new EmissionFunction(0.0,0.0);
		initQuadrature();
		emissionIntegrator = new EmissionFunctionIntegrator();
		emissionIntegrator.emissionFunction = emissionFunction;
	}
	
	@Override
	public void init(ParticipatingMedium p, Grid grid) {
		super.init(p, grid);
		
		emissivity = (double)p.getEmissivity();
		doubleReflectivity = 2.0*(1.0 - emissivity);
		
		emissionFunction.init(p);
		
		setGridStep(hx);

		tau0 = (double)p.getOpticalThickness().getValue();
		complexIntegrator.setOpticalThickness(tau0);
	}
	
	private void setGridStep(double hx) {
		emissionFunction.setGridStep(hx);
		emissionIntegrator.hx = hx;
		complexIntegrator.hx = hx;
	}
	
	/*
	 * Assumes radiosities have already been calculated using radiosities()
	 * F*(1) = -R_2 + 2R_1 E_3(\tau_0) + 2 int 
	 */
	
	private double fluxRear(double[] U) {
		int N = this.getExternalGridDensity();
		this.setFlux(N, 
						-r2 + 2.0*r1*simpleIntegrator.integralAt(tau0, 3) + 2.0*integrateSecondOrder(U, tau0, -1.0) );
		return getFlux(N);
	}
	
	public void boundaryFluxes(double[] U) {
		complexIntegrator.U = U;
		fluxFront(U);
		fluxRear(U);
	}
	
	public double fluxFront(double U[]) {		
		this.setFlux(0, 
				r1 - 2.0*r2*simpleIntegrator.integralAt(tau0, 3) - 2.0*integrateSecondOrder(U, 0.0, 1.0) );
		return getFlux(0);
	}
		
	protected double flux(double U[], int uIndex) {
		double t = getOpticalGridStep()*uIndex;
		
		complexIntegrator.setRange(0, t);
		double I_1 = complexIntegrator.integrate(2, t, -1.0);
		
		complexIntegrator.setRange(t, tau0);
		double I_2 = complexIntegrator.integrate(2, -t, 1.0);
		
		double result = r1*simpleIntegrator.integralAt(t, 3) 
					    - r2*simpleIntegrator.integralAt(tau0 - t, 3)
						+ I_1 - I_2;
		
		setFlux(uIndex, result*2.0);
		
		return getFlux(uIndex);
		
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
		return emissivity*emissionFunction.power(U[0]) +  doubleReflectivity*integrateSecondOrder(U, 0.0, 1.0);
	}
	
	/*
	 * Coefficient a2
     *
	 * a2 = \varepsilon*J*(0) + ...
	 * integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */
	
	private double a2(double[] U) {
		int N = this.getExternalGridDensity();
		return emissivity*emissionFunction.power(U[N]) + doubleReflectivity*integrateSecondOrder(U, tau0, -1.0);
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
		
	public SimpsonsRule getQuadrature() {
		return complexIntegrator;
	}
	
	public void setQuadrature(SimpsonsRule specialIntegrator) {
		this.complexIntegrator = specialIntegrator;
	}

	public void initQuadrature() {
		complexIntegrator = instanceDescriptor.newInstance(SimpsonsRule.class);
		complexIntegrator.setXStep(hx);
		complexIntegrator.setParent(this);
		complexIntegrator.emissionFunction = emissionFunction;	
	}

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(EmissionFunction emissionFunction) {
		this.emissionFunction = emissionFunction;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		//intentionally left blank
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(instanceDescriptor);
		return list;
	}
	
	@Override
	public String toString() {
		return "( " + this.getSimpleName() + " )";
	}
	
	public static void main(String[] args) { 
		
		var problem = new ParticipatingMedium();
		problem.setOpticalThickness(NumericProperty.derive(NumericPropertyKeyword.OPTICAL_THICKNESS, 0.1));
		problem.setEmissivity(NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, 0.85));
		
		File f = null;
		try {
			f = new File(NonscatteringRadiativeTransfer.class.getResource("/test/TestSolution.dat").toURI());
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
		int N = U.length - 1;
		
		var density = NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, N);
		var tauFactor = NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 0.5);
		Grid grid = new Grid(density, tauFactor);
				
		var rte = new AnalyticalDerivativeCalculator(problem,grid);
		rte.complexIntegrator.U = U;
		
		double tFactor = 10.0/800.0;
		var emissionFunction = new EmissionFunction(tFactor, 1.0/N);
		
		rte.emissionFunction = emissionFunction;
		rte.complexIntegrator.emissionFunction = emissionFunction;
		
		rte.init(problem, grid);

		rte.emissionFunction.setReductionFactor(tFactor);
		rte.radiosities(U);
		rte.compute(U);
	
		rte.complexIntegrator.U = U;
		rte.complexIntegrator.setRange(0, 0);
		double I_1 = rte.complexIntegrator.integrate(2, 0, -1.0);
		
		rte.complexIntegrator.setRange(0, rte.tau0);
		double I_2 = rte.complexIntegrator.integrate(2, 0.0, 1.0);
		
		simpleIntegrator.integralAt(0.0, 3);
		simpleIntegrator.integralAt(rte.tau0, 3);
		
		System.out.printf("%n%2.6f %4.4f %4.4f", 0.0, rte.getFlux(0), rte.getFluxDerivativeFront());
		
		for(int i = 1; i < U.length-2; i++) 
			System.out.printf("%n%2.6f %4.4f %4.4f", ((double)problem.getOpticalThickness().getValue()/(double)N)*i, rte.flux(U, i), rte.getFluxDerivative(i));
	
		System.out.printf("%n%2.6f %4.4f %4.4f", (double)problem.getOpticalThickness().getValue(), rte.getFlux(N), rte.getFluxDerivativeRear());
		
	}

	public static InstanceDescriptor<SimpsonsRule> getInstanceDescriptor() {
		return instanceDescriptor;
	}
	
	public static void setInstanceDescriptor( InstanceDescriptor<SimpsonsRule> instanceDescriptor) {
		NonscatteringRadiativeTransfer.instanceDescriptor = instanceDescriptor;
	}
	
	@Override
	public String getDescriptor() {
		return "Non-scattering Radiative Transfer";
	}
	
} 