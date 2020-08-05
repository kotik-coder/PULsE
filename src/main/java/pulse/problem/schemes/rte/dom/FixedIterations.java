package pulse.problem.schemes.rte.dom;

import static java.lang.Math.abs;
import pulse.problem.schemes.rte.RTECalculationStatus;

public class FixedIterations extends IterativeSolver {

	@Override
	public RTECalculationStatus doIterations(AdaptiveIntegrator integrator) {

		var discrete = integrator.getDiscretisation();
		double relativeError = 100;

		double qld = 0;
		double qrd = 0;
		double qsum;

		int iterations = 0;

		RTECalculationStatus status = RTECalculationStatus.NORMAL;
		final var ef = integrator.getEmissionFunction();

		for (double ql = 1e8, qr = ql; relativeError > getIterationError()
				&& status == RTECalculationStatus.NORMAL; status = sanityCheck(status, ++iterations)) {	
			// do the integration
			status = integrator.integrate();

			// get the difference in boundary heat fluxes
			qld = discrete.fluxLeft(ef);
			qrd = discrete.fluxRight(ef);
			qsum = abs(qld - ql) + abs(qrd - qr);
		
			// if the integrator attempted rescaling, last iteration is not valid anymore
			relativeError = integrator.wasRescaled() ? Double.POSITIVE_INFINITY : qsum / (abs(ql) + abs(qr));
			
			//use previous iteration
			ql = qld;
			qr = qrd;			
		}

		return status;

	}

}