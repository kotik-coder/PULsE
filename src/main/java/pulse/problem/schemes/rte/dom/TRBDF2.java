package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;

public class TRBDF2 extends AdaptiveIntegrator {

	public final static double gamma = 2.0 - Math.sqrt(2.0);
	public final static double w = Math.sqrt(2.0)/4.0;
	public final static double d = gamma/2.0;
	
	private double k1[];
	private double k2[];
	private double k3[];
	
	private static double bHat2;
	private static double bHat3;
	private static double bHat1;
	
	private int nStart;
	private int nHalf;
	
	static {
		bHat1 = (1.0 - w)/3.0;
		bHat2 = (3.0*w + 1.0)/3.0;
		bHat3 = d/3.0;
	}
	
	public TRBDF2(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		nHalf = intensities.quadratureSet.getFirstNegativeNode();
		nStart = intensities.quadratureSet.getFirstPositiveNode();
	}
	
	@Override
	public void integrate() {
		int N = intensities.grid.getDensity();
		
		Vector[] v;
		
		outer: for (double erSq = 1.0; erSq > adaptiveErSq; N = intensities.grid.getDensity()) {

			erSq = 0;

			treatZeroIndex();
			
			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(uExtended, emissionFunction); // initial value for tau = 0
			
			for (int j = 0, i; j < N && erSq < adaptiveErSq; j++) {
				v =	step(j, 1.0);
				erSq = Math.max( erSq, v[1].lengthSq() ); 
				for(i = nStart; i < nHalf; i++)
					intensities.I[i][j + 1] = v[0].get(i - nStart);
			}
			
			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(uExtended, emissionFunction); // initial value for tau = tau_0
			
			for (int j = N, i = 0; j > 0 && erSq < adaptiveErSq; j--) {
				v =	step(j, -1.0);
				erSq = Math.max( erSq, v[1].lengthSq() ); 
				for(i = nHalf; i < intensities.n; i++)
					intensities.I[i][j - 1] = v[0].get(i - nHalf);
			}
			
			System.out.printf("%n%3.7f %5d", Math.sqrt(erSq), N);
			
			if (erSq < adaptiveErSq)
				break outer;
			else
				reduceStepSize();

		}

	}
	
	public Vector[] step(int j, double sign) {
		
		double[][] aMatrix	= new double[nHalf-nStart][nHalf-nStart];
		double[] bVector	= new double[nHalf-nStart];
		
		Matrix invA;
		Vector i2, i3;
		
		k1 = new double[nHalf-nStart];
		k2 = new double[nHalf-nStart];
		k3 = new double[nHalf-nStart];
		
		double factor;
		
		double h		= sign*intensities.grid.step(j, sign); 
		int increment	= (int)(1*sign);
		double t		= intensities.grid.getNode(j); //TODO interpolate
		
		double halfAlbedo = getAlbedo()*0.5;
		
		int n1 = sign > 0 ? nStart : nHalf;
		int n2 = sign > 0 ? nHalf : intensities.n; 
		
		double[] est = new double[nHalf - nStart];
		
		/*
		 * Trapezoidal scheme
		 */
		
		for(int i = 0; i < aMatrix.length; i++) {
			
			k1[i] = super.rhs( i + n1, j, t, intensities.I[i + n1][j] );
			
			//b
			bVector[i] = intensities.I[i + n1][j] + h*d*( k1[i] + ( super.sourceEmission(t + gamma*h) 
					+ halfAlbedo*ipf.integratePartial(i + n1, j, intensities.n - n2, intensities.n - n1) )
					/intensities.mu[i + n1] ); //TODO ipf(i, j + gamma
			
			factor = -h*d*halfAlbedo/intensities.mu[i + n1];
			
			//all elements
			for(int k = 0; k < aMatrix[0].length; k++)
					aMatrix[i][k] = factor*intensities.w[k + n1]*ipf.function(i + n1, k + n1);
			
			//additionally for the diagonal elements
			aMatrix[i][i] += 1.0 + h*d/intensities.mu[i + n1];
		
		}
		
		invA = ( new Matrix(aMatrix) ).inverse();
		
		i2 = invA.multiply( new Vector(bVector) );
		
		/*
		 * + BDF2
		 */
		
		for(int i = 0; i < aMatrix.length; i++) {
		
			bVector[i] = intensities.I[i + n1][j]*(1.0 - w/d) + w/d*i2.get(i) + d*h/intensities.mu[i + n1]*(
						super.sourceEmission(t + h) + halfAlbedo*ipf.integratePartial(i + n1, j + increment, intensities.n - n2, intensities.n - n1) );	
			k2[i] = (i2.get(i) - intensities.I[i + n1][j])/(d*h) - k1[i];
		}
		
		i3 = invA.multiply( new Vector(bVector) );
	
		for(int i = 0; i < aMatrix.length; i++) {
			k3[i] = (i3.get(i) - intensities.I[i + n1][j] - w/d*(i2.get(i) - intensities.I[i + n1][j]) )/(d*h);
			est[i] += ( (w - bHat1)*k1[i] + (w - bHat2)*k2[i] + (d - bHat3)*k3[i] )*h;
		}
		
		return new Vector[] { i3, invA.multiply( new Vector(est) ) };
		
	}
	
}