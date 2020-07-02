package pulse.problem.laser;

import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class PulseTemporalShape extends PropertyHolder implements Reflexive {

		private double width;
	
		/**
		 * This evaluates the dimensionless, discretised pulse function on a
		 * {@code grid} based on the type of the {@code PulseShape}. It is then used to
		 * evaluate the heat source in the difference scheme.
		 * 
		 * @param time the dimensionless time (a multiplier of {@code tau}), at which
		 *             calculation should be performed
		 * @return a double value, representing the pulse function at {@code time}
		 * @throws IllegalArgumentException when the PulseShape is unknown
		 */

		public abstract double evaluateAt(double time);

		public void init(DiscretePulse pulse) {
			width = pulse.getDiscretePulseWidth();
		}
		
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