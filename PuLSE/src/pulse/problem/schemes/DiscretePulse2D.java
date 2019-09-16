package pulse.problem.schemes;

import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.problem.statements.TwoDimensional;

public class DiscretePulse2D extends DiscretePulse {

	private double discretePulseSpot;
	private double coordFactor;
	
	public <T extends Problem & TwoDimensional> DiscretePulse2D(T problem, Pulse pulse, Grid2D grid) {
		super(problem, pulse, grid);
		coordFactor = (double) problem.getSampleDiameter().getValue() / 2.0;		
		discretePulseSpot = grid.gridRadialDistance( 
				(double) pulse.getSpotDiameter().getValue() / 2.0,
				coordFactor );
	}
	
	/**
	 * This calculates the pulse function at a radial coordinate {@code coord}. It uses a Heaviside function
	 * to determine the result and returns 0 if {@code coord > spotDiameter}. Otherwise, it uses the {@code time}
	 * parameter to determine the pulse function using {@code evaluateAt(time)}.  
	 * @param time the time for calculation
	 * @param coord - the radial coordinate [length dimension] 
	 * @return the pulse function at {@code time} and {@code coord}
	 * @see evaluateAt
	 */
	
	public double evaluateAt(double time, double radialCoord) {
		return evaluateAt(time)*(0.5 + 0.5*Math.signum(discretePulseSpot - radialCoord));
	}
	
	@Override
	public void recalculate() {
		super.recalculate();
		
		discretePulseSpot = ((Grid2D)getGrid()).gridRadialDistance( 
				(double) getPulse().getSpotDiameter().getValue() / 2.0,
				coordFactor );	}
	
	@Override
	public void optimise(Grid grid) {
		super.optimise(grid);	
	
		if(! (grid instanceof Grid2D))
			return;
		
		Grid2D grid2D = (Grid2D)grid;
		
		for(final double factor = 1.05; factor*grid2D.hy > discretePulseSpot; 
				grid2D.hy = 1./grid2D.N, recalculate()) 
					grid2D.N += 5;		
		
	}
		
}