package pulse.problem.statements;

import java.util.List;

import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.math.IndexedVector;
import pulse.ui.Messages;

public class TranslucentMaterialProblem extends LinearisedProblem {

	private AbsorptionModel absorption;
	private final static boolean DEBUG = false;
	private final static double SENSITIVITY = 100;

	public TranslucentMaterialProblem() {
		super();
		absorption		= new BeerLambertAbsorption();
	}	

	public TranslucentMaterialProblem(AbsorptionModel laserAbsorption) {
		super();
		this.absorption	= laserAbsorption;
	}	
	
	public TranslucentMaterialProblem(Problem sdd) {
		super(sdd);
		if(sdd instanceof TranslucentMaterialProblem) {
			TranslucentMaterialProblem tp = (TranslucentMaterialProblem)sdd; 
			this.absorption	= tp.absorption;			
		}
		else 
			absorption		= new BeerLambertAbsorption();	
	}
	
	public TranslucentMaterialProblem(TranslucentMaterialProblem tp) {
		super(tp);
		this.absorption	= tp.absorption;
	}
	
	public AbsorptionModel getAbsorptionModel() {
		return absorption;
	}
	
	
	public void setAbsorptionModel(AbsorptionModel model) {
		this.absorption = model;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		absorption.set(type, property);
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}
	
	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		
		for(int i = 0, size = output[0].dimension(); i < size; i++) {
			switch( output[0].getIndex(i) ) {
				case LASER_ABSORPTIVITY		:	
					output[0].set(i, 
							(double) (absorption.getLaserAbsorptivity()).getValue()/SENSITIVITY  );
					output[1].set(i, 
							0.1 );
					break;
				case THERMAL_ABSORPTIVITY		:	
					output[0].set(i, 
							(double) (absorption.getThermalAbsorptivity()).getValue()/SENSITIVITY );
					output[0].set(i, 
							0.1 );
					break;						
				default 				: 	continue;
			}
		}
		
	}
		
	@Override
	public void assign(IndexedVector params) {
		super.assign(params);
		
		for(int i = 0, size = params.dimension(); i < size; i++) {
			switch( params.getIndex(i) ) {
				case LASER_ABSORPTIVITY		:	
					absorption.setLaserAbsorptivity( 
							NumericProperty.derive(NumericPropertyKeyword.LASER_ABSORPTIVITY, params.get(i)*SENSITIVITY) );
					break;
				case THERMAL_ABSORPTIVITY		:	
					absorption.setThermalAbsorptivity( 
							NumericProperty.derive(NumericPropertyKeyword.THERMAL_ABSORPTIVITY, params.get(i)*SENSITIVITY) );
					break;		
				default 				: 	continue;
			}
		}
	}
	
	@Override
	public String toString() {
		return Messages.getString("DistributedProblem.Descriptor"); 
	}
	
}