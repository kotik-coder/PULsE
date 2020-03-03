package pulse.problem.schemes;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;

import pulse.problem.statements.Problem;
import pulse.problem.statements.Pulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

/**
 * A {@code DiscretePulse} is an object that acts as a medium between the physical {@code Pulse}
 * and the respective {@code DifferenceScheme} used to process the solution of a {@code Problem}. 
 * @see pulse.problem.statements.Pulse
 */

public class DiscretePulse {

	private Grid grid;
	private Pulse pulse;
	private double discretePulseWidth;
	private double timeFactor;
	
	/**
	 * This creates a one-dimensional discrete pulse on a {@code grid}.
	 * <p>The dimensional factor is taken from the {@code problem}, while
	 * the discrete pulse width (a multiplier of the {@code grid} parameter
	 * {@code tau} is calculated using the {@code gridTime} method.</p>
	 * @param problem the problem, used to extract the dimensional time factor
 	 * @param pulse the physical (continuous) pulse
	 * @param grid a grid used to discretise the {@code pulse}
	 */
	
	public DiscretePulse(Problem problem, Pulse pulse, Grid grid) {
		timeFactor	= problem.timeFactor();
		this.grid	= grid;
		this.pulse	= pulse;
		
		recalculate(NumericPropertyKeyword.PULSE_WIDTH);
		recalculate(NumericPropertyKeyword.TIME_SHIFT);
		
		pulse.addListener( e -> {
			Property p = e.getProperty(); 
			
			if( ! (p instanceof NumericProperty ) )
				return;
			
			NumericPropertyKeyword key = ((NumericProperty) p).getType();				
			recalculate(key);			
		});
	}
	
	/**
	 * This evaluates the dimensionless, discretised pulse function on a {@code grid} based on the type of the {@code PulseShape}.
	 * It is then used to evaluate the heat source in the difference scheme.
	 * @param time the dimensionless time (a multiplier of {@code tau}), at which calculation should be performed
	 * @return a double value, representing the pulse function at {@code time}
	 * @throws IllegalArgumentException when the PulseShape is unknown
	 */
	
	public double evaluateAt(double time) throws IllegalArgumentException {
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
	
	/**
	 * Recalculates the {@code discretePulseWidth} by calling {@code gridTime} on
	 * the physical pulse width and {@code timeFactor}.
	 * @see pulse.problem.schemes.Grid.gridTime(double,double)
	 */
	
	public void recalculate(NumericPropertyKeyword keyword) {		
		switch(keyword) {
		case PULSE_WIDTH :		discretePulseWidth = grid.gridTime( 
				((Number)pulse.getPulseWidth().getValue()).doubleValue(), 
				timeFactor); break;
		default:
			break;
		}
	}
	
	/**
	 * Optimises the {@code grid} parameters. 
	 * <p>This can change the {@code tauFactor} and {@code tau} variables in the
	 * {@code grid} object if {@code discretePulseWidth < grid.tau}.</p> 
	 * @param grid the grid to be adjusted
	 */
	
	public void optimise(Grid grid) {
		for(final double factor = 1.05; 
			factor*grid.tau > discretePulseWidth; 
			recalculate(NumericPropertyKeyword.PULSE_WIDTH) ) {
				grid.tauFactor	/= 1.5;						
				grid.tau		 = grid.tauFactor*pow(grid.hx, 2);
		}		
	}
	
	/**
	 * Gets the discrete pulse width defined by {@code DiscretePulse}.
	 * @return a double, representing the discrete pulse width.
	 */
	
	public double getDiscretePulseWidth() {
		return discretePulseWidth;
	}
	
	/**
	 * Gets the physical {@code Pulse}
	 * @return the {@code Pulse} object
	 */
	
	public Pulse getPulse() {
		return pulse;
	}
	
	/**
	 * Gets the {@code Grid} object used to construct this {@code DiscretePulse}
	 * @return the {@code Grid} object.
	 */

	public Grid getGrid() {
		return grid;
	}
	
}