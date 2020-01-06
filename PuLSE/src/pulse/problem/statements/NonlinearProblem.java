package pulse.problem.statements;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

public class NonlinearProblem extends Problem {
	
	protected double T;
	protected double nonlinearPrecision = (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();	
	
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
		this.nonlinearPrecision = p.nonlinearPrecision;
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

	public NumericProperty getNonlinearPrecision() {
		return NumericProperty.derive(NONLINEAR_PRECISION, nonlinearPrecision);
	}

	public void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
		this.nonlinearPrecision = (double)nonlinearPrecision.getValue(); 
	}

	public NumericProperty getTestTemperature() {
		return NumericProperty.derive(TEST_TEMPERATURE, T);
	}
	
	public void setTestTemperature(NumericProperty T) {
		this.T  = (double)T.getValue();
		
		if(TaskManager.getSpecificHeatCurve() != null)
			super.cP = TaskManager.getSpecificHeatCurve().interpolateAt(this.T);
		
		if(TaskManager.getDensityCurve() != null) 
			super.rho = TaskManager.getDensityCurve().interpolateAt(this.T);
		
	}		
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NONLINEAR_PRECISION));
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(REFLECTANCE));
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
			case NONLINEAR_PRECISION : nonlinearPrecision = newVal; return;
		}				
		
	}
	
	public double maximumHeating() {
		double Q	= (double)pulse.getLaserEnergy().getValue();
		double dLas = (double)pulse.getSpotDiameter().getValue();
		
		return Q/(Math.PI*dLas*dLas*l*cP*rho);
	}
	
	@Override
	public boolean allDetailsPresent() {
		return cP != 0 && rho !=0;
	}

}