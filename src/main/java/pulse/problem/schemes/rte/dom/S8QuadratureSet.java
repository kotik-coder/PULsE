package pulse.problem.schemes.rte.dom;

public class S8QuadratureSet {

	private static double[] mu = new double[8];
	private static double[] w = new double[8];
	
	static {
	
		mu[0] =  0.1422555;
		mu[4] = -0.1422555;
		mu[1] =  0.5773503;
		mu[5] = -0.5773503;
		mu[2] =  0.8040087;
		mu[6] = -0.8040087;
		mu[3] =  0.9795543;
		mu[7] = -0.9795543;
		
		w[0] = 2.1637144/(2.0*Math.PI);
		w[4] = 2.1637144/(2.0*Math.PI);
		w[1] = 2.6406988/(2.0*Math.PI);
		w[5] = 2.6406988/(2.0*Math.PI);
		w[2] = 0.7938272/(2.0*Math.PI);
		w[6] = 0.7938272/(2.0*Math.PI);
		w[3] = 0.6849436/(2.0*Math.PI);
		w[7] = 0.6849436/(2.0*Math.PI);
		
	}
	
	private S8QuadratureSet() {}
	
	public static double[] getNodes() {
		return mu;
	}
	
	public static double[] getWeights() {
		return w;
	}
	
}