package pulse.problem.statements;

import pulse.problem.statements.AbsorptionModel.SpectralRange;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class TranslucentMaterialProblem extends LinearisedProblem {

	private AbsorptionModel laserAbsorption, thermalAbsorption;
	private final static boolean DEBUG = false;	

	public TranslucentMaterialProblem() {
		super();
		laserAbsorption		= new BeerLambertAbsorption(SpectralRange.LASER);
		thermalAbsorption	= new BeerLambertAbsorption(SpectralRange.THERMAL);
	}	

	public TranslucentMaterialProblem(AbsorptionModel laserAbsorption, AbsorptionModel thermalAbsorption) {
		super();
		this.laserAbsorption	= laserAbsorption;
		this.thermalAbsorption	= thermalAbsorption;
	}	
	
	public TranslucentMaterialProblem(Problem sdd) {
		super(sdd);
		if(sdd instanceof TranslucentMaterialProblem) {
			TranslucentMaterialProblem tp = (TranslucentMaterialProblem)sdd; 
			this.laserAbsorption	= tp.laserAbsorption;
			this.thermalAbsorption	= tp.thermalAbsorption;			
		}
		else {
			laserAbsorption		= new BeerLambertAbsorption(SpectralRange.LASER);
			thermalAbsorption	= new BeerLambertAbsorption(SpectralRange.THERMAL);
		}		
	}
	
	public TranslucentMaterialProblem(TranslucentMaterialProblem tp) {
		super(tp);
		this.laserAbsorption	= tp.laserAbsorption;
		this.thermalAbsorption	= tp.thermalAbsorption;
	}
	
	public AbsorptionModel getLaserAbsorptionModel() {
		return laserAbsorption;
	}
	
	public AbsorptionModel getThermalAbsorptionModel() {
		return thermalAbsorption;
	}
	
	public void setLaserAbsorptionModel(AbsorptionModel model) {
		this.laserAbsorption = model;
	}
	
	public void setThermalAbsorptionModel(AbsorptionModel model) {
		this.thermalAbsorption = model;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		laserAbsorption.set(type, property);
		thermalAbsorption.set(type, property);
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