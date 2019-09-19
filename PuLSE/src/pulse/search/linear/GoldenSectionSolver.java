package pulse.search.linear;

import pulse.problem.statements.Problem;
import pulse.search.direction.PathSolver;
import pulse.search.math.IndexedVector;
import pulse.search.math.Segment;
import pulse.search.math.Vector;
import pulse.tasks.SearchTask;
import pulse.ui.Messages;

/**
 * The golden-section search is a simple dichotomy search for finding the minimum
 * of strictly unimodal functions by successively narrowing the domain of the search
 * using the golden ratio partitioning.
 * @see <a href="https://en.wikipedia.org/wiki/Golden-section_search">Wikipedia page</a>
 */

public class GoldenSectionSolver extends LinearSolver {	
	
	/**
	 * The golden section &phi;, which is approximately equal to 0.618033989.
	 */
	
	public final static double PHI = 1.0 - (3.0 - Math.sqrt(5.0))/2.0;	
	
	private static GoldenSectionSolver instance = new GoldenSectionSolver();
	
	private GoldenSectionSolver() { super(); }
	
	/**
	 * <p>Let {@code a} and {@code b} be the start and end point of a {@code Segment},
	 * initially defined by the {@code super.domain(IndexedVector,Vector)} method.
	 * This method will start a loop, which at each step <i>i</i> will compare the values of the target function
	 * at the end points of a {@code Segment} constructed from one of the end points <i>a<sub>i</sub></i> or <i>b<sub>i</sub></i> 
	 * and substituting the second end point with either <math><i>a<sub>i</sub> + &phi;*(b-a)</i></math>
	 * or <math><i>b<sub>i</sub> - &phi;*(b-a)</i></math>. This theoretically ensures the least 
	 * number of steps to reach the minimum (as compared to the standard dichotomy methods).</p>     
	 */
	
	@Override
	public double linearStep(SearchTask task) {
		
		final double EPS = 1e-14;
		
		final Problem p = task.getProblem();
		
		final IndexedVector params	= p.optimisationVector( PathSolver.getSearchFlags() );
		final Vector direction		= task.getPath().getDirection();
		
		Segment segment = domain(params, direction);
		
		final double squaredError = Math.pow(searchResolution*PHI*segment.length(), 2);
		double ss2 = 0;
		double ss1 = 0;
		Vector newParams;
		
		double alpha, one_minus_alpha;
				
		for(double t = PHI*segment.length(); t*t > squaredError; t = PHI*segment.length()) {
			alpha 			= segment.getMinimum() + t;
			one_minus_alpha	= segment.getMaximum() - t;
			
			newParams = params.sum(direction.multiply(alpha)); //alpha
			p.assign(new IndexedVector(newParams, params.getIndices()));
			ss2 = task.solveProblemAndCalculateDeviation(); //f(alpha)
			
			newParams = params.sum(direction.multiply(one_minus_alpha)); //1 - alpha 
			p.assign(new IndexedVector(newParams, params.getIndices()));
			ss1 = task.solveProblemAndCalculateDeviation(); //f(1-alpha)
			
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
		return Messages.getString("GoldenSectionSolver.Descriptor");
	}
	
	/**
	 * This class uses a singleton pattern, meaning there is only instance of this class.
	 * @return the single (static) instance of this class
	 */
	
	public static GoldenSectionSolver getInstance() {
		return instance;
	}
	
}