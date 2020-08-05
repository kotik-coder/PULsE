package pulse.problem.schemes.rte.dom;

import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.DOM_GRID_DENSITY;
import static pulse.properties.NumericPropertyKeyword.GRID_STRETCHING_FACTOR;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public class StretchedGrid extends PropertyHolder {

	private double[] nodes;

	private double stretchingFactor;
	private double dimension;

	/**
	 * Constructs a uniform grid where the dimension is set to the argument. The stretching factor 
	 * and grid density are set to a default value.
	 * @param dimension the dimension of the grid
	 */
	
	public StretchedGrid(double dimension) {
		this(theDefault(DOM_GRID_DENSITY), dimension, theDefault(GRID_STRETCHING_FACTOR), true);
	}
	
	/**
	 * Constructs a non-uniform grid where the dimension and the grid density are specified by the arguments. The stretching factor 
	 * and grid density are set to a default value.
	 * @param gridDensity the grid density, which is a property of the {@code DOM_GRID_DENSITY} type
	 * @param dimension the dimension of the grid
	 */

	public StretchedGrid(NumericProperty gridDensity, double dimension) {
		this(gridDensity, dimension, theDefault(GRID_STRETCHING_FACTOR), false);
	}

	protected StretchedGrid(NumericProperty gridDensity, double dimension, NumericProperty stretchingFactor, boolean uniform) {
		this.stretchingFactor = (double) stretchingFactor.getValue();
		this.dimension = dimension;
		int n = (int) gridDensity.getValue();
		if (uniform)
			generateUniformBase(n, true);
		else
			generate(n);
	}

	public void generate(int n) {
		generateUniformBase(n, false);

		// apply stretching function

		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = 0.5 * dimension * tanh(nodes[i], stretchingFactor);
		}

	}

	public void generateUniform(boolean scaled) {
		int n1 = (int) theDefault(DOM_GRID_DENSITY).getValue();
		generateUniformBase(n1, scaled);
	}

	public void generateUniformBase(int n, boolean scaled) {
		nodes = new double[n + 1];
		double h = (scaled ? dimension : 1.0) / n;

		for (int i = 0; i < nodes.length; i++) {
			nodes[i] = i * h;
		}

	}

	public int getDensity() {
		return nodes.length - 1;
	}

	public NumericProperty getStretchingFactor() {
		return derive(GRID_STRETCHING_FACTOR, stretchingFactor);
	}

	public void setStretchingFactor(NumericProperty p) {
		if (p.getType() != GRID_STRETCHING_FACTOR)
			throw new IllegalArgumentException("Illegal type: " + p.getType());
		this.stretchingFactor = (double) p.getValue();
	}

	public double getDimension() {
		return dimension;
	}

	public double getNode(int i) {
		return nodes[i];
	}

	public double[] getNodes() {
		return nodes;
	}

	public double step(int i, double sign) {
		return nodes[i + (int) ((1. + sign) * 0.5)] - nodes[i - (int) ((1. - sign) * 0.5)];
	}

	public double stepLeft(int i) {
		return nodes[i] - nodes[i - 1];
	}

	public double stepRight(int i) {
		return nodes[i + 1] - nodes[i];
	}

	public double tanh(final double x, final double stretchingFactor) {
		return 1.0 - Math.tanh(stretchingFactor * (1.0 - 2.0 * x)) / Math.tanh(stretchingFactor);
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case GRID_STRETCHING_FACTOR:
			setStretchingFactor(property);
			break;
		default:
			throw new IllegalArgumentException("Unknown type: " + type);
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(theDefault(GRID_STRETCHING_FACTOR));
		list.add(theDefault(DOM_GRID_DENSITY));
		return list;
	}

	@Override
	public String toString() {
		return "{ " + derive(DOM_GRID_DENSITY, getDensity()) + " ; " + getStretchingFactor() + " }";
	}

	@Override
	public String getDescriptor() {
		return "Adaptive grid";
	}

}