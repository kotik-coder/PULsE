package pulse.search.linear;

import static java.lang.Math.*;

import pulse.problem.statements.Problem;
import pulse.search.direction.PathSolver;
import pulse.search.math.Segment;
import pulse.search.math.Vector;
import pulse.tasks.Path;
import pulse.tasks.SearchTask;

public class WolfeSolver extends LinearSolver {

	private static WolfeSolver instance = new WolfeSolver();
	
	private static double C1 = 0.05;
	private static double C2 = 0.8;
	
	private WolfeSolver() {
		super();
	}		
	
	@Override
	public double minimum(SearchTask task) {						
		
		Problem problem = task.getProblem();
		Path	p		= task.getPath();
		
		final Vector direction    = p.getDirection();
		final Vector g1 	  	  = p.getGradient();
		
		final double G1P 	 = g1.dot(direction);
		final double G1P_ABS = abs(G1P);
		
		Vector params    = problem.objectiveFunction(task.getSearchFlags());
		Segment segment  = boundaries(task.parameterTypes(), params, direction);
			
		double ss1 = task.calculateDeviation();
		double ss2;
		
		double randomConfinedValue = 0;
		Vector g2, newParams;
		double g2p;
		
		for(double initialLength = segment.length(); segment.length()/initialLength > searchResolution; ) {
			randomConfinedValue = segment.randomValue();
			
			newParams = params.plus(direction.times(randomConfinedValue));
			problem.assign(newParams, task.getSearchFlags());
			
			ss2 	  = task.calculateDeviation();
			
			if(ss2 - ss1 > C1*randomConfinedValue*G1P) {
				segment.setMaximum(randomConfinedValue);
				continue;
			}			
			
			g2	= PathSolver.gradient(task);
			g2p = g2.dot(direction); 
			
			if( abs(g2p) <= C2*G1P_ABS )
				break;
			
			if( g2p >= C2*G1P ) 
				break;
				
			segment.setMinimum(randomConfinedValue);
			continue;
			
			
		}
		
		problem.assign(params, task.getSearchFlags());
		p.setGradient(g1);
		
	    return randomConfinedValue;
	
	}
	
	@Override
	public String toString() {
		return Messages.getString("WolfeSolver.Descriptor"); //$NON-NLS-1$
	}
	
	public static WolfeSolver getInstance() {
		return instance;
	}

}
