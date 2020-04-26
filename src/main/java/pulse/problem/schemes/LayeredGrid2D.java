package pulse.problem.schemes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.problem.schemes.Partition.Location;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class LayeredGrid2D extends Grid2D {

	private Map<Location,Partition> h;

	public LayeredGrid2D(Map<Location,Partition> partitions, NumericProperty timeFactor) {
		h = new HashMap<Location,Partition>(partitions);			
		setGridDensity( NumericProperty.derive(
				Location.CORE_Y.densityKeyword(), 
				partitions.get(Location.CORE_Y).getDensity()) );
		setTimeFactor(timeFactor);
	}

	public Partition getPartition(Location location) {
		return h.get(location);
	}
	
	@Override
	public Grid2D copy() {
		return new LayeredGrid2D(h, getTimeFactor());
	}
	
	private void setDensity(Location location, NumericProperty density) {
		h.get(location).setDensity((int)density.getValue());
	}
	
	@Override
	public void setGridDensity(NumericProperty gridDensity) {
		super.setGridDensity(gridDensity);
		setDensity(Location.CORE_X, gridDensity);
		setDensity(Location.CORE_Y, gridDensity);
	}
	
	public NumericProperty getGridDensity(Location location) {
		return NumericProperty.derive( location.densityKeyword(), h.get(location).getDensity() );
	}
	
	@Override
	public NumericProperty getGridDensity() {
		return getGridDensity(Location.CORE_X);
	}

	
	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>(2);
		list.add(NumericProperty.def(NumericPropertyKeyword.SHELL_GRID_DENSITY));
		return list;
	}
	
	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch(type) {
		case SHELL_GRID_DENSITY		: 
			setDensity(Location.FRONT_Y, property); 
			setDensity(Location.REAR_Y, property);
			setDensity(Location.SIDE_X, property);
			setDensity(Location.SIDE_Y, property); 
			break;
		default:
			super.set(type, property);
		}
	}
	
}