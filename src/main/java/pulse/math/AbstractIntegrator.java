package pulse.math;

import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An {@code AbstractIntegrator} serves to calculate the definite integral of a function.
 * It defines the integration bounds in the form  of a {@code Segment} and two abstract methods,
 * one to calculate the integrand as a function of one or more variables and the other to 
 * actually do the integration.
 *
 */

public abstract class AbstractIntegrator extends PropertyHolder implements Reflexive {

	private Segment integrationBounds;
	
	/**
	 * Creates an {@code AbstractIntegrator} with the specified integration bounds.
	 * @param bounds the integration bounds.
	 */
	
	public AbstractIntegrator(Segment bounds) {
		setBounds(bounds);
	}

	/**
	 * Calculates the definite integral within the specified integration bounds.
	 * 
	 * @return the value of the integral
	 */

	public abstract double integrate();

	/**
	 * Calculates the integrand function.
	 * 
	 * @param vars one or more variables 
	 * @return the value of the integrand at the specified variable values. 
	 */

	public abstract double integrand(double... vars);
	
	/**
	 * Retrieves the integration bounds
	 * @return the integration bounds.
	 */
	
	public Segment getBounds() {
		return integrationBounds;
	}
	
	/**
	 * Simply sets the integration bounds to {@code bounds}
	 * @param bounds the new integration bounds.
	 */

	public void setBounds(Segment bounds) {
		this.integrationBounds = bounds;
	}
	
	@Override
	public String getPrefix() {
		return "Integrator";
	}

}