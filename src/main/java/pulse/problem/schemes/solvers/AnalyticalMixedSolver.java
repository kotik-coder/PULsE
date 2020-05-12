package pulse.problem.schemes.solvers;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.radiation.AnalyticalDerivativeCalculator;
import pulse.problem.schemes.radiation.MathUtils;
import pulse.problem.statements.AbsorbingEmittingProblem;
import pulse.properties.NumericProperty;
import pulse.ui.Messages;

public class AnalyticalMixedSolver extends MixedCoupledSolver {

	public AnalyticalMixedSolver() {
		super();
	}

	public AnalyticalMixedSolver(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
	}

	public AnalyticalMixedSolver(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		super(N, timeFactor, timeLimit);
	}

	@Override
	public void solve(AbsorbingEmittingProblem problem) {

		prepare(problem);
		
		final double wFactor = timeInterval * tau * problem.timeFactor();
		final double errorSq = MathUtils.fastPowLoop( nonlinearPrecision, 2);
						
		rte.compute(U);
		
		// time cycle

		final double HX_NP = hx/Np;
		final double TAU0_NP = opticalThickness/Np;
		final double _2TAUHX = 2.0*tau*hx;
		final double HX2_2TAU = HX2/(2.0*tau);
		final double Bi2HX = Bi2*hx;
		final double SIGMA_NP = sigma/Np;
		final double ONE_MINUS_SIGMA_NP = (1. - sigma)/Np;
		final double _2TAU_ONE_MINUS_SIGMA = 2.0*tau*(1.0 - sigma);
		final double ONE_PLUS_Bi1_HX = (1. + Bi1*hx);
		final double BETA1_FACTOR = 1.0/(HX2 + 2.0*tau*sigma*(1 + hx*Bi1)); 
		final double ONE_MINUS_SIGMA = 1.0 - sigma;
		
		double phi;
		
		int i, m, w, j;
		double F, pls;		
		double V_0, V_N;
		
		for (w = 1; w < counts; w++) {

			for (m = (w - 1) * timeInterval + 1; m < w * timeInterval + 1; m++) {

				pls 	 = discretePulse.evaluateAt( (m - 1 + EPS)*tau )*ONE_MINUS_SIGMA
						 + discretePulse.evaluateAt( (m - EPS)*tau )*sigma; 

				for( V_0 = errorSq + 1, V_N = errorSq + 1; 
						   (MathUtils.fastPowLoop((V[0] - V_0), 2) > errorSq) ||
						   (MathUtils.fastPowLoop((V[N] - V_N), 2) > errorSq)
						 ; rte.compute(V)) {
					
					//i = 0
					phi = TAU0_NP*rte.getFluxDerivative(0);
					beta[1] = (_2TAUHX*(pls - SIGMA_NP*rte.getFlux(0) - ONE_MINUS_SIGMA_NP*rte.getStoredFlux(0) ) +
							HX2*(U[0] + phi*tau) + _2TAU_ONE_MINUS_SIGMA*(U[1] - U[0]*ONE_PLUS_Bi1_HX) )
							*BETA1_FACTOR;

					for (i = 1; i < N; i++) {
						phi = TAU0_NP*phi(i);
						F = U[i] / tau + phi + ONE_MINUS_SIGMA*(U[i+1] - 2*U[i] + U[i-1])/HX2;
						beta[i + 1] = (F + a * beta[i]) / (b - a * alpha[i]);
					}
					
					V_N = V[N];
					phi = TAU0_NP*rte.getFluxDerivative(N);
					V[N] = (sigma*beta[N] + HX2_2TAU*U[N] + 0.5*HX2*phi + ONE_MINUS_SIGMA*(U[N-1] - U[N]*(1. + hx*Bi2) ) 
							+ HX_NP*(sigma*rte.getFlux(N) + ONE_MINUS_SIGMA*rte.getStoredFlux(N) ) )
							/(HX2_2TAU + sigma*(1. - alpha[N] + Bi2HX ));

					V_0 = V[0]; 
					for (j = N - 1; j >= 0; j--)
						V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
				}
				
				System.arraycopy(V, 0, U, 0, N + 1);
				rte.storeFluxes();

			}

			curve.addPoint( w * wFactor, V[N] );

			/*
			 * UNCOMMENT TO DEBUG
			 */

			//debug(problem, V, w);

		}
	
		curve.scale( maxTemp/curve.apparentMaximum() );

	}
	
	@Override
	public String toString() {
		return Messages.getString("MixedScheme2.4");
	}
	
	@Override
	public DifferenceScheme copy() {
		return new AnalyticalMixedSolver(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public void initRTE() {
		rte = new AnalyticalDerivativeCalculator(grid);	
		rte.setParent(this);
	}

}