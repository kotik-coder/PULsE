package pulse.problem.schemes;

import static java.lang.Math.pow;
import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem2D;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

import static java.lang.Math.max;

public class ADIScheme extends DifferenceScheme {
	
	private final static NumericProperty TAU_FACTOR = 
			NumericProperty.derive(NumericPropertyKeyword.TAU_FACTOR, 1.0);
	private final static NumericProperty GRID_DENSITY = 
			NumericProperty.derive(NumericPropertyKeyword.GRID_DENSITY, 30);
	
	public Solver<LinearisedProblem2D> linearisedSolver2D = 
			(problem -> {
				super.prepare(problem);

				DiscretePulse2D discretePulse2D = (DiscretePulse2D)discretePulse;
				
				//quick links

				final int N			= grid.N;
				
				final double Bi1	= (double) problem.getFrontHeatLoss().getValue();
				final double Bi2 	= (double) problem.getHeatLossRear().getValue();
				final double Bi3	= (double) problem.getSideLosses().getValue();
				
				final double d		= (double) problem.getSampleDiameter().getValue();
				final double dAv	= (double) problem.getPyrometerSpot().getValue();
				final double l		= (double) problem.getSampleThickness().getValue();
				
				final double maxTemp 	= (double) problem.getMaximumTemperature().getValue();
				
				//end

				double[][] U1	= new double[N + 1][N + 1];
				double[][] U2	= new double[N + 1][N + 1];
				
				double[][] U1_E	= new double[N + 3][N + 3];
				double[][] U2_E = new double[N + 3][N + 3];				
				
				double[] alpha  = new double[N + 2];
				double[] beta   = new double[N + 2];
				
				double[] a1 = new double[N + 1];
				double[] b1 = new double[N + 1];
				double[] c1 = new double[N + 1];
				
				final double EPS = 1e-8;
				
				HeatingCurve curve = problem.getHeatingCurve();
				curve.reinit();
				
				final int counts = (int) curve.getNumPoints().getValue();
				
				double maxVal = 0;
				int i, j, m, w;
				double pls;

				double tau	= grid.tau;
				double hy	= ((Grid2D)getGrid()).hy;
				double hx	= grid.hx;
				double HX2	= pow(hx,2); 
				double HY2	= pow(hy,2);	
				
				//precalculated FD constants				
				
				double OMEGA	= 2.0*l/d;
				double OMEGA_SQ = OMEGA*OMEGA;
				
				//a[i]*u[i-1] - b[i]*u[i] + c[i]*u[i+1] = F[i]

				for(i = 1; i < N + 1; i++) {
					a1[i] = OMEGA_SQ*(i - 0.5)/HX2/i;
				    b1[i] = 2./tau + 2.*OMEGA_SQ/HX2;
				    c1[i] = OMEGA_SQ*(i + 0.5)/HX2/i;	
				}
				
				double a2 = 1./HY2;
				double b2 = 2./HY2 + 2./grid.tau;
				double c2 = 1./HY2;
				
				double F;
				
				int lastIndex = (int)(dAv/d/hx);
				lastIndex 	  = lastIndex > N ? N : lastIndex;
				
				//precalc coefs

				double a11 = 1.0/( 1.0 + HX2/(OMEGA_SQ*grid.tau) );
				double b11 = 0.5*grid.tau/(1.0 + OMEGA_SQ*grid.tau/HX2);

				double _a11 = 1.0/(1.0 + Bi1*hy + HY2/grid.tau);
				double _b11 = 1.0/((1 + hy*Bi1)*grid.tau + HY2);
				double _c11 = 0.5*HY2*grid.tau*OMEGA_SQ/HX2;
				double _b12 = _c11*_b11;

				//end of coefs			
				
				//begin time cycle
				
				for (w = 1; w < counts; w++) {
					
					for (m = (w - 1)*timeInterval; m < w*timeInterval; m++) {
						
						/*create extended U1 array to accommodate edge values */ 
						
						for(i = 0; i <= N; i++) { 
							
							System.arraycopy(U1[i], 0, U1_E[i+1], 1, N+1);
							
							pls = discretePulse2D.evaluateAt( (m + EPS)*tau, i*hx); //i = 0, j = 0
							U1_E[i+1][0]	= U1[i][1] 	 + 2.0 * hy * pls - 2.0 * hy * Bi1 * U1[i][0];
							U1_E[i+1][N+2]	= U1[i][N-1] - 2.0 * hy * Bi2 * U1[i][N];														
						}
						
						//first equation, i -> x (radius), j -> y (thickness)
						
						alpha[1] = a11;
						
						for (j = 0; j <= N; j++) {
							
							beta[1]  = b11*(2.*U1_E[1][j+1]/tau + (U1_E[1][j+2] - 2.*U1_E[1][j+1] + U1_E[1][j])/HY2);

							for (i = 1; i < N; i++) {
								F  		   = -2.*U1_E[i+1][j+1]/tau - (U1_E[i+1][j] - 2.0*U1_E[i+1][j+1] + U1_E[i+1][j+2])/HY2;
							    alpha[i+1] = c1[i]/(b1[i]-a1[i]*alpha[i]);
							    beta[i+1]  = (F - a1[i]*beta[i])/(a1[i]*alpha[i] - b1[i]);
							}

						    U2[N][j] = ( 
						    				 OMEGA_SQ*tau*beta[N] + HX2*U1_E[N+1][j+1] + 
						    				 HX2*tau/(2.0*HY2) * (U1_E[N+1][j+2] - 2*U1_E[N+1][j+1] + U1_E[N+1][j] ) 
						    				) /( (1.0 - alpha[N] + hx*OMEGA*Bi3)*OMEGA_SQ*tau + HX2 );						    

						    for (i = N - 1; i >= 0; i--) 
						    	U2[i][j] = alpha[i+1]*U2[i+1][j] + beta[i+1];						    
														
						}
						
						//second equation
						
						/*create extended U2 array to accommodate edge values */ 
						
						for(j = 0; j <= N; j++) { 
							
							for(i = 0; i <= N; i++)
								U2_E[i+1][j+1] = U2[i][j];
							
							U2_E[N+2][j+1] = U2[N-1][j] - 2.0 * hx * OMEGA * Bi3 * U2[N][j];
						}
						
						alpha[1] = _a11;
						
						for (i = 1; i <= N; i++) {
							
							pls = discretePulse2D.evaluateAt( (m + 1 + EPS)*tau, i*hx );
							beta[1] = (tau*hy*pls + HY2*U2_E[i+1][1])*_b11 + 
									_b12*(U2_E[i+2][1]*(1 + 1.0/(2.0*i)) - 2.*U2_E[i+1][1] + (1 - 1.0/(2.0*i))*U2_E[i][1]);
							
							for (j = 1; j < N; j++) {
								F = -2./tau*U2_E[i+1][j+1] - 
									OMEGA_SQ/HX2*( (1 + 1.0/(2.0*i) )*U2_E[i+2][j+1] - 2.*U2_E[i+1][j+1] + (1 - 1.0/(2.0*i) )*U2_E[i][j+1] );
						      	alpha[j+1] = c2/(b2-a2*alpha[j]);
						      	beta [j+1] = (F - a2*beta[j])/(a2*alpha[j] - b2);							
							}
												  
						    U1[i][N] = (tau*beta[N] + HY2*U2_E[i+1][N+1] 
						    		+ _c11*( 
						    		(1 + 1.0/(2.0*i))*U2_E[i+2][N+1] - 2.*U2_E[i+1][N+1] + (1 - 1.0/(2.0*i))*U2_E[i][N+1]
						    			   )
						    		        )
						    		/((1 - alpha[N] + hy*Bi2)*tau + HY2 );

						    for (j = N - 1; j >= 0; j--)
						    	U1[i][j] = alpha[j+1]*U1[i][j+1] + beta[j+1];
						      
						}
						
						//i = 0 boundary
						
						pls = discretePulse2D.evaluateAt( (m + 1 + EPS)*tau);
						beta[1] = (tau*hy*pls + HY2*U2_E[1][1])*_b11 + 2.0*_b12*( U2_E[2][1] - U2_E[1][1] );
					
						for (j = 1; j < N; j++) {
							F 		   = -2./tau*U2_E[1][j+1] - 2.0*OMEGA_SQ/HX2*( U2_E[2][j+1] - U2_E[1][j+1] );
					      	beta [j+1] = (F - a2*beta[j])/(a2*alpha[j] - b2);							
						}
						
					    U1[0][N] = (tau*beta[N] + HY2*U2_E[1][N+1] 
					    		+ 2.0*_c11*( U2_E[2][N+1] - U2_E[1][N+1] ) )
					    		/((1 - alpha[N] + hy*Bi2)*tau + HY2);

					    for (j = N - 1; j >= 0; j--)
					    	U1[0][j] = alpha[j+1]*U1[0][j+1] + beta[j+1];
					    					    
					}
				
					//calc average value

					double sum = 0;
					
					for (i = 0; i <= lastIndex; i++)
						sum += U1[i][N];
					
					sum /= (lastIndex + 1);
					curve.setTemperatureAt(w, sum);
					curve.setTimeAt(w, w*timeInterval*tau*problem.timeFactor());

					maxVal = max(maxVal, sum);			
					
				}					

				curve.scale( maxTemp/maxVal );
	});	
	
	public ADIScheme() {
		this(GRID_DENSITY, TAU_FACTOR);
	}	
	
	public ADIScheme(NumericProperty N, NumericProperty timeFactor) {
		super(N, timeFactor);
		grid = new Grid2D(N, timeFactor);	
		grid.setParent(this);
	}
	
	public ADIScheme(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
		this(N, timeFactor);
		setTimeLimit(timeLimit);
	}
	
	@Override
	public DifferenceScheme copy() {
		return new ADIScheme(grid.getGridDensity(),
				grid.getTimeFactor(), getTimeLimit());
	}
	
	@Override
	public String toString() {
		return Messages.getString("ADIScheme.4");
	}
	
	@Override
	public Solver<? extends Problem> solver(Problem problem) {
		if(problem instanceof LinearisedProblem2D)
			return linearisedSolver2D;
		else 
			return null;
	}
	
}