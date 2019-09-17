package pulse.problem.statements;

import java.util.ArrayList;
import java.util.List;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.ui.Messages;

import static java.lang.Math.signum;
import static pulse.properties.NumericPropertyKeyword.*;

/**
 * A {@code Pulse} stores the parameters of the laser pulse,
 * but does not provide the calculation facilities.
 * @see pulse.problem.schemes.DiscretePulse 
 * 
 */

public class Pulse extends PropertyHolder {
	
	private PulseShape pulseShape;
	private double pulseWidth, spotDiameter;
	
	/**
	 * Creates a default {@code Pulse} with a {@code RECTANGULAR} shape.
	 */
	
	public Pulse() {
		this(PulseShape.RECTANGULAR);
	}
	
	/**
	 * Creates a {@code Pulse} with default values of pulse width and laser spot diameter (as per XML specification),
	 * but with custom {@code PulseShape}.
	 * @param pform the pulse shape
	 */
	
	public Pulse(PulseShape pform) {
		this.pulseShape = pform;
		pulseWidth  = (double) NumericProperty.def(PULSE_WIDTH).getValue();
		spotDiameter = (double) NumericProperty.def(SPOT_DIAMETER).getValue();
	}
	
	/**
	 * Copy constructor
	 * @param p the pulse, parameters of which will be copied.
	 */

	public Pulse(Pulse p) {
		this.pulseShape	= p.getPulseShape();
		this.spotDiameter = p.spotDiameter;
		this.pulseWidth	= p.pulseWidth;
	}
	
	/**
	 * Retrieves the {code PulseShape} enum constant.
	 * @return the {@code} PulseShape
	 */
	
	public PulseShape getPulseShape() {
		return pulseShape;
	}

	public void setPulseShape(PulseShape pulseShape) {
		this.pulseShape = pulseShape;
	}

	public NumericProperty getPulseWidth() {
		return NumericProperty.derive(PULSE_WIDTH, pulseWidth);
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double)pulseWidth.getValue();
	}

	public NumericProperty getSpotDiameter() {
		return NumericProperty.derive(SPOT_DIAMETER, spotDiameter);
	}

	public void setSpotDiameter(NumericProperty spotDiameter) {
		this.spotDiameter = (double)spotDiameter.getValue();
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getPulseShape());
		sb.append(" ");
		sb.append(pulseWidth*1E3);
		sb.append(Messages.getString("Pulse.2"));
		sb.append(spotDiameter*1E3);
		sb.append(Messages.getString("Pulse.3"));
		return sb.toString();
	}
	
	/**
	 * The listed parameters for {@code Pulse} are: <code>PulseShape, PULSE_WIDTH, SPOT_DIAMETER</code>.
	 */
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(PulseShape.RECTANGULAR);
		list.add(NumericProperty.def(PULSE_WIDTH));
		list.add(NumericProperty.def(SPOT_DIAMETER));
		return list;				
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case PULSE_WIDTH : setPulseWidth(property); break;
		case SPOT_DIAMETER : setSpotDiameter(property); break;
		}
	}
	
	/**
	 * The {@code PulseShape}, an instance of {@code EnumProperty}, defines 
	 * a few simple pulse shapes usually encountered in a laser flash experiment.  
	 *
	 */
	
	public enum PulseShape implements EnumProperty {	
		
		/**
		 * Currently not supported (redirects to {@code RECTANGULAR})
		 */
		
		TRAPEZOIDAL,
		
		/**
		 * The simplest pulse shape defined as 
		 * <math>0.5*(1 + sgn(<i>t</i><sub>pulse</sub> - <i>t</i>))</math>,
		 * where <math>sgn(...)</math> is the signum function, 
		 * <sub>pulse</sub> is the pulse width.
		 * @see java.lang.Math.signum(double) 
		 */
		
		RECTANGULAR, 
		
		/**
		 * A pulse shape defined as an isosceles triangle. 
		 */
		
		TRIANGULAR,
		
		/**
		 * A bell-shaped laser pulse centered at {@code pulseWidth}/2.0. 
		 */
		
		GAUSSIAN;

		@Override
		public Object getValue() {
			return this;
		}	

		@Override
		public String getDescriptor(boolean addHtmlTags) {
			return "Pulse shape";
		}

		@Override
		public EnumProperty evaluate(String string) {
			return valueOf(string);
		}

	}

}