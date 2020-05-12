package pulse.problem.schemes.radiation;

import pulse.problem.schemes.Grid;
import pulse.problem.statements.AbsorbingEmittingProblem;

public class AnalyticalDerivativeCalculator extends RadiativeFluxCalculator {

	private double FD[], FDP[];
	
	public AnalyticalDerivativeCalculator(Grid grid) {
		super(grid);
		FD = new double[N+1];	
		FDP = new double[N+1];
	}
	
	@Override 
	public void init(AbsorbingEmittingProblem p, Grid grid) {
		super.init(p, grid);
		FD = new double[N+1];	
		FDP = new double[N+1];
	}
	
	public double getFluxDerivative(int index) {
		return FD[index];
	}
	
	public double getStoredFluxDerivative(int index) {
		return FDP[index];
	}

	@Override
	public void storeFluxes() {
		super.storeFluxes();
		System.arraycopy(FD, 0, FDP, 0, N + 1); //store previous results
	}
	
	public double getFluxMeanDerivative(int uIndex) {
		return 0.5*( FD[uIndex] + FDP[uIndex] );
	}
	
	public double getFluxMeanDerivativeFront() {
		return 0.5*( FD[0] + FDP[0] );
	}
	
	public double getFluxMeanDerivativeRear() {
		return 0.5*( FD[N] + FDP[N] );
	}

	
	@Override
	public void compute(double U[]) {
		radiosities(U);
		FD[0] = fluxDerivativeFront(U);
		for(int i = 1; i < N; i++)
			FD[i] = fluxDerivative(U, i);
		FD[N] = fluxDerivativeRear(U);
		boundaryFluxes(U);
	}

	/*
	 * -dF/d\tau
	 *
	 * = 2 R_1 E_2(y \tau_0) + 2 R_2 E_2( (1 - y) \tau_0 ) - \pi J*(y 'tau_0) 
	 *
	 */
	
	public double fluxDerivative(double U[], int uIndex) {
		double t = hx*uIndex*tau0;
				
		double value = r1*simpleIntegrator.integralAt(t, 2) + r2*simpleIntegrator.integralAt(tau0 - t, 2)
		   - 2.0*emissionFunction.function(U[uIndex]) + integrateFirstOrder(t);
		
		return 2*value;
		
	}
	
	private double fluxDerivativeFront(double[] U) {
		double value = r1*simpleIntegrator.integralAt(0, 2) + r2*simpleIntegrator.integralAt(tau0, 2)
		   - 2.0*emissionFunction.function(U[0]) + integrateFirstOrderFront();
		
		return 2*value;
		
	}

	private double fluxDerivativeRear(double[] U) {
		double t = hx*N*tau0;
				
		double value = r1*simpleIntegrator.integralAt(t, 2) + r2*simpleIntegrator.integralAt(tau0 - t, 2)
		   - 2.0*emissionFunction.function(U[N]) + integrateFirstOrderRear();
		
		return 2*value;
		
	}
	
	
	private double integrateFirstOrder(double y) {
		double integral = 0;
		
		complexIntegrator.setRange(0, y);
		integral += Double.compare(y, 0) == 0 ? 0 : complexIntegrator.integrate(1, y, -1);

		complexIntegrator.setRange(y, tau0);
		integral += Double.compare(y, tau0) == 0 ? 0 : complexIntegrator.integrate(1, -y, 1);
		
		return integral;
		
	}
	
	private double integrateFirstOrderFront() {
		complexIntegrator.setRange(0, tau0);
		return complexIntegrator.integrate(1, 0, 1);
	}
	
	private double integrateFirstOrderRear() {
		complexIntegrator.setRange(0, tau0);
		return complexIntegrator.integrate(1, tau0, -1);
	}

	@Override
	public String getDescriptor() {
		return "Analytical Flux Calculator";
	}
	
}