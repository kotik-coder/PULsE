package pulse.problem.schemes.rte.exact;

import java.util.stream.IntStream;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.schemes.rte.Integrator;

/**
 * A class with simple quadrature methods for evaluating the definite integral
 * $\int_a^b{f(x) E_n (\alpha + \beta x) dx}$
 *
 */

public class SimpsonsRule extends Integrator {

	protected double min;
	protected double max;
	protected double rMax;
	protected double rMin;

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

	/*
	 * integral = int_0^tau_0{J*(t) E_2(a + b t) dt} A , B as params
	 */

	public void adjustRange(double alpha, double beta) {
		double bound = (cutoff - alpha) / beta;

		double a = 0.5 - beta / 2;
		double b = 1. - a;

		rMax = max * a + Math.min(bound, max) * b;
		rMin = Math.max(bound, min) * a + min * b;
	}

	@Override
	public double integrate(int order, double... params) {
		adjustRange(params[A_INDEX], params[B_INDEX]);
		stepSize = (rMax - rMin) / integrationSegments;
		return integrateSimpson(order, params);
	}

	public double integrateMidpoint(int order, double... params) {
		double integral = 0;

		for (int i = 0; i < integrationSegments; i++) {
                    integral += integrand(order, params[A_INDEX], params[B_INDEX], rMin + (i + 0.5) * stepSize);
                }

		return integral * stepSize;
	}

	/**********************************************************************
	 * Integrate f from a to b using Simpson's rule. Source:
	 * https://introcs.cs.princeton.edu/java/93integration/SimpsonsRule.java.html
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
			x += integrand(order, params[0], params[1], rMin + stepSize * i);
		}

		// 2/3 terms
		for (int i = 2; i < integrationSegments; i += 2) {
                    y += integrand(order, params[0], params[1], rMin + stepSize * i);
                }

		sum += x * 4.0 + y * 2.0;

		return sum * stepSize / 3.0;
	}

	public double integrateBoole(int order, double... params) {
		double fa = integrand(order, params[0], params[1], rMin);
		double fb = integrandNoInterpolation(order, params[0], params[1], rMax);

		// 1/90 terms
		double sum = (fa + fb);

		// 1/32 terms
		sum += 32. * IntStream.range(0, integrationSegments)
				.mapToDouble(i -> integrand(order, params[0], params[1], rMin + stepSize * (i + 0.25))
						+ integrand(order, params[0], params[1], rMin + stepSize * (i + 0.75)))
				.sum();

		// 1/12 terms
		sum += 12. * IntStream.range(0, integrationSegments)
				.mapToDouble(i -> integrand(order, params[0], params[1], rMin + stepSize * (i + 0.5))).sum();

		// 1/14 terms
		sum += 14. * IntStream.range(1, integrationSegments)
				.mapToDouble(i -> integrand(order, params[0], params[1], rMin + stepSize * i)).sum();

		return sum * stepSize / 90.;
	}

	/*
	 * a - params[0] b - params[1] t - params[2]
	 */

	@Override
	public double integrand(int order, double... params) {
		return emissionFunction.powerInterpolated(params[T_INDEX])
				* expIntegrator.integralAt(params[A_INDEX] + params[B_INDEX] * params[T_INDEX], order);
	}

	public double integrandNoInterpolation(int order, double... params) {
		return emissionFunction.powerInterpolated(params[T_INDEX])
				* expIntegrator.integralAt(params[A_INDEX] + params[B_INDEX] * params[T_INDEX], order);
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

	@Override
	public String getPrefix() {
		return "Numerical Quadrature";
	}

}