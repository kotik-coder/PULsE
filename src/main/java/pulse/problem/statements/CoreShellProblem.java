package pulse.problem.statements;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.AXIAL_COATING_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.COATING_DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.RADIAL_COATING_THICKNESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.InvDiamTransform;
import pulse.math.transforms.InvLenSqTransform;
import pulse.math.transforms.InvLenTransform;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ExtendedThermalProperties;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
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
        return tA / (double) getProperties().getSampleThickness().getValue();
    }

    public double radialFactor() {
        return tR / (double) getProperties().getSampleThickness().getValue();
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
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(AXIAL_COATING_THICKNESS);
        set.add(RADIAL_COATING_THICKNESS);
        set.add(COATING_DIFFUSIVITY);
        return set;
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
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);

        var bounds = new Segment(0.1, 1.0);
        var properties = (ExtendedThermalProperties) this.getProperties();

        for (int i = 0, size = output.dimension(); i < size; i++) {
            var key = output.getIndex(i);
            switch (key) {
                case AXIAL_COATING_THICKNESS:
                    output.setTransform(i, new InvLenTransform(properties));
                    output.set(i, tA);
                    output.setParameterBounds(i, bounds);
                    break;
                case RADIAL_COATING_THICKNESS:
                    output.setTransform(i, new InvDiamTransform(properties));
                    output.set(i, tR);
                    output.setParameterBounds(i, bounds);
                    break;
                case COATING_DIFFUSIVITY:
                    output.setTransform(i, new InvLenSqTransform(properties));
                    output.set(i, coatingDiffusivity);
                    output.setParameterBounds(i, new Segment(0.5 * coatingDiffusivity, 1.5 * coatingDiffusivity));
                    break;
                default:
                    continue;
            }
        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);

        for (int i = 0, size = params.dimension(); i < size; i++) {
            switch (params.getIndex(i)) {
                case AXIAL_COATING_THICKNESS:
                    tA = params.inverseTransform(i);
                    break;
                case RADIAL_COATING_THICKNESS:
                    tR = params.inverseTransform(i);
                    break;
                case COATING_DIFFUSIVITY:
                    coatingDiffusivity = params.inverseTransform(i);
                    break;
                default:
                    continue;
            }
        }
    }

}
