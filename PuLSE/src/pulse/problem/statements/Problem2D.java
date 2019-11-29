package pulse.problem.statements;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.schemes.DiscretePulse;
import pulse.problem.schemes.DiscretePulse2D;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.Grid2D;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import static pulse.properties.NumericPropertyKeyword.*;

public abstract class Problem2D extends Problem implements TwoDimensional {
	
	protected double d, Bi3, dAv;
	private final static boolean DEBUG = false;	

	protected Problem2D() {
		super();
		Bi3 = (double)NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d 	= (double)NumericProperty.def(DIAMETER).getValue();
		dAv = (double)NumericProperty.def(PYROMETER_SPOT).getValue();
	}	

	protected Problem2D(double d, double Bi3, double dAv) {
		super();
		this.d 	 = d;
		this.Bi3 = Bi3;
	}
	
	protected Problem2D(Problem sdd) {
		super(sdd);
		Bi3 = (double)NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d 	= (double)NumericProperty.def(DIAMETER).getValue();
		dAv = (double)NumericProperty.def(PYROMETER_SPOT).getValue();
	}
	
	protected Problem2D(Problem2D sdd) {
		super(sdd);
		this.d	 = sdd.d;
		this.Bi3 = sdd.Bi3;
		this.dAv = sdd.dAv;
	}
	
	@Override
	public NumericProperty getSampleDiameter() {
		return NumericProperty.derive(DIAMETER, d);
	}

	public void setSampleDiameter(NumericProperty d) {
		this.d = (double)d.getValue();
	}

	public NumericProperty getSideLosses() {
		return NumericProperty.derive(HEAT_LOSS_SIDE, Bi3);
	}

	public void setSideLosses(NumericProperty bi3) {
		this.Bi3 = (double)bi3.getValue();
	}
	
	@Override
	public NumericProperty getPyrometerSpot() {
		return NumericProperty.derive(PYROMETER_SPOT, dAv); //$NON-NLS-1$
	}

	public void setPyrometerSpot(NumericProperty dAv) {
		this.dAv = (double)dAv.getValue();
	}
	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.addAll(super.listedTypes());
		list.add(NumericProperty.def(HEAT_LOSS_SIDE));
		list.add(NumericProperty.def(DIAMETER));
		list.add(NumericProperty.def(PYROMETER_SPOT));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		switch(type) {
		case PYROMETER_SPOT		: setPyrometerSpot(property); break;
		case DIAMETER			: setSampleDiameter(property); break;
		case HEAT_LOSS_SIDE		: setSideLosses(property); break;
		}
	}
	
	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}
	
	@Override
	public DiscretePulse discretePulseOn(Grid grid) {
		return grid instanceof Grid2D ? 
				new DiscretePulse2D(this, pulse, (Grid2D)grid) : 
				super.discretePulseOn(grid);
	}
	
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
	
}