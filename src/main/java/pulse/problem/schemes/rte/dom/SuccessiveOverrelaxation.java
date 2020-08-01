package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class SuccessiveOverrelaxation extends IterativeSolver {

	private double W;

	public SuccessiveOverrelaxation() {
		super();
		this.W = (double) NumericProperty.theDefault(NumericPropertyKeyword.RELAXATION_PARAMETER).getValue();
	}

	@Override
	public RTECalculationStatus doIterations(DiscreteIntensities discrete, AdaptiveIntegrator integrator) {

		double relativeError = 100;

		double qld = 0;
		double qrd = 0;

		int iterations = 0;
		final var ef = integrator.getEmissionFunction();
		RTECalculationStatus status = RTECalculationStatus.NORMAL;

		for (double ql = 1e8, qr = ql; relativeError > iterationError; status = sanityCheck(status, ++iterations)) {
			ql = discrete.getFluxLeft();
			qr = discrete.getFluxRight();

			integrator.storeIteration();
			status = integrator.integrate();

			// if the integrator attempted rescaling, last iteration is not valid anymore
			if (integrator.wasRescaled()) {
				relativeError = Double.POSITIVE_INFINITY;
			} else { // calculate the (k+1) iteration as: I_k+1 = I_k * (1 - W) + I*
				integrator.successiveOverrelaxation(W);
				qld = Math.abs(discrete.fluxLeft(ef) - ql);
				qrd = Math.abs(discrete.fluxRight(ef) - qr);
				relativeError = (qld + qrd) / (Math.abs(ql) + Math.abs(qr));
			}

		}

		return status;

	}

	public NumericProperty getRelaxationParameter() {
		return NumericProperty.derive(NumericPropertyKeyword.RELAXATION_PARAMETER, W);
	}

	public void setRelaxationParameter(NumericProperty p) {
		if (p.getType() != NumericPropertyKeyword.RELAXATION_PARAMETER)
			throw new IllegalArgumentException("Unknown type: " + p.getType());
		W = (double) p.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {

		super.set(type, property);

		if (type == NumericPropertyKeyword.RELAXATION_PARAMETER)
			setRelaxationParameter(property);
		else
			throw new IllegalArgumentException("Unknown type: " + type);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.theDefault(NumericPropertyKeyword.RELAXATION_PARAMETER));
		return list;
	}

	@Override
	public String toString() {
		return super.toString() + " ; " + getRelaxationParameter();
	}

}