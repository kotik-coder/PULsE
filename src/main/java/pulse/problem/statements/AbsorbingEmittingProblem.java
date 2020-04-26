package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.EMISSIVITY;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class AbsorbingEmittingProblem extends LinearisedProblem {

	private final static boolean DEBUG = false;
	
	private double opticalThickness;
	private double planckNumber;
	private double emissivity;
	private double initialTemperature;
	
	public AbsorbingEmittingProblem() {
		super();		
		this.initialTemperature	= (double)NumericProperty.def(TEST_TEMPERATURE).getValue();
		this.opticalThickness = (double)NumericProperty.def(OPTICAL_THICKNESS).getValue();
		this.planckNumber = (double)NumericProperty.def(PLANCK_NUMBER).getValue();
		this.emissivity = (double)NumericProperty.def(EMISSIVITY).getValue();
	}
	
	public AbsorbingEmittingProblem(Problem p) {
		super(p);
	}
	
	public AbsorbingEmittingProblem(AbsorbingEmittingProblem p) {
		super(p);
		this.initialTemperature		= p.initialTemperature;
		this.emissivity = p.emissivity;
		this.opticalThickness = p.opticalThickness;
		this.planckNumber = p.planckNumber;
	}
	
	@Override
	public String toString() {
		return Messages.getString("DistributedEmissionAbsorptionProblem.Descriptor"); 
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	public NumericProperty getOpticalThickness() {
		return NumericProperty.derive(OPTICAL_THICKNESS, opticalThickness);
	}

	public void setOpticalThickness(NumericProperty tau0) {
		if(tau0.getType() != OPTICAL_THICKNESS)
			throw new IllegalArgumentException("Illegal type: " + tau0.getType());
		this.opticalThickness = (double)tau0.getValue();
	}

	public NumericProperty getPlanckNumber() {
		return NumericProperty.derive(PLANCK_NUMBER, planckNumber);
	}

	public void setPlanckNumber(NumericProperty planckNumber) {
		if(planckNumber.getType() != PLANCK_NUMBER)
			throw new IllegalArgumentException("Illegal type: " + planckNumber.getType());
		this.planckNumber = (double)planckNumber.getValue();
	}

	public NumericProperty getEmissivity() {
		return NumericProperty.derive(EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty emissivity) {
		if(emissivity.getType() != EMISSIVITY)
			throw new IllegalArgumentException("Illegal type: " + emissivity.getType());
		this.emissivity = (double)emissivity.getValue();
	}

	public NumericProperty getInitialTemperature() {
		return NumericProperty.derive(TEST_TEMPERATURE, initialTemperature);
	}

	public void setInitialTemperature(NumericProperty initialTemperature) {
		if(initialTemperature.getType() != TEST_TEMPERATURE)
			throw new IllegalArgumentException("Illegal type: " + initialTemperature.getType());
		this.initialTemperature = (double)initialTemperature.getValue();
	}
	
	@Override
	public boolean allDetailsPresent() {
		 if((emissivity > 0) && (cP > 0) && (rho > 0))
			 return super.allDetailsPresent();
		 else
			 return false;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		super.set(type, value);
		double newVal = ((Number)value.getValue()).doubleValue();
		
		switch(type) {
			case TEST_TEMPERATURE	 :	initialTemperature = newVal; return; 
			case PLANCK_NUMBER		 :  planckNumber = newVal; return;
			case OPTICAL_THICKNESS	 :  opticalThickness = newVal; return;
			case EMISSIVITY			 :  emissivity = newVal; return;
			default: break;
		}				
		
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(PLANCK_NUMBER));
		list.add(NumericProperty.def(OPTICAL_THICKNESS));
		list.add(NumericProperty.def(EMISSIVITY));
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		return list;
	}
	
	public double maximumHeating() {
		double Q	= (double)pulse.getLaserEnergy().getValue();
		double dLas = (double)pulse.getSpotDiameter().getValue();
		
		return 4.0*emissivity*Q/(Math.PI*dLas*dLas*l*cP*rho);
	}
	
}