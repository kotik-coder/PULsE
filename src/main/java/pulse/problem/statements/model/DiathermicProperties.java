package pulse.problem.statements.model;

import java.util.Set;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import static pulse.properties.NumericProperty.requireType;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_CONVECTIVE;

public class DiathermicProperties extends ThermalProperties {

    private static final long serialVersionUID = 1294930368429607512L;
    private double diathermicCoefficient;
    private double convectiveLosses;

    public DiathermicProperties() {
        super();
        this.diathermicCoefficient = (double) def(DIATHERMIC_COEFFICIENT).getValue();
        this.convectiveLosses = (double) def(HEAT_LOSS_CONVECTIVE).getValue();
    }

    public DiathermicProperties(ThermalProperties p) {
        super(p);
        var property = p instanceof DiathermicProperties
                ? ((DiathermicProperties) p).getDiathermicCoefficient()
                : def(DIATHERMIC_COEFFICIENT);
        this.diathermicCoefficient = (double) property.getValue();
        this.convectiveLosses = (double) property.getValue();
    }

    @Override
    public ThermalProperties copy() {
        return new ThermalProperties(this);
    }

    public NumericProperty getDiathermicCoefficient() {
        return derive(DIATHERMIC_COEFFICIENT, diathermicCoefficient);
    }

    public void setDiathermicCoefficient(NumericProperty diathermicCoefficient) {
        requireType(diathermicCoefficient, DIATHERMIC_COEFFICIENT);
        this.diathermicCoefficient = (double) diathermicCoefficient.getValue();
    }

    public NumericProperty getConvectiveLosses() {
        return derive(HEAT_LOSS_CONVECTIVE, convectiveLosses);
    }

    public void setConvectiveLosses(NumericProperty convectiveLosses) {
        requireType(convectiveLosses, HEAT_LOSS_CONVECTIVE);
        this.convectiveLosses = (double) convectiveLosses.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        double value = ((Number) property.getValue()).doubleValue();
        switch (type) {
            case DIATHERMIC_COEFFICIENT:
                diathermicCoefficient = value;
                break;
            case HEAT_LOSS_CONVECTIVE:
                convectiveLosses = value;
                break;
            default:
                super.set(type, property);
                break;
        }
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(DIATHERMIC_COEFFICIENT);
        set.add(HEAT_LOSS_CONVECTIVE);
        return set;
    }

}
