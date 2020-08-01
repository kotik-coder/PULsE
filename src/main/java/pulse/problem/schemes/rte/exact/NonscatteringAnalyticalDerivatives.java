package pulse.problem.schemes.rte.exact;

import static java.lang.Double.compare;

import pulse.math.FunctionWithInterpolation;
import pulse.math.Segment;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.FluxesAndExplicitDerivatives;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;

/**
 * A solver of the radiative transfer equation for an absorbing-emitting medium
 * where the fluxes and their derivatives are calculated using analytical
 * formulae with the selected numerical quadrature.
 *
 */

public class NonscatteringAnalyticalDerivatives extends NonscatteringRadiativeTransfer {

	private static FunctionWithInterpolation ei2 = ExponentialIntegrals.get(2);

	public NonscatteringAnalyticalDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
		final int N = (int) grid.getGridDensity().getValue();
		final double tau0 = (double) problem.getOpticalThickness().getValue();
		setFluxes(new FluxesAndExplicitDerivatives(N, tau0));
	}

	/**
	 * Evaluates fluxes and their derivatives using analytical formulae and the
	 * selected numerical quadrature. Usually works best with the
	 * {@code ChandrasekharsQuadrature}.
	 */

	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		fluxes();
		for (int i = 0, N = getFluxes().getDensity(); i < N; i++) {
			evalFluxDerivative(i);
		}
		return RTECalculationStatus.NORMAL;
	}

	/*
	 * -dF/d\tau
	 *
	 * = 2 R_1 E_2(y \tau_0) + 2 R_2 E_2( (1 - y) \tau_0 ) - \pi J*(y 'tau_0)
	 *
	 */

	private void evalFluxDerivative(final int uIndex) {
		double t = opticalCoordinateAt(uIndex);

		double value = getRadiosityFront() * ei2.valueAt(t)
				+ getRadiosityRear() * ei2.valueAt(getFluxes().getOpticalThickness() - t)
				- 2.0 * getEmissionFunction().powerAt(t) + integrateFirstOrder(t);

		((FluxesAndExplicitDerivatives) getFluxes()).setFluxDerivative(uIndex, 2.0 * value);
	}

	private double integrateFirstOrder(final double y) {
		double integral = 0;
		final double tau0 = getFluxes().getOpticalThickness();
		var quadrature = getQuadrature();

		setForIntegration(0, y);
		quadrature.setCoefficients(y, -1);
		integral += compare(y, 0) == 0 ? 0 : quadrature.integrate();

		setForIntegration(y, tau0);
		quadrature.setCoefficients(-y, 1);
		integral += compare(y, tau0) == 0 ? 0 : quadrature.integrate();

		return integral;
	}

	/**
	 * This will set integration bounds by creating a segment using {@code x} and
	 * {@code y} values. Note this ignores the order of arguments, as the lower and
	 * upper bound will be equal to {@code min(x,y)} and {@code max(x,y)}
	 * respectively. The order of integration is set to unity.
	 * 
	 * @param x lower bound
	 * @param y upper bound
	 */

	private void setForIntegration(final double x, final double y) {
		final var quadrature = getQuadrature();
		quadrature.setBounds(new Segment(x, y));
		quadrature.setOrder(1);
	}

}