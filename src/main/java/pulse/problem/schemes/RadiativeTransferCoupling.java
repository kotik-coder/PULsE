package pulse.problem.schemes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.problem.schemes.rte.RadiativeTransferSolver;
import pulse.problem.schemes.rte.dom.DiscreteOrdinatesMethod;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;

public class RadiativeTransferCoupling extends PropertyHolder {

    private RadiativeTransferSolver rte;
    private InstanceDescriptor<? extends RadiativeTransferSolver> instanceDescriptor = new InstanceDescriptor<RadiativeTransferSolver>(
            "RTE Solver Selector", RadiativeTransferSolver.class);

    public RadiativeTransferCoupling() {
        instanceDescriptor.setSelectedDescriptor(DiscreteOrdinatesMethod.class.getSimpleName());
        instanceDescriptor.addListener(() -> firePropertyChanged(this, instanceDescriptor));
        super.parameterListChanged();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        //intentionally blank
    }

    public void init(ParticipatingMedium problem, Grid grid) {

        if (rte == null) {
            newRTE(problem, grid);
            instanceDescriptor.addListener(() -> {
                newRTE(problem, grid);
                rte.init(problem, grid);
            });

        } else {
            rte.init(problem, grid);
        }

    }

    private void newRTE(ParticipatingMedium problem, Grid grid) {
        rte = instanceDescriptor.newInstance(RadiativeTransferSolver.class, problem, grid);
        rte.setParent(this);
    }

    public InstanceDescriptor<? extends RadiativeTransferSolver> getInstanceDescriptor() {
        return instanceDescriptor;
    }

    public RadiativeTransferSolver getRadiativeTransferEquation() {
        return rte;
    }

    public void setRadiativeTransferEquation(RadiativeTransferSolver solver) {
        this.rte = solver;
    }

    @Override
    public String toString() {
        return instanceDescriptor.toString();
    }

    @Override
    public String getPrefix() {
        return "RTE Coupling";
    }

    @Override
    public List<Property> listedTypes() {
        var list = super.listedTypes();
        list.add(instanceDescriptor);
        return list;
    }

}
