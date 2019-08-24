package pulse.input;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.signum;
import static java.lang.Math.sqrt;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.problem.statements.TwoDimensional;
import pulse.properties.EnumProperty;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;
import pulse.ui.Messages;
import static java.lang.Math.round;

public class Pulse extends PropertyHolder {
	
	private PulseShape pulseShape;
	private double pulseWidth, spotDiameter;
	private double pWidth, pSpotDiameter;
	
	public Pulse() {
		pulseShape  = PulseShape.RECTANGULAR;
		pulseWidth  = (double) NumericProperty.PULSE_WIDTH.getValue();
		spotDiameter = (double) NumericProperty.SPOT_DIAMETER.getValue();
	}
	
	public Pulse(PulseShape pform) {
		this.pulseShape = pform;
		pulseWidth  = (double) NumericProperty.PULSE_WIDTH.getValue();
		spotDiameter = (double) NumericProperty.SPOT_DIAMETER.getValue();
	}
	
	public Pulse(PulseShape pform, NumericProperty pwidth, NumericProperty ptime) {
		this.pulseShape = pform;
		this.spotDiameter = (double)pwidth.getValue();
		this.pulseWidth = (double)ptime.getValue();
		this.pWidth = pulseWidth;
		this.pSpotDiameter = spotDiameter;
	}
	
	public Pulse(Pulse p) {
		this.pulseShape	= p.getPulseShape();
		this.spotDiameter = p.spotDiameter;
		this.pulseWidth	= p.pulseWidth;
		this.pWidth		= p.pWidth;
		this.pSpotDiameter 	= p.pSpotDiameter;
	}
	
	public void transform(Problem problem, DifferenceScheme scheme) {
		pWidth(problem, scheme);
		pSpotDiameter(problem, scheme);
	}
	
	public double pWidth(Problem problem, DifferenceScheme scheme) {
		double timeStep   = scheme.getTimeStep();
		
		pWidth = round(
				pulseWidth/(problem.timeFactor()*
				timeStep) )*
				timeStep;
		
		return pWidth;
	}
	
	public double pSpotDiameter(Problem problem, DifferenceScheme scheme) {
		double xStep = scheme.getXStep();
		
		if(problem instanceof TwoDimensional) {
			double d		  = (double)( ( (TwoDimensional)problem ).getSecondDimensionData() ).getSampleDiameter().getValue();	
			pSpotDiameter = round(spotDiameter/d/xStep)*xStep; //diameter of the pulse width in hx [lengths steps of the difference scheme]
		} 
		else 
			pSpotDiameter			  = 1.0;
		
		return pSpotDiameter;
	}
	
	/*
	 * Introduce the trapezoidal function!
	 * AL
	 */
	
	public double evaluateAt(double time) throws IllegalArgumentException {		
		final double PTIME = 1./pWidth;

		switch (pulseShape) {		
			case RECTANGULAR : return 0.5*PTIME*(1 + signum(pWidth - time));
			case TRAPEZOIDAL : return 0.5*PTIME*(1 + signum(pWidth - time)); //needs correction!
			case TRIANGULAR	 : return PTIME*(1 + signum(pWidth - time))*(1 - abs(2.*time - pWidth)*PTIME);
			case GAUSSIAN		 : return PTIME*5./sqrt(PI)*exp(-25.*pow(time*PTIME - 0.5, 2));
			default			 : throw new IllegalArgumentException("Unknown pulse form received: " + pulseShape.toString()); //$NON-NLS-1$
		}
	    
	}
	
	public double evaluateAt(double time, double coord) throws IllegalArgumentException {
		return evaluateAt(time)*(0.5 + 0.5*signum(pSpotDiameter - coord));
	}

	public PulseShape getPulseShape() {
		return pulseShape;
	}

	public void setPulseShape(PulseShape pulseShape) {
		this.pulseShape = pulseShape;
	}

	public NumericProperty getPulseWidth() {
		return new NumericProperty(pulseWidth, NumericProperty.PULSE_WIDTH);
	}

	public void setPulseWidth(NumericProperty pulseWidth) {
		this.pulseWidth = (double)pulseWidth.getValue();
	}

	public NumericProperty getSpotDiameter() {
		return new NumericProperty(spotDiameter, NumericProperty.SPOT_DIAMETER);
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
		list.add(NumericProperty.PULSE_WIDTH);
		list.add(NumericProperty.SPOT_DIAMETER);
		return list;				
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

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case PULSE_WIDTH : setPulseWidth(property); break;
		case SPOT_DIAMETER : setSpotDiameter(property); break;
		}
	}

}
