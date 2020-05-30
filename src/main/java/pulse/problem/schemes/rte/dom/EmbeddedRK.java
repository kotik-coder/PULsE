package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;

public class EmbeddedRK extends AdaptiveStepIntegrator {

	public Vector b, bHat;
	public Vector c;	
	public Matrix rk;
	
	public EmbeddedRK(DiscreteIntensities intensities, EmissionFunction ef, IntegratedPhaseFunction ipf) {
		super(intensities, ef, ipf);
		this.rkInitFehlberg();
	}
	
	private void rkInitF2() {
		
		rk = new Matrix(new double[][] {
			{0.0,		0.0,	0.0}, 
			{0.5,		0.0,	0.0}, 
			{1.0/256.,		255./256., 	0.0}
		});
		
		b = new Vector(new double[]{ 1.0/512., 255./256., 1./512. });
		bHat = new Vector(new double[]{ 1.0/256., 255./256., 0.0 });
		c = new Vector(new double[] { 0.0, 0.5, 1.0 });
		
	}
	
	private void rkInitFehlberg() {
		
		rk = new Matrix(new double[][] {
			{0.0,		0.0,	0.0,	0.0,	0.0}, 
			{0.25,		0.0,	0.0,	0.0,	0.0}, 
			{3.0/32.0,	9.0/32.0, 	0.0,	0.0,	0.0},
			{1932./2197.,	-7200./2197,	7296/2197.,	0.0,	0.0},
			{439./216.,	-8.0,	3680./513.,	-845./4104,	0.0},
			{-8.0/27.0,	2.0,	-3544.0/2565,	1859./4104.,	-11.0/40.0}
		});
		
		b = new Vector(new double[]{ 16.0/135.0, 0.0, 6656./12825.,	28561/56430., -9.0/50.0, 2.0/55.0 });
		bHat = new Vector(new double[]{ 25./216.,	0.0,	1408./2565.,	2197./4104.,	-1.0/5.0,	0.0 });	
		c = new Vector(new double[] { 0.0, 0.25, 3.0/8.0, 12.0/13.0, 1.0, 0.5 });
		
	}
	
	private double[] rk(int i, int j, final double sign) {
		
		double h = intensities.grid.step(j, sign);
		final double hSigned = h*sign;
		final double t = intensities.grid.getNode(j);
		
		double[] q = new double[b.dimension()];
		
		double sum = 0;
		double errorSq = 0;
		
		for(int m = 0; m < q.length; m++) {
			
				sum = 0;
				for(int k = 0; k < m; k++) 
					sum += rk.get(m, k)*q[k];
								
				q[m] = rhs(i, j, t + hSigned*c.get(m), intensities.I[i][j] + sum*hSigned);

				errorSq += (b.get(m) - bHat.get(m))*q[m];	
		}		
		
		return new double[] {intensities.I[i][j] + hSigned*b.dot(new Vector(q)), errorSq*hSigned};
		
	}

	@Override
	public double[] stepRight(int i, int j) {
		return rk(i, j, 1.0);
	}

	@Override
	public double[] stepLeft(int i, int j) {
		return rk(i, j, -1.0);
	}

}