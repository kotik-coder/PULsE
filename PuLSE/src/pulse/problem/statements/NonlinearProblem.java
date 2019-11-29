package pulse.problem.statements;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

public class NonlinearProblem extends Problem {
	
	protected double T, alpha;
	protected double nonlinearPrecision = (double)NumericProperty.def(NONLINEAR_PRECISION).getValue();	
	
	private final static boolean DEBUG = false;	
		
	public NonlinearProblem() {
		super();		
		this.T		= (double)NumericProperty.def(TEST_TEMPERATURE).getValue();
		this.alpha	= (double)NumericProperty.def(ABSORPTION).getValue();
	}
	
	public NonlinearProblem(Problem p) {
		super(p);
		alpha = 1.0;
	}
	
	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		this.nonlinearPrecision = p.nonlinearPrecision;
		this.T		= p.T;
		
		final double EPS = 1E-5;
		
		if(p.alpha > EPS) 
			this.alpha = p.alpha;
		else
			this.alpha = 1.0;
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
	
	public NumericProperty getAbsorptionCoefficient() {
		return NumericProperty.derive(ABSORPTION, alpha);
	}
	
	public void setAbsorptionCoefficient(NumericProperty alpha) {
		this.alpha = (double)alpha.getValue();
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
		list.add(NumericProperty.def(ABSORPTION));
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
	
	public void assign(IndexedVector params) {
		super.assign(params);
		
		for(int i = 0, size = params.dimension(); i < size; i++) {
			
			switch( params.getIndex(i) ) {
				case ABSORPTION			:	alpha = params.get(i); break;
				default 				: 	continue;
			}
		}
		
	}
	
	public IndexedVector optimisationVector(List<Flag> flags) {	
		IndexedVector optimisationVector = super.optimisationVector(flags);
		
		for(int i = 0, size = optimisationVector.dimension(); i < size; i++) {
			
			switch( optimisationVector.getIndex(i) ) {
				case ABSORPTION			:	optimisationVector.set(i, alpha); break;
				default 				: 	continue;
			}
		}
		
		return optimisationVector;
		
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		super.set(type, value);
		double newVal = ((Number)value.getValue()).doubleValue();
		
		switch(type) {
			case TEST_TEMPERATURE	 :	T = newVal; return; 
			case ABSORPTION 		 : 	alpha = newVal; 	return;
			case NONLINEAR_PRECISION : nonlinearPrecision = newVal; return;
		}				
		
	}
	
	public double maximumHeating() {
		double Q	= (double)pulse.getLaserEnergy().getValue();
		double dLas = (double)pulse.getSpotDiameter().getValue();
		
		return alpha*Q/(Math.PI*dLas*dLas*l*cP*rho);
	}
	
	@Override
	public boolean allDetailsPresent() {
		return cP != 0 && rho !=0;
	}

}