package pulse.problem.schemes.radiation;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.AbsorbingEmittingProblem;

public class DiscreteDerivativeCalculator extends RadiativeFluxCalculator {

	private double _h, _2h, _05h, HX_TAU0;
	
	public DiscreteDerivativeCalculator(Grid grid) {
		super(grid);
	}
	
	public void init(AbsorbingEmittingProblem p, Grid grid) {
		super.init(p, grid);
		HX_TAU0 = hx*tau0;
		_h = 1./(2.0*HX_TAU0);
		_2h = _h/2.0;
		_05h = 2.0*_h;
	}
	
	@Override
	public void compute(double U[]) {
		radiosities(U);
		complexIntegrator.U = U;
		for(int i = 1; i < N; i++)
			F[i] = flux(U, i);
		boundaryFluxes(U);
	}
	
	public double getFluxMeanDerivative(int uIndex) {
		double f = ( F[uIndex - 1] - F[uIndex + 1] ) + ( FP[uIndex - 1] - FP[uIndex + 1] );
		return f*_2h;
	}
	
	public double getFluxDerivative(int uIndex) {
		return ( F[uIndex - 1] - F[uIndex + 1] )*_h;
	}
	
	public double getFluxMeanDerivativeFront() {
		double f = ( F[0] - F[1] ) + ( FP[0] - FP[1] );
		return f*_h;
	}
	
	public double getFluxMeanDerivativeRear() {
		double f = ( F[N-1] - F[N] ) + ( FP[N-1] - FP[N] );
		return f*_h;	
	}
	
	public double fluxDerivative(double[] U, int uIndex) {
		return (flux(U, uIndex-1) - flux(U, uIndex+1))*_h;		
	}
	
	public double fluxDerivativeFront() {
		return (F[0] - F[1])*_05h;
	}
	
	public double fluxDerivativeRear() {
		return (F[N-1] - F[N])*_05h;
	}
	
	@Override
	public String getDescriptor() {
		return "Discrete Flux Calculator";
	}
	
}