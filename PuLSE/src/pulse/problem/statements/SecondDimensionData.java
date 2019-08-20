package pulse.problem.statements;

import java.util.ArrayList;
import java.util.List;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/*
 * A class for specifying parameters of the second dimension of the problem
 */

public class SecondDimensionData extends PropertyHolder {
	
	private double d, Bi3, dAv;
	
	public final static NumericProperty HEAT_LOSS_SIDE	
	= new NumericProperty(NumericPropertyKeyword.HEAT_LOSS_SIDE, 
			Messages.getString("HeatLossSide.Descriptor"), 
			Messages.getString("HeatLossSide.Abbreviation"),
			0.0, 0.0, 10.0, 0.0, 1.0, true);
			
	protected SecondDimensionData() {
		Bi3 = (double)HEAT_LOSS_SIDE.getValue();
		d 	= (double)NumericProperty.DIAMETER.getValue();
		dAv = (double)NumericProperty.PYROMETER_SPOT.getValue();
	}

	protected SecondDimensionData(double d, double Bi3, double dAv) {
		this.d 	 = d;
		this.Bi3 = Bi3;
	}
	
	protected SecondDimensionData(SecondDimensionData sdd) {
		this.d	 = sdd.d;
		this.Bi3 = sdd.Bi3;
		this.dAv = sdd.dAv;
	}
	
	public NumericProperty getSampleDiameter() {
		return new NumericProperty(d, NumericProperty.DIAMETER);
	}

	public void setSampleDiameter(NumericProperty d) {
		this.d = (double)d.getValue();
	}

	public NumericProperty getSideLosses() {
		return new NumericProperty(Bi3, HEAT_LOSS_SIDE);
	}

	public void setSideLosses(NumericProperty bi3) {
		this.Bi3 = (double)bi3.getValue();
	}
	
	public void resetHeatLosses() {
		this.Bi3 = 0;
	}
	
	public NumericProperty getPyrometerSpot() {
		return new NumericProperty(dAv, NumericProperty.PYROMETER_SPOT); //$NON-NLS-1$
	}

	public void setPyrometerSpot(NumericProperty dAv) {
		this.dAv = (double)dAv.getValue();
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = new ArrayList<Property>();
		list.add(HEAT_LOSS_SIDE);
		list.add(NumericProperty.DIAMETER);
		list.add(NumericProperty.PYROMETER_SPOT);
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case PYROMETER_SPOT : setPyrometerSpot(property); break;
		case DIAMETER : setSampleDiameter(property); break;
		case HEAT_LOSS_SIDE : setSideLosses(property); break;
		}
	}
	
}