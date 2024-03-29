package pulse.problem.statements;

import java.util.List;
import java.util.Set;

import pulse.math.ParameterVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitTranslucentSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.AbsorptionModel;
import pulse.problem.statements.model.BeerLambertAbsorption;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.SOURCE_GEOMETRIC_FACTOR;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.InstanceDescriptor;

public class PenetrationProblem extends ClassicalProblem {

    private static final long serialVersionUID = -6760177658036060627L;
    private InstanceDescriptor<AbsorptionModel> instanceDescriptor
            = new InstanceDescriptor<>(
                    "Absorption Model Selector", AbsorptionModel.class);
    private AbsorptionModel absorption;

    public PenetrationProblem() {
        super();
        instanceDescriptor.setSelectedDescriptor(BeerLambertAbsorption.class.getSimpleName());
        instanceDescriptor.addListener(() -> initAbsorption());
        absorption = instanceDescriptor.newInstance(AbsorptionModel.class);
        absorption.setParent(this);
    }

    public PenetrationProblem(PenetrationProblem p) {
        super(p);
        instanceDescriptor.setSelectedDescriptor(BeerLambertAbsorption.class.getSimpleName());
        instanceDescriptor.addListener(() -> initAbsorption());
        this.absorption = p.getAbsorptionModel().copy();
        this.absorption.setParent(this);
    }

    private void initAbsorption() {
        setAbsorptionModel(instanceDescriptor.newInstance(AbsorptionModel.class));
        firePropertyChanged(this, instanceDescriptor);
    }

    public AbsorptionModel getAbsorptionModel() {
        return absorption;
    }

    public void setAbsorptionModel(AbsorptionModel model) {
        this.absorption = model;
        this.absorption.setParent(this);
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(instanceDescriptor);
        return list;
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.remove(SOURCE_GEOMETRIC_FACTOR);
        return set;
    }

    public InstanceDescriptor<AbsorptionModel> getAbsorptionSelector() {
        return instanceDescriptor;
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        absorption.optimisationVector(output);
    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        absorption.assign(params);
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ImplicitTranslucentSolver.class;
    }

    @Override
    public String toString() {
        return Messages.getString("DistributedProblem.Descriptor");
    }

    @Override
    public Problem copy() {
        return new PenetrationProblem(this);
    }

}
