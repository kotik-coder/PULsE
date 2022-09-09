package pulse.problem.statements.model;

import java.util.Set;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.GAS_EXCHANGE_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_GAS;
import static pulse.properties.NumericPropertyKeyword.SOLID_EXCHANGE_COEFFICIENT;

public class TwoTemperatureProperties extends ThermalProperties {

    private double exchangeSolid;
    private double exchangeGas;
    private double gasHeatLoss;
   
    public TwoTemperatureProperties() {
        super();
        exchangeSolid = (double) def(SOLID_EXCHANGE_COEFFICIENT).getValue();
        exchangeGas = (double) def(GAS_EXCHANGE_COEFFICIENT).getValue();
        gasHeatLoss = (double) def(HEAT_LOSS_GAS).getValue();
    }

    public TwoTemperatureProperties(ThermalProperties p) {
        super(p);
        if (p instanceof TwoTemperatureProperties) {
            var np = (TwoTemperatureProperties) p;
            this.exchangeSolid = np.exchangeSolid;
            this.exchangeGas = np.exchangeGas;
            this.gasHeatLoss = np.gasHeatLoss;
        }
        else {
            exchangeSolid = (double) def(SOLID_EXCHANGE_COEFFICIENT).getValue();
            exchangeGas = (double) def(GAS_EXCHANGE_COEFFICIENT).getValue();
            gasHeatLoss = (double) def(HEAT_LOSS_GAS).getValue();
        }
    }

    @Override
    public ThermalProperties copy() {
        return new TwoTemperatureProperties(this);
    }

    /**
     * Used to change the parameter values of this {@code Problem}. It is only
     * allowed to use those types of {@code NumericPropery} that are listed by
     * the {@code listedParameters()}.
     *
     * @param type
     * @param value
     * @see listedTypes()
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty value) {
        switch (type) {
            case SOLID_EXCHANGE_COEFFICIENT:
                setSolidExchangeCoefficient(value);
                break;
            case GAS_EXCHANGE_COEFFICIENT:
                setGasExchangeCoefficient(value);
                break;
            case HEAT_LOSS_GAS:
                setGasHeatLoss(value);
                break;
            default:
                super.set(type, value);
        }
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(HEAT_LOSS_GAS);
        set.add(SOLID_EXCHANGE_COEFFICIENT);
        set.add(GAS_EXCHANGE_COEFFICIENT);
        return set;
    }

    public NumericProperty getSolidExchangeCoefficient() {
        return derive(SOLID_EXCHANGE_COEFFICIENT, exchangeSolid);
    }
    
    public NumericProperty getGasExchangeCoefficient() {
        return derive(GAS_EXCHANGE_COEFFICIENT, exchangeGas);
    }

    public void setSolidExchangeCoefficient(NumericProperty p) {
        NumericProperty.requireType(p, SOLID_EXCHANGE_COEFFICIENT);
        this.exchangeSolid = (double) p.getValue();
        firePropertyChanged(this, p);
    }
    
    public void setGasExchangeCoefficient(NumericProperty p) {
        NumericProperty.requireType(p, GAS_EXCHANGE_COEFFICIENT);
        this.exchangeGas = (double) p.getValue();
        firePropertyChanged(this, p);
    }

    public NumericProperty getGasHeatLoss() {
        return derive(HEAT_LOSS_GAS, gasHeatLoss);
    }

    public void setGasHeatLoss(NumericProperty p) {
        NumericProperty.requireType(p, HEAT_LOSS_GAS);
        this.gasHeatLoss = (double) p.getValue();
        firePropertyChanged(this, p);
    }

}
