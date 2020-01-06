package pulse.problem.statements;

import java.util.List;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.ui.Messages;

public class DiathermicMaterialProblem extends LinearisedProblem {

	private double diathermicCoefficient;
	private final static boolean DEBUG = false;	

	public DiathermicMaterialProblem() {
		super();
		this.diathermicCoefficient = (double)NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT).getValue();
	}	

	public DiathermicMaterialProblem(NumericProperty diathermicCoefficient) {
		super();
		this.diathermicCoefficient = (double)(diathermicCoefficient.getValue());
	}	
	
	public DiathermicMaterialProblem(Problem sdd) {
		super(sdd);
		if(sdd instanceof DiathermicMaterialProblem)
			this.diathermicCoefficient = ((DiathermicMaterialProblem)sdd).diathermicCoefficient;
		else
			this.diathermicCoefficient = (double)NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT).getValue();
	}
	
	public DiathermicMaterialProblem(DiathermicMaterialProblem sdd) {
		super(sdd);
		this.diathermicCoefficient = sdd.diathermicCoefficient;
	}
	
	public NumericProperty getDiathermicCoefficient() {
		return NumericProperty.derive(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT, diathermicCoefficient);
	}
	
	public void setDiathermicCoefficient(NumericProperty diathermicCoefficient) {
		this.diathermicCoefficient = (double)diathermicCoefficient.getValue();
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		NumericPropertyKeyword prop = (NumericPropertyKeyword)type;
		double newVal = ((Number)property.getValue()).doubleValue();
		
		switch(prop) {
		case DIATHERMIC_COEFFICIENT : diathermicCoefficient = newVal; break;
		default : super.set(type, property); break; 
		}
					
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}
	
	@Override
	public IndexedVector optimisationVector(List<Flag> flags) {
		IndexedVector optimisationVector = super.optimisationVector(flags);		 				
		
		for(int i = 0, size = optimisationVector.dimension(); i < size; i++) {
			switch( optimisationVector.getIndex(i) ) {
				case DIATHERMIC_COEFFICIENT		:	
					optimisationVector.set(i, diathermicCoefficient);
					break;									
				default 				: 	continue;
			}
		}
		
		return optimisationVector;
		
	}
		
	@Override
	public void assign(IndexedVector params) {
		super.assign(params);
		
		for(int i = 0, size = params.dimension(); i < size; i++) {
			switch( params.getIndex(i) ) {
				case DIATHERMIC_COEFFICIENT		:	
					diathermicCoefficient = params.get(i);
					break;				
				default 				: 	continue;
			}
		}
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT));
		return list;
	}
	
	@Override
	public String toString() {
		return Messages.getString("DiathermicProblem.Descriptor"); 
	}
	
}