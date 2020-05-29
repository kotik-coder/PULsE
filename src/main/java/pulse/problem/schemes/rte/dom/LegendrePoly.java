package pulse.problem.schemes.rte.dom;

import java.util.List;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;

import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public abstract class LegendrePoly extends PropertyHolder {

	protected double[] roots;
	protected double[] c;
	protected double[] weights;
	protected int n;
	protected LaguerreSolver solver;
	protected double solverError;
	
	public LegendrePoly(final int n) {
		this.n = n;
		c = new double[n+1];
		this.solverError = (double)NumericProperty.theDefault(NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR).getValue();
		solver = new LaguerreSolver(solverError);
	}
	
	public abstract void init();
		
	public double poly(double x) {
		double poly = 0;
		
		for(int i = 0; i < c.length; i++)
			poly += c[i]*MathUtils.fastPowLoop(x, i);
		
		return poly;
		
	}
	
	public double derivative(double x) {
		double d = 0;
		
		for(int i = 1; i < c.length; i++)
			d += i*c[i]*MathUtils.fastPowLoop(x, i-1);
		
		return d;
		
	}
			
	/**
	 * This will generate the coefficients for the Legendre polynomial,
	 * arranged in order of significance (from x^0 to x^n).
	 */
	
	public void coefficients() {
		
		long intFactor = MathUtils.fastPowInt(2, n);
		
		for(int i = 0; i < c.length; i++) 
			c[i] = intFactor*binomial(n,i)*generalisedBinomial((n+i-1)*0.5,n);
			
	}
	
	/**
	 * Fast calculation of binomial coefficient C_m^k = m!/(k!(m-k)!)
	 * @param m integer.
	 * @param k integer, k <= m.
	 * @return binomial coefficient C_m^k.
	 */
	
    public static long binomial(int m, int k) 
    { 
    	int k1 = m - k;
    	k = k > k1 ? k1 : k;
    	
    	long c = 1;
    	
	    // Calculate  value of Binomial Coefficient in bottom up manner 
	    for (int i = 1; i < k + 1; i++, m--) 
	    	c = c / i * m + c % i * m / i;  // split c * n / i into (c / i * i + c % i) * n / i
	    
	    return c;
	    
    }
    
	/**
	 * Fast calculation of binomial coefficient C_m^k = m!/(k!(m-k)!)
	 * @param m integer.
	 * @param k integer, k <= m.
	 * @return binomial coefficient C_m^k.
	 */
	
    public static double generalisedBinomial(double alpha, int k) 
    { 
    	
    	double c = 1;
    	
    	for(int i = 0; i < k; i++) 
    		c *= (alpha - i)/(k - i);
	    
    	return c;
    	
    } 
    
    public NumericProperty getSolverError() {
    	return NumericProperty.derive(NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR, solverError);
    }
    
    public void setSolverError(NumericProperty solverError) {
    	if(solverError.getType() != NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR)
    		throw new IllegalArgumentException("Illeagl type: " + solverError.getType());
    	this.solverError = (double)solverError.getValue(); 
    	solver = new LaguerreSolver(this.solverError);
    }
    
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
			case LAGUERRE_SOLVER_ERROR : 
				this.setSolverError(property); 
				break;
			default : return;
		}
		
		notifyListeners(this, property);
		 
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.LAGUERRE_SOLVER_ERROR));
		return list;				
	}
    
	public double[] getRoots() {
		return roots;
	}

	public double[] getWeights() {
		return weights;
	}
	
	@Override
	public String getPrefix() {
		return "Legendre Polynomial";
	}
	
	public int getOrder() {
		return n;
	}

}