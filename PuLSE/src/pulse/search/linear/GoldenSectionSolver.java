package pulse.search.linear;

import java.util.List;

import pulse.problem.statements.Problem;
import pulse.properties.Flag;
import pulse.search.direction.PathSolver;
import pulse.search.math.IndexedVector;
import pulse.search.math.Segment;
import pulse.search.math.Vector;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

public class GoldenSectionSolver extends LinearSolver {	
	
	private final static double PHI = 0.6180; //golden section	
	private static GoldenSectionSolver instance = new GoldenSectionSolver();
	
	private GoldenSectionSolver() {
		super();
	}
	
	@Override
	public double minimum(SearchTask task) {
		
		final double EPS = 1e-14;
		
		final Problem p = task.getProblem();
		
		final IndexedVector params	= p.optimisationVector( PathSolver.getSearchFlags() );
		final Vector direction		= task.getPath().getDirection();
		
		Segment segment    = boundaries(params, direction);
		
		final double squaredError = Math.pow(searchResolution*PHI*segment.length(), 2);
		double ss2 = 0;
		double ss1 = 0;
		Vector newParams;
		
		double alpha, one_minus_alpha;
		
		for(double t = PHI*segment.length(); t*t > squaredError; t = PHI*segment.length()) {
			alpha 			= segment.getMinimum() + t;
			one_minus_alpha	= segment.getMaximum() - t;
			
			newParams = params.plus(direction.times(alpha)); //alpha
			p.assign(new IndexedVector(newParams, params.getIndices()));
			ss2 = task.calculateDeviation(); //f(alpha)
			
			newParams = params.plus(direction.times(one_minus_alpha)); //1 - alpha 
			p.assign(new IndexedVector(newParams, params.getIndices()));
			ss1 = task.calculateDeviation(); //f(1-alpha)
			
			p.assign(new IndexedVector(newParams, params.getIndices())); //return to old position
			
			if(ss2 - ss1 > EPS)		
				segment.setMaximum(alpha);
			else 				
				segment.setMinimum(one_minus_alpha);
							
		}
	
	  return segment.mean();
	  
	}
	
	@Override
	public String toString() {
		return Messages.getString("GoldenSectionSolver.Descriptor"); //$NON-NLS-1$
	}
	
	public static GoldenSectionSolver getInstance() {
		return instance;
	}
	
}
