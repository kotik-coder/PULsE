package pulse.problem.statements;

import static java.lang.Math.pow;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.AXIAL_COATING_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.COATING_DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.RADIAL_COATING_THICKNESS;

import java.util.ArrayList;
import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.statements.model.ExtendedThermalProperties;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class CoreShellProblem extends ClassicalProblem2D {

	private double tA;
	private double tR;
	private double coatingDiffusivity;
	private final static boolean DEBUG = true;

	public CoreShellProblem() {
		super();
		tA = (double) def(AXIAL_COATING_THICKNESS).getValue();
		tR = (double) def(RADIAL_COATING_THICKNESS).getValue();
		coatingDiffusivity = (double) def(COATING_DIFFUSIVITY).getValue();
		setComplexity(ProblemComplexity.HIGH);
	}

	@Override
	public String toString() {
		return Messages.getString("UniformlyCoatedSample.Descriptor");
	}

	public NumericProperty getCoatingAxialThickness() {
		return derive(AXIAL_COATING_THICKNESS, tA);
	}

	public NumericProperty getCoatingRadialThickness() {
		return derive(RADIAL_COATING_THICKNESS, tR);
	}

	public double axialFactor() {
		return tA / (double)getProperties().getSampleThickness().getValue();
	}

	public double radialFactor() {
		return tR / (double)getProperties().getSampleThickness().getValue();
	}

	public void setCoatingAxialThickness(NumericProperty t) {
		this.tA = (double) t.getValue();
	}

	public void setCoatingRadialThickness(NumericProperty t) {
		this.tR = (double) t.getValue();
	}

	public NumericProperty getCoatingDiffusivity() {
		return derive(COATING_DIFFUSIVITY, coatingDiffusivity);
	}

	public void setCoatingDiffusivity(NumericProperty a) {
		this.coatingDiffusivity = (double) a.getValue();
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.addAll(super.listedTypes());
		list.add(def(AXIAL_COATING_THICKNESS));
		list.add(def(RADIAL_COATING_THICKNESS));
		list.add(def(COATING_DIFFUSIVITY));
		return list;
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case COATING_DIFFUSIVITY:
			setCoatingDiffusivity(property);
			break;
		case AXIAL_COATING_THICKNESS:
			setCoatingAxialThickness(property);
			break;
		case RADIAL_COATING_THICKNESS:
			setCoatingRadialThickness(property);
			break;
		default:
			super.set(type, property);
			break;
		}
	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			switch (output[0].getIndex(i)) {
			case AXIAL_COATING_THICKNESS:
				output[0].set(i, tA / (double)getProperties().getSampleThickness().getValue());
				output[1].set(i, 0.01);
				break;
			case RADIAL_COATING_THICKNESS:
				final double d = (double)((ExtendedThermalProperties)getProperties()).getSampleDiameter().getValue();
				output[0].set(i, 2.0 * tR / d);
				output[1].set(i, 0.01);
				break;
			case COATING_DIFFUSIVITY:
				double value = coatingDiffusivity / pow(tA + 2.0 * tR, -2);
				output[0].set(i, value);
				output[1].set(i, 0.75 * value);
				break;
			default:
				continue;
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {
			case AXIAL_COATING_THICKNESS:
				final double l = (double)((ExtendedThermalProperties)getProperties()).getSampleThickness().getValue();
				tA = params.get(i) * l;
				break;
			case RADIAL_COATING_THICKNESS:
				final double d = (double)((ExtendedThermalProperties)getProperties()).getSampleDiameter().getValue();
				tR = params.get(i) / (d / 2.0);
				break;
			case COATING_DIFFUSIVITY:
				coatingDiffusivity = params.get(i) * pow(tA + 2.0 * tR, 2);
				break;
			default:
				continue;
			}
		}
	}

}