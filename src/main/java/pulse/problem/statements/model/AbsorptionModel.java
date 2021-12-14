package pulse.problem.statements.model;

import static pulse.problem.statements.model.SpectralRange.LASER;
import static pulse.problem.statements.model.SpectralRange.THERMAL;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.LASER_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.THERMAL_ABSORPTIVITY;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.COMBINED_ABSORPTIVITY;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class AbsorptionModel extends PropertyHolder implements Reflexive {

    private Map<SpectralRange, NumericProperty> absorptionMap;

    protected AbsorptionModel() {
        setPrefix("Absorption model");
        absorptionMap = new HashMap<>();
        absorptionMap.put(LASER, def(LASER_ABSORPTIVITY));
        absorptionMap.put(THERMAL, def(THERMAL_ABSORPTIVITY));
    }

    public abstract double absorption(SpectralRange range, double x);

    public NumericProperty getLaserAbsorptivity() {
        return absorptionMap.get(LASER);
    }

    public NumericProperty getThermalAbsorptivity() {
        return absorptionMap.get(THERMAL);
    }

    public NumericProperty getCombinedAbsorptivity() {
        return getThermalAbsorptivity();
    }

    public NumericProperty getAbsorptivity(SpectralRange spectrum) {
        return absorptionMap.get(spectrum);
    }

    public void setAbsorptivity(SpectralRange range, NumericProperty a) {
        absorptionMap.put(range, a);
    }

    public void setLaserAbsorptivity(NumericProperty a) {
        absorptionMap.put(LASER, a);
    }

    public void setThermalAbsorptivity(NumericProperty a) {
        absorptionMap.put(THERMAL, a);
    }

    public void setCombinedAbsorptivity(NumericProperty a) {
        setThermalAbsorptivity(a);
        setLaserAbsorptivity(a);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {

        switch (type) {
            case LASER_ABSORPTIVITY:
                absorptionMap.put(LASER, property);
                break;
            case THERMAL_ABSORPTIVITY:
                absorptionMap.put(THERMAL, property);
                break;
            case COMBINED_ABSORPTIVITY:
                setCombinedAbsorptivity(property);
                break;
            default:
                break;
        }

    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " : " + absorptionMap.get(LASER) + " ; " + absorptionMap.get(THERMAL);
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(LASER_ABSORPTIVITY);
        set.add(THERMAL_ABSORPTIVITY);
        set.add(COMBINED_ABSORPTIVITY);
        return set;
    }

}
