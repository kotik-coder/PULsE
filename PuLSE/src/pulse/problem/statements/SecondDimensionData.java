package pulse.problem.statements;

import java.util.HashMap;
import java.util.Map;

import pulse.properties.NumericProperty;
import pulse.util.PropertyHolder;

/*
 * A class for specifying parameters of the second dimension of the problem
 */

public class SecondDimensionData extends PropertyHolder {
	
	private double d, Bi3, dAv;
	
	protected SecondDimensionData() {
		Bi3 = (double)NumericProperty.DEFAULT_BIOT.getValue();
		d 	= (double)NumericProperty.DEFAULT_DIAMETER.getValue();
		dAv = (double)NumericProperty.DEFAULT_PYROMETER_SPOT.getValue();
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
		return new NumericProperty(d, NumericProperty.DEFAULT_DIAMETER);
	}

	public void setSampleDiameter(NumericProperty d) {
		this.d = (double)d.getValue();
	}

	public NumericProperty getSideLosses() {
		return new NumericProperty(Messages.getString("SecondDimensionData.0"), Bi3, NumericProperty.DEFAULT_BIOT); //$NON-NLS-1$
	}

	public void setSideLosses(NumericProperty bi3) {
		this.Bi3 = (double)bi3.getValue();
	}
	
	public void resetHeatLosses() {
		this.Bi3 = 0;
	}
	
	public NumericProperty getPyrometerSpot() {
		return new NumericProperty(dAv, NumericProperty.DEFAULT_PYROMETER_SPOT); //$NON-NLS-1$
	}

	public void setPyrometerSpot(NumericProperty dAv) {
		this.dAv = (double)dAv.getValue();
	}
	
	@Override
	public Map<String,String> propertyNames() {
		Map<String,String> map = new HashMap<String,String>(2);
		map.put(getSideLosses().getSimpleName(), Messages.getString("SecondDimensionData.1")); //$NON-NLS-1$
		map.put(getSampleDiameter().getSimpleName(), Messages.getString("SecondDimensionData.2")); //$NON-NLS-1$
		map.put(getPyrometerSpot().getSimpleName(), Messages.getString("SecondDimensionData.3")); //$NON-NLS-1$
		return map;
	}
	
}