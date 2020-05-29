package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.MathUtils;

public class LobattoQuadrature extends LegendrePoly {

	public LobattoQuadrature(int n) {
		super(n);
		roots = new double[n + 1]; //n+1 for Lobatto
		weights = new double[n + 1]; //n+1 for Lobatto
	}
	
	@Override
	public void init() {
		coefficients();
		lobattoNodes();
		lobattoWeights();
	}
	
	private void lobattoWeights() {
		
		double denominator = 1;
		
		for(int i = 0; i < roots.length; i++) {
			denominator = n*(n + 1)*MathUtils.fastPowLoop(poly(roots[i]), 2);
			weights[i] = 2.0/denominator;
		}
		
	}
	
	private void lobattoNodes() {
		var dc = new double[c.length - 1];
		
		for(int i = 1; i < c.length; i++) 
			dc[i-1] = c[i]*i; 		
		
		var complexRoots = solver.solveAllComplex(dc, 1.0);

		//the last roots is always zero, so we have n non-zero roots in total
		//in case of even n, the first n/2 roots are positive and the rest are negative
		for(int i = 0; i < complexRoots.length; i++)  
			roots[i+1] = complexRoots[i].getReal();
		
		roots[0] = 1.0;
		roots[n/2] = roots[n/2] < 1E-10 ? 0.0 : roots[n/2];
		roots[complexRoots.length + 1] = -1.0;	
	}
	
    public static void main(String[] args) {
    	int n = 32;
    	var legendre = new LobattoQuadrature(n);
    	legendre.init();
    	legendre.lobattoNodes();
    	for(int i = 0; i < legendre.roots.length; i++) 
    		System.out.printf("%n%d %3.5f", i, legendre.roots[i]);
    }
    
	@Override
	public String getPrefix() {
		return "Lobatto Quadrature";
	}

}