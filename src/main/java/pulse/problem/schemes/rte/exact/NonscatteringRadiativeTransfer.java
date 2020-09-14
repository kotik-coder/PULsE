package pulse.problem.schemes.rte.exact;

import java.util.List;
import java.util.stream.IntStream;

import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;
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

	private static FunctionWithInterpolation ei3 = ExponentialIntegrals.get(3);

	private double emissivity;

	private BlackbodySpectrum emissionFunction;
	private CompositionProduct quadrature;

	private double radiosityFront;
	private double radiosityRear;

	private InstanceDescriptor<CompositionProduct> instanceDescriptor = new InstanceDescriptor<CompositionProduct>(
			"Quadrature Selector", CompositionProduct.class);

	protected NonscatteringRadiativeTransfer(ParticipatingMedium problem, Grid grid) {
		super();
		instanceDescriptor.setSelectedDescriptor(ChandrasekharsQuadrature.class.getSimpleName());
		init(problem, grid);
		emissionFunction = new BlackbodySpectrum(problem);
		initQuadrature();
		instanceDescriptor.addListener(() -> initQuadrature());
	}

	@Override
	public void init(ParticipatingMedium p, Grid grid) {
		super.init(p, grid);
		emissivity = (double)p.getProperties().getEmissivity().getValue();
	}

	/**
	 * The superclass method will update the interpolation that the blackbody
	 * spectrum uses to evaluate the temperature profile and calculate the
	 * radiosities. A {@code NORMAL} status is always returned.
	 */

	@Override
	public RTECalculationStatus compute(double[] array) {
		emissionFunction.setInterpolation(interpolateTemperatureProfile(array));
		radiosities();
		return RTECalculationStatus.NORMAL;
	}

	/**
	 * Calculates the radiative fluxes on the grid specified in the constructor
	 * arguments. This uses the values of radiosities and involves calculating the
	 * composition product using the selected quadratures.
	 * 
	 * @see pulse.problem.schemes.rte.exact.CompositionProduct
	 */

	public void fluxes() {
		fluxFront();
		IntStream.range(1, getFluxes().getDensity()).forEach(i -> flux(i));
		fluxRear();
	}

	private void fluxFront() {
		final double tau0 = getFluxes().getOpticalThickness();
		double flux = radiosityFront - 2.0 * radiosityRear * ei3.valueAt(tau0) - 2.0 * integrateSecondOrder(0.0, 1.0);
		getFluxes().setFlux(0, flux);
	}

	/*
	 * Assumes radiosities have already been calculated using radiosities() F*(1) =
	 * -R_2 + 2R_1 E_3(\tau_0) + 2 int
	 */

	private void fluxRear() {
		var fluxes = getFluxes();
		final int N = fluxes.getDensity();
		final double tau0 = fluxes.getOpticalThickness();
		double flux = -radiosityRear + 2.0 * radiosityFront * ei3.valueAt(tau0)
				+ 2.0 * integrateSecondOrder(tau0, -1.0);
		fluxes.setFlux(N, flux);
	}

	protected void flux(int uIndex) {
		final double t = opticalCoordinateAt(uIndex);
		final double tau0 = getFluxes().getOpticalThickness();

		quadrature.setOrder(2);
		quadrature.setBounds(new Segment(0, t));
		quadrature.setCoefficients(t, -1.0);
		final double I_1 = quadrature.integrate();

		quadrature.setBounds(new Segment(t, tau0));
		quadrature.setCoefficients(-t, 1.0);
		final double I_2 = quadrature.integrate();

		double result = radiosityFront * ei3.valueAt(t) - radiosityRear * ei3.valueAt(tau0 - t) + I_1 - I_2;

		getFluxes().setFlux(uIndex, result * 2.0);

	}

	/**
	 * Retrieves the quadrature that is used to evaluate the composition product
	 * invoked when calculating the radiative fluxes.
	 * 
	 * @return the quadrature
	 */

	public CompositionProduct getQuadrature() {
		return quadrature;
	}

	/**
	 * Sets the quadrature and updates its spectral function to that specified by
	 * this object.
	 * 
	 * @param specialIntegrator the quadrature used to evaluate the composition
	 *                          product
	 */

	public void setQuadrature(CompositionProduct specialIntegrator) {
		this.quadrature = specialIntegrator;
		quadrature.setParent(this);
		quadrature.setEmissionFunction(emissionFunction);
	}

	public BlackbodySpectrum getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(BlackbodySpectrum emissionFunction) {
		this.emissionFunction = emissionFunction;
		quadrature.setEmissionFunction(emissionFunction);
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

	public InstanceDescriptor<CompositionProduct> getInstanceDescriptor() {
		return instanceDescriptor;
	}

	@Override
	public String getDescriptor() {
		return "Non-scattering Radiative Transfer";
	}

	public double getRadiosityFront() {
		return radiosityFront;
	}

	public double getRadiosityRear() {
		return radiosityRear;
	}

	/*
	 * Radiosities of front and rear surfaces respectively in the assumption of
	 * diffuse and opaque boundaries
	 */

	private void radiosities() {
		final double doubleReflectivity = 2.0 * (1.0 - emissivity);
		;
		final double b = b(doubleReflectivity);
		final double sq = 1.0 - b * b;
		final double a1 = a1(doubleReflectivity);
		final double a2 = a2(doubleReflectivity);

		radiosityFront = (a1 + b * a2) / sq;
		radiosityRear = (a2 + b * a1) / sq;
	}

	/*
	 * Coefficient b
	 */

	private double b(final double doubleReflectivity) {
		return doubleReflectivity * ei3.valueAt(getFluxes().getOpticalThickness());
	}

	/*
	 * Coefficient a1
	 *
	 * a1 = \varepsilon*J*(0) + integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */

	private double a1(final double doubleReflectivity) {
		return emissivity * emissionFunction.powerAt(0.0) + doubleReflectivity * integrateSecondOrder(0.0, 1.0);
	}

	/*
	 * Coefficient a2
	 *
	 * a2 = \varepsilon*J*(0) + ... integral = int_0^1 { J*(t) E_2(\tau_0 t) dt }
	 */

	private double a2(final double doubleReflectivity) {
		final double tau0 = getFluxes().getOpticalThickness();
		return emissivity * emissionFunction.powerAt(tau0) + doubleReflectivity * integrateSecondOrder(tau0, -1.0);
	}

	/*
	 * Source function J*(t) = (1 + @U[i]*tFactor)^4, where i = t/hx tFactor =
	 * (tMax/t0)
	 */

	private double integrateSecondOrder(double a, double b) {
		quadrature.setOrder(2);
		quadrature.setBounds(new Segment(0, getFluxes().getOpticalThickness()));
		quadrature.setCoefficients(a, b);
		return quadrature.integrate();
	}

	private void initQuadrature() {
		setQuadrature(instanceDescriptor.newInstance(CompositionProduct.class));
	}

}