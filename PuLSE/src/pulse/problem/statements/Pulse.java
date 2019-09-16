package pulse.problem.statements;

import java.util.ArrayList;
import java.util.List;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.ui.Messages;
import static pulse.properties.NumericPropertyKeyword.*;

/**
 * A {@code Pulse} describes the parameters of the laser pulse 
 * and provides means of calculating the pulse power as a function of time.
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
		
	/*
	
	public double evaluateAt(Problem problem, DifferenceScheme scheme, double time) throws IllegalArgumentException {		
		double pWidth = scheme.pWidth(problem);
		double pSpotDiameter = scheme.pSpotDiameter(problem);
		
		final double PTIME = 1./pWidth;

		switch (pulseShape) {		
			case RECTANGULAR : return 0.5*PTIME*(1 + signum(pWidth - time));
			case TRAPEZOIDAL : return 0.5*PTIME*(1 + signum(pWidth - time)); //needs correction!
			case TRIANGULAR	 : return PTIME*(1 + signum(pWidth - time))*(1 - abs(2.*time - pWidth)*PTIME);
			case GAUSSIAN		 : return PTIME*5./sqrt(PI)*exp(-25.*pow(time*PTIME - 0.5, 2));
			default			 : throw new IllegalArgumentException("Unknown pulse form received: " + pulseShape.toString()); //$NON-NLS-1$
		}
	    
	}*/
	
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
		sb.append(" "); //$NON-NLS-1$
		sb.append(pulseWidth*1E3);
		sb.append(Messages.getString("Pulse.2")); //$NON-NLS-1$
		sb.append(spotDiameter*1E3);
		sb.append(Messages.getString("Pulse.3")); //$NON-NLS-1$
		return sb.toString();
	}
	
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
	
	public enum PulseShape implements EnumProperty {
		TRAPEZOIDAL, RECTANGULAR, TRIANGULAR, GAUSSIAN;

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