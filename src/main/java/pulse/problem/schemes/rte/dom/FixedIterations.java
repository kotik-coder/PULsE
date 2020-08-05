package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.RTECalculationStatus;

public class FixedIterations extends IterativeSolver {

	@Override
	public RTECalculationStatus doIterations(AdaptiveIntegrator integrator) {

		var discrete = integrator.getDiscretisation();
		var quantities = discrete.getQuantities();
		double relativeError = 100;

		double qld = 0;
		double qrd = 0;

		int iterations = 0;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;
		final var ef = integrator.getEmissionFunction();
		
		for (double ql = 1e8, qr = ql; relativeError > getIterationError()
				&& status == RTECalculationStatus.NORMAL; status = sanityCheck(status, ++iterations)) {
			ql = quantities.getFluxLeft();
			qr = quantities.getFluxRight();

			status = integrator.integrate();

			qld = Math.abs(discrete.fluxLeft(ef) - ql);
			qrd = Math.abs(discrete.fluxRight(ef) - qr);

			// if the integrator attempted rescaling, last iteration is not valid anymore
			relativeError = integrator.wasRescaled() ? Double.POSITIVE_INFINITY
					: (qld + qrd) / (Math.abs(ql) + Math.abs(qr));
		}

		return status;

	}

}