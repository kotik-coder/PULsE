package pulse.problem.schemes.rte.exact;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.problem.statements.ParticipatingMedium;

public class NonscatteringDiscreteDerivatives extends NonscatteringRadiativeTransfer {

	public NonscatteringDiscreteDerivatives(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
	}

	@Override
	public void init(ParticipatingMedium p, Grid grid) {
		super.init(p, grid);
	}

	@Override
	public RTECalculationStatus compute(double U[]) {
		super.compute(U);
		radiosities();
		for (int i = 1, N = this.getExternalGridDensity(); i < N; i++)
			flux(i);
		boundaryFluxes();
		return RTECalculationStatus.NORMAL;
	}

}