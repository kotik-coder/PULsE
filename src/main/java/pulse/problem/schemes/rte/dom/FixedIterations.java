package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.RTECalculationStatus;

public class FixedIterations extends IterativeSolver {

	@Override
	public RTECalculationStatus doIterations(DiscreteIntensities discrete, AdaptiveIntegrator integrator) {

		double relativeError = 100;

		double qld = 0;
		double qrd = 0;

		int iterations = 0;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;

		for (double ql = 1e8, qr = ql; relativeError > iterationError
				&& status == RTECalculationStatus.NORMAL; status = sanityCheck(status, ++iterations)) {
			ql = discrete.getQLeft();
			qr = discrete.getQRight();

			status = integrator.integrate();

			qld = Math.abs(discrete.qLeft(integrator.emissionFunction) - ql);
			qrd = Math.abs(discrete.qRight(integrator.emissionFunction) - qr);

			// if the integrator attempted rescaling, last iteration is not valid anymore
			relativeError = integrator.wasRescaled() ? Double.POSITIVE_INFINITY
					: (qld + qrd) / (Math.abs(ql) + Math.abs(qr));
		}

		return status;

	}

}