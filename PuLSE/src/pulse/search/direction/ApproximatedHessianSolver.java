package pulse.search.direction;

import static pulse.search.math.Matrix.*;

import pulse.search.math.Matrix;
import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

public class ApproximatedHessianSolver extends PathSolver {
	
	private static ApproximatedHessianSolver instance = new ApproximatedHessianSolver();
	
	private ApproximatedHessianSolver() {
		super();
	}
	
	@Override
	public void endOfStep(SearchTask task) {
		Path p		= task.getPath();
		Vector dir	= p.getDirection();
		
		final double minimumPoint = p.getMinimumPoint();
		final Matrix prevHessian = p.getHessian();
		final Vector g0			 = p.getGradient();	//g0
		Vector g1				 = gradient(task);	//g1
		
		p.setHessian(
				hessian(g0, g1, dir, prevHessian, minimumPoint)
				); //g_k, g_k+1, p_k+1, B_k, alpha_k+1
		
		p.setGradient(g1); //set g1 as the new gradient for next step
		
	}
	
	@Override
	public Vector direction(Path p) {
		return (p.getHessian().invert()).multiply(p.getGradient()).invert();
	}
	
	private Matrix hessian(Vector g1, Vector g2, Vector dir, Matrix prevHessian, double alpha) {
	    Vector y  = g2.minus(g1);                                      //g[k+1] - g[k]
	    return prevHessian.
	    			  plus( (multiplyAsMatrices(g1, g1)).multiply(1./g1.dot(dir)) ) .
	    			  plus( (multiplyAsMatrices(y, y)  ).multiply(1./(alpha*y.dot(dir)) ) ) ;         //BFGS for Ge[k+1]	    
	}
	
	@Override
	public String toString() {
		return Messages.getString("ApproximatedHessianSolver.Descriptor"); //$NON-NLS-1$
	}
		
	public static ApproximatedHessianSolver getInstance() {
		return instance;
	}
	
}
