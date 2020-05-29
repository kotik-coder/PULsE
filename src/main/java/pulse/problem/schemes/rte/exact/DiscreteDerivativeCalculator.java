package pulse.problem.schemes.rte.exact;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.ParticipatingMedium;

public class DiscreteDerivativeCalculator extends NonscatteringRadiativeTransfer {
	
	public DiscreteDerivativeCalculator(ParticipatingMedium problem, Grid grid) {
		super(problem, grid);
	}
	
	@Override
	public void init(ParticipatingMedium p, Grid grid) {
		super.init(p, grid);
	}
	
	@Override
	public void compute(double U[]) {
		radiosities(U);
		complexIntegrator.U = U;
		for(int i = 1, N = this.getExternalGridDensity(); i < N; i++)
			flux(U, i);
		boundaryFluxes(U);
	}
		
}