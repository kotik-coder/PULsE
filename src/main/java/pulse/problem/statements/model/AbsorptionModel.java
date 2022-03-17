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
import pulse.math.ParameterVector;
import pulse.math.Segment;
import static pulse.math.transforms.StandardTransformations.ABS;
import pulse.math.transforms.Transformable;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.Flag;
import static pulse.properties.NumericProperties.derive;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.COMBINED_ABSORPTIVITY;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;
import pulse.search.Optimisable;

public abstract class AbsorptionModel extends PropertyHolder implements Reflexive, Optimisable {

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
    
    @Override
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        for (int i = 0, size = output.dimension(); i < size; i++) {
            var key = output.getIndex(i);
            double value = 0;

            Transformable transform = ABS;
            output.setParameterBounds(i, new Segment(1E-2, 1000.0));
            
            switch (key) {
                case LASER_ABSORPTIVITY:
                    value = (double) (getLaserAbsorptivity()).getValue();
                    break;
                case THERMAL_ABSORPTIVITY:
                    value = (double) (getThermalAbsorptivity()).getValue();
                    break;
                case COMBINED_ABSORPTIVITY:
                    value = (double) (getCombinedAbsorptivity()).getValue();
                    break;
                default:
                    continue;
            }

            //do this for the listed key values 
            output.setTransform(i, transform);
            output.set(i, value);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        double value;

        for (int i = 0, size = params.dimension(); i < size; i++) {
            var key = params.getIndex(i);

            switch (key) {
                case LASER_ABSORPTIVITY:
                case THERMAL_ABSORPTIVITY:
                case COMBINED_ABSORPTIVITY:
                    value = params.inverseTransform(i);
                    break;
                default:
                    continue;
            }

            set(key, derive(key, value));

        }
    }

}
