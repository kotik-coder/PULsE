package pulse.problem.schemes;

import static java.lang.Math.pow;
import pulse.HeatingCurve;
import pulse.problem.statements.LinearisedProblem2D;
import pulse.problem.statements.NonlinearProblem2D;
import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

import static java.lang.Math.abs;
import static java.lang.Math.PI;
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

				final double Bi3	= (double) problem.getSideLosses().getValue();
				final double d		= (double) problem.getSampleDiameter().getValue();
				final double dAv	= (double) problem.getPyrometerSpot().getValue();
				
				final double l			= (double) problem.getSampleThickness().getValue();
				final double Bi1		= (double) problem.getFrontHeatLoss().getValue();
				final double Bi2 		= (double) problem.getHeatLossRear().getValue();
				final double maxTemp 	= (double) problem.getMaximumTemperature().getValue();
				
				//end

				double[][] U1	= new double[grid.N + 1][grid.N + 1];
				double[][] U2	= new double[grid.N + 1][grid.N + 1];
				double[] alpha  = new double[grid.N + 2];
				double[] beta   = new double[grid.N + 2];
				double[] beta1  = new double[grid.N + 2];
				double[] beta2  = new double[grid.N + 2];
				
				double[] a1 = new double[grid.N + 1];
				double[] b1 = new double[grid.N + 1];
				double[] c1 = new double[grid.N + 1];
				
				final double EPS = 1e-8;
				
				HeatingCurve curve = problem.getHeatingCurve();
				curve.reinit();
				
				final int counts = (int) curve.getNumPoints().getValue();
				
				double maxVal = 0;
				int i, j, m, w;
				double pls;

				double hy = ((Grid2D)getGrid()).hy;
				double hx = grid.hx;
				
				//precalculated constants				

				double D2    = pow(d,2); 
				double L2    = pow(l,2); 
				double HX2   = pow(hx,2); 
				double HY2   = pow(hy,2);
				double D2HX2 = D2*HX2; 
				double L2TAU = L2*grid.tau;
				
				//a[i]*u[i-1] - b[i]*u[i] + c[i]*u[i+1] = F[i]

				for(i = 1; i < grid.N + 1; i++) {
					a1[i] = (L2/D2)*(4.*i - 2)/HX2/i;
				    b1[i] = 2./grid.tau + 8.*(L2/D2)/HX2;
				    c1[i] = (L2/D2)*(4.*i + 2)/HX2/i;	
				}
				
				double a2 = 1./HY2;
				double b2 = 2./HY2 + 2./grid.tau;
				double c2 = 1./HY2;
				
				double F, F1, F2;
				
				int lastIndex = (int)(dAv/d/hx);
				lastIndex 	  = lastIndex > grid.N ? grid.N : lastIndex;
				
				//precalc coefs

				double a11 = 4.*L2TAU/(D2HX2 + 4.*L2TAU);
				double b11 = 0.5*grid.tau*D2HX2/(D2HX2 + 4.*L2TAU);
				double c11 = Bi3*hx*d*grid.tau*l*(2.*grid.N + 1)/grid.N;
				double b21 = grid.tau*D2HX2/(D2HX2 + 4.*L2TAU);
				double b22 = 1./grid.tau - ( 1 + Bi1*hy )/HY2;
				double b31 = grid.tau*D2HX2/(D2HX2 + 4.*L2TAU);
				double b32 = 1./grid.tau - ( 1 + Bi2*hy )/HY2;
				double f11 = 1./grid.tau - ( 1 + Bi1*hy )/HY2;
				double f21 = 1./grid.tau - ( 1 + Bi2*hy )/HY2;
				double c21 = Bi3*hx*d*grid.tau*l*(2.*grid.N + 1)/grid.N;
				double c22 = 1./grid.tau - (1 + Bi1*hy)/HY2;
				double c31 = Bi3*hx*d*grid.tau*l*(2.*grid.N + 1)/grid.N;
				double c32 = 1./grid.tau - (1 + Bi2*hy)/HY2 ;

				double _a11 = grid.tau/(Bi1*hy*grid.tau + HY2 + grid.tau);
				double _b11 = 1./((1 + hy*Bi1)*grid.tau + HY2);
				double _b12 = 2.*HY2*grid.tau/((1 + hy*Bi1)*grid.tau + HY2)*pow(l/d/hx,2);
				double _c11 = 2.*grid.tau*pow(hy*l/d/hx,2);
				double _b21 = 1./((1 + hy*Bi1)*grid.tau + HY2);
				double _b22 = HY2*grid.tau/((1 + hy*Bi1)*grid.tau + HY2)*pow(2.*l/d/hx,2);
				double _b31 = 1./((1 + hy*Bi1)*grid.tau + HY2);
				double _b32 = - HY2*grid.tau/((1 + hy*Bi1)*grid.tau + HY2)*pow(2.*l/d/hx,2);
				double _b33 = 1 + hx*d/2./l*Bi3*(2.*grid.N + 1)/2./grid.N;
				double _f21 = 1./grid.tau - 4.*L2/D2HX2;
				double _f22 = 4.*L2/D2HX2;
				double _f31 = 1./grid.tau - pow(2.*l/d/hx, 2)*(hx*d/2./l*Bi3*(2.*grid.N + 1)/2./grid.N + 1);
				double _f32 = pow(2.*l/d/hx, 2);
				double _c21 = HY2*grid.tau*pow(2.*l/d/hx, 2);
				double _c31 = - grid.tau*pow(2.*l*hy/d/hx, 2);
				double _c32 = hx*d/2./l*(2.*grid.N + 1)/2./grid.N*Bi3 + 1;
				double _c33;

				//end of coefs			
				
				//begin time cycle
				
				for (w = 1; w < counts; w++) {
					
					for (m = (w - 1)*timeInterval; m < w*timeInterval; m++) {
						
						//first equation, i -> x (radius), j -> y (thickness)
						
						alpha[1] = a11;
						
						for (j = 1; j < grid.N; j++) {
							
							beta[1]  = b11*(2.*U1[0][j]/grid.tau + (U1[0][j+1] - 2.*U1[0][j] + U1[0][j-1])/HY2);

							for (i = 1; i < grid.N; i++) {
								F  		   = -(2./grid.tau*U1[i][j] + (U1[i][j-1] - 2.0*U1[i][j] + U1[i][j+1])/HY2);
							    alpha[i+1] = c1[i]/(b1[i]-a1[i]*alpha[i]);
							    beta[i+1]  = (F - a1[i]*beta[i])/(a1[i]*alpha[i] - b1[i]);	
							}

						    U2[grid.N][j] = grid.tau/(4.*L2TAU*(1 - alpha[grid.N]) + D2HX2 + c11)*
						    (4.*L2*beta[grid.N] + 0.5*D2HX2*( 2./grid.tau*U1[grid.N][j] + (U1[grid.N][j+1] - 2.*U1[grid.N][j] + U1[grid.N][j-1])/HY2 ) );

						    for (i = grid.N - 1; i >= 0; i--)
						    	U2[i][j] = alpha[i+1]*U2[i+1][j] + beta[i+1];
							
						}
						    
						//boundary : j = 0 (j = grid.N), m = 1/2, i = 1 to grid.N-1

						pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau );

						beta1[1] = b21*( b22*U1[0][0] + U1[0][1]/HY2 + pls/hy );
						beta2[1] = b31*( b32*U1[0][grid.N] + U1[0][grid.N-1]/HY2 );
						
						for (i = 1; i < grid.N; i++) {
							pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau, i*hx );	
							F1  = -2.*( f11*U1[i][0] + U1[i][1]/HY2 + pls/hy);
					    	F2  = -2.*( f21*U1[i][grid.N] + U1[i][grid.N-1]/HY2);
					    	beta1[i+1] = (F1 - a1[i]*beta1[i])/(a1[i]*alpha[i] - b1[i]);
					    	beta2[i+1] = (F2 - a1[i]*beta2[i])/(a1[i]*alpha[i] - b1[i]);	
						}

						pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau, (grid.N - EPS)*hx );		

						U2[grid.N][0] = grid.tau/(4.*L2TAU*(1 - alpha[grid.N]) + D2HX2 + c21 )*(4.*L2*beta1[grid.N] + D2HX2*( c22*U1[grid.N][0] + U1[grid.N][1]/HY2 + pls/hy ));
						U2[grid.N][grid.N] = grid.tau/(4.*L2TAU*(1 - alpha[grid.N]) + D2HX2 + c31 )*(4.*L2*beta2[grid.N] + D2HX2*( c32*U1[grid.N][grid.N] + U1[grid.N][grid.N-1]/HY2 ));

						for (i = grid.N - 1; i >= 0; i--) {
							U2[i][0] = alpha[i+1]*U2[i+1][0] + beta1[i+1];
						    U2[i][grid.N] = alpha[i+1]*U2[i+1][grid.N] + beta2[i+1];						
						}
						
						//second equation
						
						alpha[1] = _a11;

						for (i = 1; i < grid.N; i++) {
							
							pls = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau, i*hx );
							beta[1] = (grid.tau*hy*pls + HY2*U2[i][0])*_b11 + _b12*(U2[i+1][0]*(1 + 0.5/i) - 2.*U2[i][0] + (1 - 0.5/i)*U2[i-1][0]);
							
							for (j = 1; j < grid.N; j++) {
								F          = -2./grid.tau*U2[i][j] - 4.*L2/D2HX2*( (1 + 0.5/i)*U2[i+1][j] - 2.*U2[i][j] + (1 - 0.5/i)*U2[i-1][j] );
						      	alpha[j+1] = c2/(b2-a2*alpha[j]);
						      	beta [j+1] = (F - a2*beta[j])/(a2*alpha[j] - b2);							
							}
												  
						    U1[i][grid.N] = (grid.tau*beta[grid.N] + HY2*U2[i][grid.N] + _c11*((1 + 0.5/i)*U2[i+1][grid.N] - 2.*U2[i][grid.N] + (1 - 0.5/i)*U2[i-1][grid.N]))
						    /((1 - alpha[grid.N] + hy*Bi2)*grid.tau + HY2);

						    for (j = grid.N - 1; j >= 0; j--)
						    	U1[i][j] = alpha[j+1]*U1[i][j+1] + beta[j+1];
						      
						}										

						//boundary : i = 0 (i = grid.N), m = 1/2, j = 1 to grid.N-1

						// i = 0, j = 0

						pls 	 = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau);
						beta1[1] = _b21*(grid.tau*hy*pls + HY2*U2[0][0]) + _b22*(U2[1][0] - U2[0][0]);

						// i = grid.N, j = 1

						pls 	 = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau, (grid.N - EPS)*hx );
						beta2[1] = _b31*(grid.tau*hy*pls + HY2*U2[grid.N][0]) + _b32*(U2[grid.N][0]*_b33 - U2[grid.N-1][0]);
						
						for (j = 1; j < grid.N; j++) {
							F1		   = -2.*( _f21*U2[0][j] + _f22*U2[1][j] );
						    F2		   = -2.*( _f31*U2[grid.N][j] + _f32*U2[grid.N-1][j] );
						    beta1[j+1] = (F1 - a2*beta1[j])/(a2*alpha[j] - b2);
						    beta2[j+1] = (F2 - a2*beta2[j])/(a2*alpha[j] - b2);						
						}

						_c33 = 1./((1 - alpha[grid.N] + hy*Bi2)*grid.tau + HY2);

						U1[0][grid.N] = (grid.tau*beta1[grid.N] + HY2*U2[0][grid.N] + _c21*(U2[1][grid.N] - U2[0][grid.N]))*_c33;
						U1[grid.N][grid.N] = (grid.tau*beta2[grid.N] + HY2*U2[grid.N][grid.N] + _c31*(_c32*U2[grid.N][grid.N] - U2[grid.N-1][grid.N]))*_c33;

						for (j = grid.N - 1; j >= 0; j--) {
							U1[0][j] = alpha[j+1]*U1[0][j+1] + beta1[j+1];
						    U1[grid.N][j] = alpha[j+1]*U1[grid.N][j+1] + beta2[j+1];	
						}				
						
						
					}
					
					//calc average value

					double sum = 0;
					
					for (i = 0; i <= lastIndex; i++)
						sum += U1[i][grid.N];
					
					sum /= (lastIndex + 1);
					curve.setTemperatureAt(w, sum);
					curve.setTimeAt(w, w*timeInterval*grid.tau*problem.timeFactor());

					maxVal = max(maxVal, sum);			
					
				}					

				curve.scale( maxTemp/maxVal );
			});
	
	public Solver<NonlinearProblem2D> nonlinearSolver2D = (problem -> {
		super.prepare(problem);
		
		//quick links

		DiscretePulse2D discretePulse2D = (DiscretePulse2D)discretePulse;
		
		final Pulse pulse 					= problem.getPulse();

		final double Bi3 	= (double) problem.getSideLosses().getValue();
		final double d 		= (double) problem.getSampleDiameter().getValue();
		final double dAv 	= (double) problem.getPyrometerSpot().getValue();
		
		final double l = (double) problem.getSampleThickness().getValue();
		final double Bi1 = (double) problem.getFrontHeatLoss().getValue();
		final double Bi2 = (double) problem.getHeatLossRear().getValue();
		final double maxTemp = (double) problem.getMaximumTemperature().getValue();
		
		//end

		double[][] U1 = new double[grid.N + 1][grid.N + 1];
		double[][] U2 = new double[grid.N + 1][grid.N + 1];
		double[] alpha  = new double[grid.N + 2];
		double[] beta   = new double[grid.N + 2];
		double[] beta1  = new double[grid.N + 2];
		double[] beta2  = new double[grid.N + 2];
		
		double[] a1 = new double[grid.N + 1];
		double[] b1 = new double[grid.N + 1];
		double[] c1 = new double[grid.N + 1];
		
		final double EPS = 1e-8;
		
		double hy = ((Grid2D)getGrid()).hy;
		double hx = grid.hx;
		
		HeatingCurve curve = problem.getHeatingCurve();
		curve.reinit();
		
		final int counts = (int) curve.getNumPoints().getValue();
		
		double maxVal = 0;
		int i, j, m, w;
		double pls;

		//precalculated constants				

		double D2    = pow(d,2); 
		double L2    = pow(l,2); 
		double HX2   = pow(hx,2); 
		double HY2   = pow(hy,2);
		double D2HX2 = D2*HX2; 
		double L2TAU = L2*grid.tau;
		
		//a[i]*u[i-1] - b[i]*u[i] + c[i]*u[i+1] = F[i]

		for(i = 1; i < grid.N + 1; i++) {
			a1[i] = (L2/D2)*(4.*i - 2)/HX2/i;
		    b1[i] = 2./grid.tau + 8.*(L2/D2)/HX2;
		    c1[i] = (L2/D2)*(4.*i + 2)/HX2/i;	
		}
		
		double a2 = 1./HY2;
		double b2 = 2./HY2 + 2./grid.tau;
		double c2 = 1./HY2;
		
		double F, F1, F2;
		
		int lastIndex = (int)(dAv/d/hx);
		lastIndex 	  = lastIndex > grid.N ? grid.N : lastIndex;
		
		/***/
		
		final double T   	   = (double) problem.getTestTemperature().getValue();
		final double rho 	   = (double) problem.getDensity().getValue();
		final double cV  	   = (double) problem.getSpecificHeat().getValue();
		final double qAbs 	   = (double) problem.getAbsorbedEnergy().getValue();
		final double nonlinearPrecision = (double) problem.getNonlinearPrecision().getValue(); 
		
		//precalc coefs
		
		double pulseWidth = (double)pulse.getSpotDiameter().getValue();

		double ENERGY = qAbs/(PI*l*pow(pulseWidth,2)*rho*cV);

		double a11 = 4.*L2TAU/(4.*L2TAU + D2HX2);
		
		double b11 = 0.5*D2HX2*grid.tau/(4.*L2TAU + D2HX2);
		double b21 = D2HX2*grid.tau/(4.*L2TAU + D2HX2);
		double b22 = 1./grid.tau - 1./HY2;
		double b23 = -Bi1*T/4./hy;
		double b24 = ENERGY/hy;
		double b31 = D2HX2*grid.tau/(4.*L2TAU + D2HX2);
		double b32 = 1./grid.tau - 1./HY2;
		double b33 = -Bi2*T/4./hy;
		
		double c11 = -0.5*grid.tau*d*hx*l*(2.*grid.N + 1)/2./grid.N*Bi3*T;
		double c12 = 0.5*grid.tau*D2HX2;
		double c13;
		double c21 = -0.5*grid.tau*d*hx*l*(2.*grid.N + 1)/2./grid.N*Bi3*T;
		double c22 = (1./grid.tau - 1./HY2);
		double c23 = -Bi1*T/4./hy;
		double c24 = ENERGY/hy;
		double c25;
		double c31 = -0.5*grid.tau*d*hx*l*(2.*grid.N + 1)/2./grid.N*Bi3*T;
		double c32 = (1./grid.tau - 1./HY2);
		double c33 = -Bi2*T/4./hy;
					
		double f11 = (1./grid.tau - 1/HY2);
		double f12 = ENERGY/hy;
		double f13 = -0.25/hy*Bi1*T;
		double f21 = (1./grid.tau - 1/HY2);
		double f22 = -0.25/hy*Bi2*T;
								
		double _a11 = grid.tau/(grid.tau + HY2);
		
		double _b11 = hy*grid.tau/(HY2 + grid.tau)*ENERGY;
		double _b12 = -0.25*hy*grid.tau/(HY2 + grid.tau)*Bi1*T;
		double _b13 = HY2/(HY2 + grid.tau);
		double _b14 = 2.*grid.tau/(HY2 + grid.tau)*pow(l*hy/d/hx, 2);
		double _b21 = hy*grid.tau/(HY2 + grid.tau)*ENERGY;
		double _b22 = -0.25*hy*grid.tau/(HY2 + grid.tau)*Bi1*T;
		double _b23 = HY2/(HY2 + grid.tau);
		double _b24 = -grid.tau/(HY2 + grid.tau)*pow(2.*hy*l/d/hx, 2);
		double _b31 = hy*grid.tau/(HY2 + grid.tau)*ENERGY;
		double _b32 = -0.25*hy*grid.tau/(HY2 + grid.tau)*Bi1*T;
		double _b33 = HY2/(HY2 + grid.tau);
		double _b34 = -grid.tau/(HY2 + grid.tau)*pow(2.*hy*l/d/hx, 2);
		double _b35 = hx*d/8./l*Bi3*T*(2.*grid.N + 1)/2./grid.N;
		
		double _c11 = -0.25*grid.tau*hy*Bi2*T;
		double _c12 = 2.*grid.tau*pow(l*hy/d/hx, 2);
		double _c13;
		double _c21 = -0.25*grid.tau*hy*Bi2*T;
		double _c22 = grid.tau*pow(2.*hy*l/hx/d, 2);
		double _c23;
		double _c31 = -0.25*grid.tau*hy*Bi2*T;
		double _c32 = -grid.tau*pow(2.*hy*l/hx/d, 2);
		double _c33 = hx*d/8./l*Bi3*T*(2.*grid.N + 1)/2./grid.N;
					
		double _f11 = (1./grid.tau - 4.*L2/D2HX2);
		double _f12 = 4.*L2/D2HX2;
		double _f21 = (1./grid.tau - 4.*L2/D2HX2);
		double _f22 = 4.*L2/D2HX2;
		double _f23 = -0.25*l/d/hx*(2.*grid.N + 1)/grid.N*Bi3*T;
		
		double pls1, pls2;

		//end of coefs
		
		//begin time cycle
		
		for (w = 1; w < counts; w++) {
			
			for (m = (w - 1)*timeInterval; m < w*timeInterval; m++) {
				
				//first equation
				
				alpha[1] = a11;
				
				for (j = 1; j < grid.N; j++) {
					
					beta[1]  = b11*(2.*U1[0][j]/grid.tau + (U1[0][j+1] - 2.*U1[0][j] + U1[0][j-1])/HY2);

					for (i = 1; i < grid.N; i++) {
						F  		   = -(2./grid.tau*U1[i][j] + (U1[i][j-1] - 2.0*U1[i][j] + U1[i][j+1])/HY2);
					    alpha[i+1] = c1[i]/(b1[i]-a1[i]*alpha[i]);
					    beta[i+1]  = (F - a1[i]*beta[i])/(a1[i]*alpha[i] - b1[i]);	
					}

				    c13 = 1./(D2HX2 + 4.*L2TAU*(1 - alpha[grid.N]) );
				    
				    for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
				        diff = -U2[grid.N][j];
				    	U2[grid.N][j]  = 4.*L2TAU*c13*beta[grid.N] + c13*c11*( pow(U2[grid.N][j]/T + 1, 4) - 1)
				        	      + c12*c13*(2./grid.tau*U1[grid.N][j] + (U1[grid.N][j+1] - 2.*U1[grid.N][j] + U1[grid.N][j-1])/HY2);
				        diff += U2[grid.N][j];					    	
				    }

				    for (i = grid.N - 1; i >= 0; i--)
				    	U2[i][j] = alpha[i+1]*U2[i+1][j] + beta[i+1];
					
				}
				    
				//boundary : j = 0 (j = grid.N), m = 1/2, i = 1 to grid.N-1

				pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau );

				beta1[1] = b21*(b22*U1[0][0] + U1[0][1]/HY2 + b23*( pow(U1[0][0]/T + 1, 4) - 1) + b24*pls);
				beta2[1] = b31*(b32*U1[0][grid.N] + U1[0][grid.N-1]/HY2 + b33*( pow(U1[0][grid.N]/T + 1, 4) - 1));
				
				for (i = 1; i < grid.N; i++) {
					pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau, i*hx );
				    F1  = -2.*( f11*U1[i][0] + U1[i][1]/HY2 + f12*pls + f13*( pow(U1[i][0]/T + 1, 4) - 1));
			    	F2  = -2.*( f21*U1[i][grid.N] + U1[i][grid.N-1]/HY2 + f22*( pow(U1[i][grid.N]/T + 1, 4) - 1));
			    	beta1[i+1]  = (F1 - a1[i]*beta1[i])/(a1[i]*alpha[i] - b1[i]);
			    	beta2[i+1]  = (F2 - a1[i]*beta2[i])/(a1[i]*alpha[i] - b1[i]);	
				}

				c25 = 1./( D2HX2 + (1 - alpha[grid.N])*4.*L2TAU );
				
				pls = discretePulse2D.evaluateAt( (m - EPS)*grid.tau, (grid.N - EPS)*hx );			

				for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
				  diff = -0.5*U2[grid.N][0] - 0.5*U2[grid.N][grid.N];

				  U2[grid.N][0] = 4.*L2TAU*c25*beta1[grid.N] + c25*c21*( pow(U2[grid.N][0]/T + 1, 4) - 1)
				  + grid.tau*c25*D2HX2*( c22*U1[grid.N][0] + U1[grid.N][1]/HY2 + c23*( pow(U1[grid.N][0]/T + 1, 4) - 1) + c24*pls );

				  U2[grid.N][grid.N] = 4.*L2TAU*c25*beta2[grid.N] + c25*c31*( pow(U2[grid.N][grid.N]/T + 1, 4) - 1)
				  + grid.tau*c25*D2HX2*( c32*U1[grid.N][grid.N] + U1[grid.N][grid.N-1]/HY2 + c33*( pow(U1[grid.N][grid.N]/T + 1, 4) - 1));

				  diff += 0.5*U2[grid.N][0] + 0.5*U2[grid.N][grid.N];
					
				}										

				for (i = grid.N - 1; i >= 0; i--) {
					U2[i][0] = alpha[i+1]*U2[i+1][0] + beta1[i+1];
				    U2[i][grid.N] = alpha[i+1]*U2[i+1][grid.N] + beta2[i+1];						
				}
				
				//second equation
				
				alpha[1] = _a11;

				for (i = 1; i < grid.N; i++) {
					
					pls = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau, i*hx );
					
					for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {
						
						beta[1] = _b11*pls + _b12*(pow(U1[i][0]/T + 1, 4) - 1) + _b13*U2[i][0] +
					    	      _b14*( U2[i+1][0]*(1 + 0.5/i) - 2.*U2[i][0] + U2[i-1][0]*(1 - 0.5/i));

						for (j = 1; j < grid.N; j++) {
							F		   = -2./grid.tau*U2[i][j] - 2.*L2/D2HX2/i*( (2.*i + 1)*U2[i+1][j] - 4.*i*U2[i][j] + (2*i - 1)* U2[i-1][j] );
			    	      	alpha[j+1] = c2/(b2 - a2*alpha[j]);
			    	      	beta [j+1] = (F - a2*beta[j])/(a2*alpha[j] - b2);	
						}							

					    _c13 = 1./(HY2 - alpha[grid.N]*grid.tau + grid.tau);

					    diff = -0.5*U1[i][0] - 0.5*U1[i][grid.N];

					    U1[i][grid.N] = _c13*grid.tau*beta[grid.N] + _c13*HY2*U2[i][grid.N] + _c13*_c11*(pow(U1[i][grid.N]/T + 1, 4) - 1)
					    	     + _c13*_c12*(  U2[i+1][grid.N]*(1 + 0.5/i) - 2.*U2[i][grid.N] + U2[i-1][grid.N]*(1 - 0.5/i) );

					    for (j = grid.N-1; j >= 0; j--)
					    	U1[i][j] = alpha[j+1]*U1[i][j+1] + beta[j+1];

					    diff += 0.5*U1[i][0] + 0.5*U1[i][grid.N];	
						
					}						
				      
				}										

				//boundary : i = 0 (i = grid.N), m = 1/2, j = 1 to grid.N-1

			    pls1 = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau);
			    pls2 = discretePulse2D.evaluateAt( (m + 1 + EPS)*grid.tau, (grid.N - EPS)*hx );

			    for(double diff = 100; abs(diff)/maxTemp > nonlinearPrecision; ) {

			    // i = 0, j = 0
			    beta1[1] = _b21*pls1 + _b22*(pow(U1[0][0]/T + 1, 4) - 1) + _b23*U2[0][0] + _b24*(U2[0][0] - U2[1][0]);

			    // i = grid.N, j = 1
			    beta2[1] = _b31*pls2 + _b32*(pow(U1[grid.N][0]/T + 1, 4) - 1) + _b33*U2[grid.N][0]
			    + _b34*( _b35*(pow(U2[grid.N][0]/T + 1, 4) - 1  ) + U2[grid.N][0] - U2[grid.N-1][0] ) ;

			    for (j = 1; j < grid.N; j++) {
			      F1 = -2.*( _f11*U2[0][j] + _f12*U2[1][j] );
			      F2 = -2.*( _f21*U2[grid.N][j] + _f22*U2[grid.N-1][j] + _f23*(pow(U2[grid.N][j]/T + 1, 4) - 1) );
			      beta1[j+1] = (F1 - a2*beta1[j])/(a2*alpha[j] - b2);
			      beta2[j+1] = (F2 - a2*beta2[j])/(a2*alpha[j] - b2);
			    }

			    _c23 = 1./(HY2 - alpha[grid.N]*grid.tau + grid.tau);

			    diff = -0.5*U1[0][grid.N] - 0.5*U1[grid.N][grid.N];

			    U1[0][grid.N] = grid.tau*beta1[grid.N]*_c23 + HY2*_c23*U2[0][grid.N]
			    + _c21*_c23*(pow(U1[0][grid.N]/T + 1, 4) - 1) + _c22*_c23*(U2[1][grid.N] - U2[0][grid.N]);

			    U1[grid.N][grid.N] = grid.tau*beta2[grid.N]*_c23 + HY2*U2[grid.N][grid.N]*_c23 +
			    _c31*_c23*(pow(U1[grid.N][grid.N]/T + 1, 4) - 1)  + _c32*_c23*
			    (_c33*(pow(U2[grid.N][grid.N]/T + 1, 4) - 1) + U2[grid.N][grid.N] - U2[grid.N-1][grid.N]);

			    diff += 0.5*U1[0][grid.N] + 0.5*U1[grid.N][grid.N];

			    }

			    for (j = grid.N-1; j >= 0; j--) {
			      U1[0][j] = alpha[j+1]*U1[0][j+1] + beta1[j+1];
			      U1[grid.N][j] = alpha[j+1]*U1[grid.N][j+1] + beta2[j+1];
			    }
				
				
			}
			
			//calc average value

			double sum = 0;
			
			for (i = 0; i <= lastIndex; i++)
				sum += U1[i][grid.N];
			
			sum /= (lastIndex + 1);
			curve.setTemperatureAt(w, sum);
			curve.setTimeAt(w, w*timeInterval*grid.tau*problem.timeFactor());

			maxVal = max(maxVal, sum);	
			
			
		}					

		problem.setMaximumTemperature(NumericProperty.derive(NumericPropertyKeyword.MAXTEMP, maxVal));

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
		else if(problem instanceof NonlinearProblem2D)
			return nonlinearSolver2D;
		else 
			return null;
	}
	
}