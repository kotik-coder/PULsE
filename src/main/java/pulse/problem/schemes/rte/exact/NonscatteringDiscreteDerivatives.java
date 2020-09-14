package pulse.problem.schemes.rte.exact;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.FluxesAndImplicitDerivatives;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.ThermoOpticalProperties;

/**
 * A solver of the radiative transfer equation for an absorbing-emitting medium
 * where the fluxes are calculated using analytical formulae while their
 * derivatives are calculated using the central-difference approximation.
 *
 */

public class NonscatteringDiscreteDerivatives extends NonscatteringRadiativeTransfer {

	public NonscatteringDiscreteDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
		var properties = (ThermoOpticalProperties)problem.getProperties();
		setFluxes(new FluxesAndImplicitDerivatives(grid.getGridDensity(), properties.getOpticalThickness()));
	}

	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		fluxes();
		return RTECalculationStatus.NORMAL;
	}

}