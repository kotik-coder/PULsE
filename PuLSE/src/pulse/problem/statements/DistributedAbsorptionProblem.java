package pulse.problem.statements;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class DistributedAbsorptionProblem extends LinearisedProblem {

	private AbsorptionModel absorptionModel;
	private final static boolean DEBUG = false;	

	public DistributedAbsorptionProblem() {
		super();
		absorptionModel = new BeerLambertAbsorption();
	}	

	public DistributedAbsorptionProblem(AbsorptionModel m) {
		super();
		this.absorptionModel = m;
	}	
	
	public DistributedAbsorptionProblem(Problem sdd) {
		super(sdd);
		if(sdd instanceof DistributedAbsorptionProblem)
			this.absorptionModel = ((DistributedAbsorptionProblem)sdd).absorptionModel;
		else
			this.absorptionModel = new BeerLambertAbsorption();
	}
	
	public DistributedAbsorptionProblem(DistributedAbsorptionProblem sdd) {
		super(sdd);
		this.absorptionModel = sdd.absorptionModel;
	}
	
	public AbsorptionModel getAbsorptionModel() {
		return absorptionModel;
	}
	
	public void setAbsorptionModel(AbsorptionModel model) {
		this.absorptionModel = model;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		absorptionModel.set(type, property);
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}
	/*
	@Override
	public IndexedVector optimisationVector(List<Flag> flags) {
		IndexedVector optimisationVector = super.optimisationVector(flags);		 				
		
		for(int i = 0, size = optimisationVector.dimension(); i < size; i++) {
			switch( optimisationVector.getIndex(i) ) {
				case PYROMETER_SPOT		:	
					optimisationVector.set(i, dAv);
					break;
				case SPOT_DIAMETER		:	
					optimisationVector.set(i, (double)pulse.getSpotDiameter().getValue());
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
				case PYROMETER_SPOT		:	
					dAv = params.get(i);
					break;
				case SPOT_DIAMETER		:
					NumericProperty spotDiameter = NumericProperty.derive(SPOT_DIAMETER, params.get(i)); 
					pulse.setSpotDiameter(spotDiameter);
					pulse.notifyListeners(this, spotDiameter);
					break;					
				case HEAT_LOSS		:	
					Bi3 = params.get(i);
					break;
				default 				: 	continue;
			}
		}
	}
	*/
	
	@Override
	public String toString() {
		return Messages.getString("DistributedProblem.Descriptor"); 
	}
	
}