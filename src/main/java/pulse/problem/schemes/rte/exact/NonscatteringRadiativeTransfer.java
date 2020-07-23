package pulse.problem.schemes.rte.exact;

import java.util.List;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.schemes.rte.RTECalculationStatus;
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

	protected BlackbodySpectrum emissionFunction;
	private EmissionFunctionIntegrator emissionIntegrator;
	protected SimpsonsRule complexIntegrator;

	protected static ExponentialFunctionIntegrator simpleIntegrator = ExponentialFunctionIntegrator
			.getDefaultIntegrator();

	protected double r1, r2;

	private static InstanceDescriptor<SimpsonsRule> instanceDescriptor = new InstanceDescriptor<SimpsonsRule>(
			"Quadrature Selector", SimpsonsRule.class);

	static {
		instanceDescriptor.setSelectedDescriptor(ChandrasekharsQuadrature.class.getSimpleName());
	}

	protected NonscatteringRadiativeTransfer(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
		this.hx = grid.getXStep();
		initQuadrature();
		emissionIntegrator = new EmissionFunctionIntegrator();

		instanceDescriptor.addListener(() -> initQuadrature());

	}

	@Override
	public RTECalculationStatus compute(double[] tempArray) {
		double[] xArray = new double[tempArray.length];

		for (int i = 0; i < xArray.length; i++) {
			xArray[i] = tau0 * i * hx;
		}

		emissionFunction.setInterpolation(this.temperatureInterpolation(xArray, tempArray));
		return RTECalculationStatus.NORMAL;
	}

	@Override
	public void init(ParticipatingMedium p, Grid grid) {
		super.init(p, grid);

		emissivity = p.getEmissivity();
		doubleReflectivity = 2.0 * (1.0 - emissivity);

		emissionFunction = new BlackbodySpectrum(p);
		emissionIntegrator.emissionFunction = emissionFunction;
		complexIntegrator.emissionFunction = emissionFunction;
		
		setGridStep(hx);

		tau0 = (double) p.getOpticalThickness().getValue();
		complexIntegrator.setOpticalThickness(tau0);
	}

	private void setGridStep(double hx) {
		emissionIntegrator.hx = hx;
		complexIntegrator.hx = hx;
	}

	/*
	 * Assumes radiosities have already been calculated using radiosities() F*(1) =
	 * -R_2 + 2R_1 E_3(\tau_0) + 2 int
	 */

	private double fluxRear() {
		int N = this.getExternalGridDensity();
		this.setFlux(N, -r2 + 2.0 * r1 * simpleIntegrator.integralAt(tau0, 3) + 2.0 * integrateSecondOrder(tau0, -1.0));
		return getFlux(N);
	}

	public void boundaryFluxes() {
		fluxFront();
		fluxRear();
	}

	public double fluxFront() {
		this.setFlux(0, r1 - 2.0 * r2 * simpleIntegrator.integralAt(tau0, 3) - 2.0 * integrateSecondOrder(0.0, 1.0));
		return getFlux(0);
	}

	protected double flux(int uIndex) {
		double t = getOpticalGridStep() * uIndex;

		complexIntegrator.setRange(0, t);
		double I_1 = complexIntegrator.integrate(2, t, -1.0);

		complexIntegrator.setRange(t, tau0);
		double I_2 = complexIntegrator.integrate(2, -t, 1.0);

		double result = r1 * simpleIntegrator.integralAt(t, 3) - r2 * simpleIntegrator.integralAt(tau0 - t, 3) + I_1
				- I_2;

		setFlux(uIndex, result * 2.0);

		return getFlux(uIndex);

	}

	/*
	 * Radiosities of front and rear surfaces respectively in the assumption of
	 * diffuse and opaque boundaries
	 */

	public void radiosities() {
		double _b = b();
		double sq = 1.0 - _b * _b;
		r1 = (a1() + _b * a2()) / sq;
		r2 = (a2() + _b * a1()) / sq;
	}

	/*
	 * Coefficient b
	 */

	private double b() {
		return doubleReflectivity * simpleIntegrator.integralAt(tau0, 3);
	}

	/*
	 * Coefficient a1
	 *
	 * a1 = \varepsilon*J*(0) + integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */

	private double a1() {
		return emissivity * emissionFunction.powerAt(0.0) + doubleReflectivity * integrateSecondOrder(0.0, 1.0);
	}

	/*
	 * Coefficient a2
	 *
	 * a2 = \varepsilon*J*(0) + ... integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */

	private double a2() {
		return emissivity * emissionFunction.powerAt(tau0) + doubleReflectivity * integrateSecondOrder(tau0, -1.0);
	}

	/*
	 * Source function J*(t) = (1 + @U[i]*tFactor)^4, where i = t/hx tFactor =
	 * (tMax/t0)
	 */

	private double integrateSecondOrder(double a, double b) {
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

	public BlackbodySpectrum getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.emissionFunction = emissionFunction;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// intentionally left blank
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

	public static InstanceDescriptor<SimpsonsRule> getInstanceDescriptor() {
		return instanceDescriptor;
	}

	@Override
	public String getDescriptor() {
		return "Non-scattering Radiative Transfer";
	}

}