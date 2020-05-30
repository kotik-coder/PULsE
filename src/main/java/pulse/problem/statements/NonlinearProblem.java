package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.StandartType;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class NonlinearProblem extends Problem {
	
	protected double T;
	protected double emissivity;
	
	protected final static double STEFAN_BOTLZMAN = 5.6703E-08; //Stephan-Boltzmann constant
	
	private final static boolean DEBUG = false;	
		
	public NonlinearProblem() {
		super();		
		this.T	= (double)NumericProperty.def(TEST_TEMPERATURE).getValue();
	}
	
	public NonlinearProblem(Problem p) {
		super(p);
	}
	
	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		this.T		= p.T;
	}
	
	@Override
	public void retrieveData(ExperimentalData c) {		
		super.retrieveData(c);
		this.setTestTemperature(c.getMetadata().getTestTemperature());		
	}
	
	@Override
	public void setSpecificHeat(NumericProperty cV) {
		super.setSpecificHeat(cV);
	}

	public NumericProperty getTestTemperature() {
		return NumericProperty.derive(TEST_TEMPERATURE, T);
	}
	
	public void setTestTemperature(NumericProperty T) {
		this.T  = (double)T.getValue();
		
		var cP = InterpolationDataset.getDataset(StandartType.SPECIFIC_HEAT);
		
		if(cP != null)
			super.cP = cP.interpolateAt(this.T);
		
		var rho = InterpolationDataset.getDataset(StandartType.DENSITY);
		
		if(rho != null) 
			super.rho = rho.interpolateAt(this.T);
		
	}		
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(SPECIFIC_HEAT));
		list.add(NumericProperty.def(DENSITY));	
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem.Descriptor"); 
	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		super.set(type, value);
		double newVal = ((Number)value.getValue()).doubleValue();
		
		switch(type) {
			case TEST_TEMPERATURE	 :	T = newVal; return; 
			default: break;
		}				
		
	}
	
	public double maximumHeating() {
		double Q	= (double)pulse.getLaserEnergy().getValue();
		double dLas = (double)pulse.getSpotDiameter().getValue();
		
		//evaluateDependentParameters();
		
		return 4.0*emissivity*Q/(Math.PI*dLas*dLas*l*cP*rho);
	}
	
	@Override
	public boolean allDetailsPresent() {
		return cP != 0 && rho !=0;
	}
	
	public void evaluateDependentParameters() {		
		final double lambda = thermalConductivity();
		emissivity =  Bi1*lambda/(4.*Math.pow(T, 3)*l*STEFAN_BOTLZMAN);
	}
	
	public double thermalConductivity() {
		return a*cP*rho;
	}
	
	public NumericProperty getThermalConductivity() {
		return NumericProperty.derive(CONDUCTIVITY, thermalConductivity());
	}
	
	public NumericProperty getEmissivityProperty() {
		return NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, emissivity);
	}
	
	public void setEmissivity(NumericProperty e) {
		if(e.getType() != NumericPropertyKeyword.EMISSIVITY)
			throw new IllegalArgumentException("Illegal type: " + e.getType());
		this.emissivity = (double)e.getValue();
	}

}