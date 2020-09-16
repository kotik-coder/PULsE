package pulse.problem.schemes;

import static pulse.problem.schemes.Partition.Location.CORE_X;
import static pulse.problem.schemes.Partition.Location.CORE_Y;
import static pulse.problem.schemes.Partition.Location.FRONT_Y;
import static pulse.problem.schemes.Partition.Location.REAR_Y;
import static pulse.problem.schemes.Partition.Location.SIDE_X;
import static pulse.problem.schemes.Partition.Location.SIDE_Y;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.SHELL_GRID_DENSITY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pulse.problem.schemes.Partition.Location;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;

public class LayeredGrid2D extends Grid2D {

	private Map<Location, Partition> h;

	public LayeredGrid2D(Map<Location, Partition> partitions, NumericProperty timeFactor) {
		h = new HashMap<>(partitions);
		setGridDensity(derive(CORE_Y.densityKeyword(), partitions.get(CORE_Y).getDensity()));
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
		h.get(location).setDensity((int) density.getValue());
	}

	@Override
	public void setGridDensity(NumericProperty gridDensity) {
		super.setGridDensity(gridDensity);
		setDensity(CORE_X, gridDensity);
		setDensity(CORE_Y, gridDensity);
	}

	public NumericProperty getGridDensity(Location location) {
		return derive(location.densityKeyword(), h.get(location).getDensity());
	}

	@Override
	public NumericProperty getGridDensity() {
		return getGridDensity(CORE_X);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>(2);
		list.add(def(SHELL_GRID_DENSITY));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case SHELL_GRID_DENSITY:
			setDensity(FRONT_Y, property);
			setDensity(REAR_Y, property);
			setDensity(SIDE_X, property);
			setDensity(SIDE_Y, property);
			break;
		default:
			super.set(type, property);
		}
	}

}