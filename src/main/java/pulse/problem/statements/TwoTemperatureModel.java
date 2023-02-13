package pulse.problem.statements;

import java.util.List;
import pulse.math.Parameter;
import static pulse.properties.NumericProperties.derive;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitTwoTemperatureSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.Gas;
import pulse.problem.statements.model.Helium;
import pulse.problem.statements.model.ThermalProperties;
import pulse.problem.statements.model.TwoTemperatureProperties;
import pulse.properties.NumericProperty;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import pulse.properties.Property;
import pulse.ui.Messages;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyEvent;

public class TwoTemperatureModel extends PenetrationProblem {

    private static final long serialVersionUID = 2567125396986165234L;

    private Gas gas;

    private InstanceDescriptor<Gas> instanceDescriptor
            = new InstanceDescriptor<>("Gas Selector", Gas.class);

    public TwoTemperatureModel() {
        super();
        setComplexity(ProblemComplexity.MODERATE);
        instanceDescriptor.setSelectedDescriptor(Helium.class.getSimpleName());
        setGas(instanceDescriptor.newInstance(Gas.class));
        addListeners();
        gas.evaluate((double) this.getProperties().getTestTemperature().getValue());
    }

    public TwoTemperatureModel(TwoTemperatureModel p) {
        super(p);
        this.gas = p.gas;
        instanceDescriptor.setSelectedDescriptor(gas.getClass().getSimpleName());
        addListeners();
        gas.evaluate((double) this.getProperties().getTestTemperature().getValue());
    }

    private void addListeners() {
        instanceDescriptor.addListener(() -> setGas(instanceDescriptor.newInstance(Gas.class)));
        this.getProperties().addListener((PropertyEvent event) -> {
            pulse.properties.Property p1 = event.getProperty();
            if (p1 instanceof NumericProperty) {
                pulse.properties.NumericPropertyKeyword npType = ((NumericProperty) p1).getType();
                if (npType == TEST_TEMPERATURE) {
                    gas.evaluate((double) p1.getValue());
                }
            }
        });
    }

    @Override
    public void initProperties() {
        setProperties(new TwoTemperatureProperties());
    }

    @Override
    public void initProperties(ThermalProperties properties) {
        setProperties(new TwoTemperatureProperties(properties));
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        var ttp = (TwoTemperatureProperties) getProperties();

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();
            Segment bounds = Segment.boundsFrom(p.getIdentifier().getKeyword());
            double value;
            switch (key) {
                case SOLID_EXCHANGE_COEFFICIENT:
                    value = (double) ttp.getSolidExchangeCoefficient().getValue();
                    break;
                case GAS_EXCHANGE_COEFFICIENT:
                    value = (double) ttp.getGasExchangeCoefficient().getValue();
                    break;
                case HEAT_LOSS_GAS:
                    value = (double) ttp.getGasHeatLoss().getValue();
                    break;
                default:
                    continue;
            }

            p.setTransform(new StickTransform(bounds));
            p.setValue(value);
            p.setBounds(bounds);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var ttp = (TwoTemperatureProperties) getProperties();

        for (Parameter p : params.getParameters()) {

            var key = p.getIdentifier().getKeyword();
            var np = derive(key, p.inverseTransform());

            switch (key) {
                case SOLID_EXCHANGE_COEFFICIENT:
                    ttp.setSolidExchangeCoefficient(np);
                    break;
                case GAS_EXCHANGE_COEFFICIENT:
                    ttp.setGasExchangeCoefficient(np);
                    break;
                case HEAT_LOSS_GAS:
                    ttp.setGasHeatLoss(np);
                    break;
                default:
            }

        }

    }

    @Override
    public String toString() {
        return Messages.getString("TwoTemperatureModel.Descriptor");
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ImplicitTwoTemperatureSolver.class;
    }

    @Override
    public TwoTemperatureModel copy() {
        return new TwoTemperatureModel(this);
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(instanceDescriptor);
        return list;
    }

    public InstanceDescriptor<Gas> getGasSelector() {
        return instanceDescriptor;
    }

    public Gas getGas() {
        return gas;
    }

    public final void setGas(Gas gas) {
        this.gas = gas;
        gas.evaluate((double) getProperties().getTestTemperature().getValue());
        firePropertyChanged(this, instanceDescriptor);
    }

    /**
     * Diffusivity of solid over diffusivity of gas
     *
     * @return
     */
    public double diffusivityRatio() {
        return (double) getProperties().getDiffusivity().getValue()
                / gas.thermalDiffusivity();
    }

}
