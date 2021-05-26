package pulse.problem.laser;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.INTEGRATION_SEGMENTS;

import pulse.input.ExperimentalData;
import pulse.math.FixedIntervalIntegrator;
import pulse.math.MidpointIntegrator;
import pulse.math.Segment;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An abstract time-dependent pulse shape. Declares the abstract method to
 * calculate the laser power function at a given moment of time. This generally
 * utilises a discrete pulse width. By default, uses a midpoint-rule numeric integrator
 * to calculate the pulse integral.
 *
 */

public abstract class PulseTemporalShape extends PropertyHolder implements Reflexive {

	private double width;
	
	private final static int DEFAULT_POINTS = 256;
	private FixedIntervalIntegrator integrator;
	
	public PulseTemporalShape() {
		//intentionlly blank
	}
	
	public PulseTemporalShape(PulseTemporalShape another) {
		this.integrator = another.integrator;
	}
	
	/**
	 * Creates a new midpoint-integrator using the number of segments equal to {@value DEFAULT_POINTS}.
	 * The integrand function is specified by the {@code evaluateAt} method of this class.
	 * @see pulse.math.MidpointIntegrator
	 * @see evaluateAt()
	 */
	
	public void initAreaIntegrator() {
		integrator = new MidpointIntegrator(new Segment(0.0, getPulseWidth()),
				derive(INTEGRATION_SEGMENTS, DEFAULT_POINTS)) {

			@Override
			public double integrand(double... vars) {
				return evaluateAt(vars[0]);
			}

		};
	}
	
	/**
	 * Uses numeric integration (midpoint rule) to calculate the area of the pulse
	 * shape corresponding to the selected parameters. The integration bounds are non-negative.
	 * 
	 * @return the area
	 */

	public double area() {
		integrator.setBounds(new Segment(0.0, getPulseWidth()));
		return integrator.integrate();
	}

	
	/**
	 * This evaluates the dimensionless, discretised pulse function on a
	 * {@code grid} needed to evaluate the heat source in the difference scheme.
	 * 
	 * @param time the dimensionless time (a multiplier of {@code tau}), at which
	 *             calculation should be performed
	 * @return a double value, representing the pulse function at {@code time}
	 */

	public abstract double evaluateAt(double time);

	/**
	 * Stores the pulse width from {@code pulse} and initialises area integration.
	 * @param pulse the discrete pulse containing the pulse width
	 */
	
	public void init(ExperimentalData data, DiscretePulse pulse) {
		width = pulse.getDiscreteWidth();
		this.initAreaIntegrator();
	}
	
	public abstract PulseTemporalShape copy();

	@Override
	public String getPrefix() {
		return "Pulse temporal shape";
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

	public double getPulseWidth() {
		return width;
	}

	public void setPulseWidth(double width) {
		this.width = width;
	}

}