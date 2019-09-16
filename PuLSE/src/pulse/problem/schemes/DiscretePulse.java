package pulse.problem.schemes;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;

import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;

public class DiscretePulse {

	private Grid grid;
	private Pulse pulse;
	private double discretePulseWidth;
	private double timeFactor;
	
	public DiscretePulse(Problem problem, Pulse pulse, Grid grid) {
		timeFactor	= problem.timeFactor();
		discretePulseWidth = grid.gridTime( 
				((Number)pulse.getPulseWidth().getValue()).doubleValue(),
				timeFactor);
		this.grid = grid;
		this.pulse = pulse;
	}
	
	/**
	 * This evaluates the pulse function [<math>s<sup>-1</sup></math>] based on the type of the {@code PulseShape}.
	 * It is then used to evaluate the heat source [<math>W</math>] in the heat equation.
	 * @param time the time, at which calculation should be performed
	 * @param timeGridUnit the unit of time on this grid. It is used to calculate the time rounded up to a multiple of that unit.
	 * @return a double value, representing the pulse function at {@code time}
	 * @throws IllegalArgumentException
	 */
	
	public double evaluateAt(double time) {
		final double _WIDTH = 1./discretePulseWidth;

		switch (pulse.getPulseShape()) {		
			case RECTANGULAR : return 0.5*_WIDTH*(1 + signum(discretePulseWidth - time));
			case TRAPEZOIDAL : return 0.5*_WIDTH*(1 + signum(discretePulseWidth - time)); //needs correction! TODO
			case TRIANGULAR	 : return 	  _WIDTH*(1 + signum(discretePulseWidth - time))*
							   (1 - abs(2.*time - discretePulseWidth)*_WIDTH);
			case GAUSSIAN	 : return 	  _WIDTH*5./sqrt(PI)*exp(-25.*pow(time*_WIDTH - 0.5, 2));
			default			 : 
				throw new IllegalArgumentException("Unknown pulse form received: "
			+ pulse.getPulseShape().toString());
		}
	    
	}
	
	public void recalculate() {
		discretePulseWidth = grid.gridTime( 
				((Number)pulse.getPulseWidth().getValue()).doubleValue(), 
				timeFactor);
	}
	
	public void optimise(Grid grid) {
		for(final double factor = 1.05; 
			factor*grid.tau > discretePulseWidth; 
			recalculate() ) {
				grid.tauFactor	/= 1.5;						
				grid.tau		 = grid.getTimeStep();
		}		
	}
	
	public double getDiscretePulseWidth() {
		return discretePulseWidth;
	}
	
	public Pulse getPulse() {
		return pulse;
	}

	public Grid getGrid() {
		return grid;
	}
	
}