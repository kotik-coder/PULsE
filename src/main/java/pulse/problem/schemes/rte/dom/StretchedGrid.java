package pulse.problem.schemes.rte.dom;

import java.util.Arrays;

public class StretchedGrid {

	private double[] nodes;
	private LegendrePoly generatingPolynom;
	
	private final static double STRETCHING_FACTOR = 1.0;
	private final static int GRID_DENSITY = 16;
	
	private double stretchingFactor;
	private double dimension;
	
	public StretchedGrid(double dimension) {
		this(GRID_DENSITY, dimension, STRETCHING_FACTOR);
	}
	
	public StretchedGrid(int n, double dimension) {
		this(n, dimension, STRETCHING_FACTOR);
	}
	
	public StretchedGrid(int n, double dimension, double stretchingFactor) {
		this.stretchingFactor = stretchingFactor;
		this.dimension = dimension;
		if(Double.compare(stretchingFactor, 1.0) == 0)
			generateUniform(n, true);
		else
			generate(n);
	}
	
	public double[] getNodes() {
		return nodes;
	}
	
	public int getDensity() {
		return nodes.length - 1;
	}
	
	public double sinh(double x, double stretchingFactor) {
		return Math.sinh(stretchingFactor*x)/Math.sinh(stretchingFactor);
	}
	
	public double tanh(double x, double stretchingFactor) {
		return 1.0 - Math.tanh(stretchingFactor*(1.0 - x))/Math.tanh(stretchingFactor);
	}
	
	public void generate(int n) {
		generateUniform(n, false);
		double[] uniform = new double[nodes.length];
		System.arraycopy(nodes, 0, uniform, 0, nodes.length);
		nodes = new double[n + 1];
		
		//apply stretching function
		for(int i = 0; i < uniform.length; i++) 
			nodes[i] = dimension*sinh(uniform[i], stretchingFactor);
		
	}
	
	private void generateLobatto(int n, double dimension) { 
		generatingPolynom = new LobattoQuadrature(n);
		generatingPolynom.init();
		int N = generatingPolynom.getRoots().length;
		nodes = new double[N];
		
		for(int i = 0; i < N; i++)
			nodes[i] = 0.5*(generatingPolynom.getRoots()[i] + 1.0)*dimension;

		Arrays.sort(nodes);
		
	}
	
	public void generateUniform(int n, boolean scaled) {
		nodes = new double[n + 1];
		double h = 1.0/((double)n);
		
		for(int i = 0; i < n + 1; i++)
			nodes[i] = i*h;
		for(int i = 0; (i < n + 1) && scaled; i++)
			nodes[i] *= dimension;

	}
	
	/**
	 * Assumes uniform grid
	 * @param array
	 * @param hx
	 * @param index
	 * @return
	 */
	
	public double interpolateUniform(double[] array, double hx, int index) {
		double t = index*hx*dimension;
		double h = nodes[1] - nodes[0];
		int floor = (int)(t/h);
		double alpha = t - floor*h;
		return (1.0 - alpha)*array[floor] + alpha*array[floor + 1];				
	}
	
	public double interpolateStretched(double[] array, double hx, int index) {
		double t = index*hx*dimension;
		
		//loops through nodes sorted in ascending order
		for(int i = 0; i < nodes.length; i++) 
			/* if node is greater than t, then the associated function
			 * can be interpolated between points f_i and f_i-1, since
			 * t lies between nodes n_i and n_i-1
			*/
			if(nodes[i] > t) {
				double mu = (t - nodes[i - 1])/stepRight(i - 1);
				return array[i]*mu + array[i-1]*(1.0 - mu);
			}
		
		//return last element if the condition has still not been satisfied
		return array[nodes.length - 1];
				
	}
	
	public double getNode(int i) {
		return nodes[i];
	}
	
	public double stepRight(int i) {
		return nodes[i+1] - nodes[i];
	}
	
	public double stepLeft(int i) {
		return nodes[i] - nodes[i-1];
	}
	
	public double step(int i, double sign) {
		return nodes[i + (int) ((1. + sign)*0.5)] - nodes[i - (int) ((1. - sign)*0.5)];
	}

}