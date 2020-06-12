package pulse.problem.schemes.rte.dom;

public class FixedIterations extends IterativeSolver {

	@Override
	public void doIterations(DiscreteIntensities discrete, AdaptiveIntegrator integrator) {

		double relativeError = 100;

		double qld = 0;
		double qrd = 0;

		for (double ql = 1e8, qr = ql; relativeError > iterationError;) {
			ql = discrete.getQLeft();
			qr = discrete.getQRight();

			integrator.integrate();

			qld = Math.abs(discrete.qLeft(integrator.emissionFunction) - ql);
			qrd = Math.abs(discrete.qRight(integrator.emissionFunction) - qr);

			// if the integrator attempted rescaling, last iteration is not valid anymore
			relativeError = integrator.wasRescaled() ? Double.POSITIVE_INFINITY
					: (qld + qrd) / (Math.abs(ql) + Math.abs(qr));

		}

	}

}