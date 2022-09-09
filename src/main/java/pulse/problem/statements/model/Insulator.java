package pulse.problem.statements.model;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.REFLECTANCE;

import java.util.Set;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;

public class Insulator extends AbsorptionModel {

    private double R;

    public Insulator() {
        super();
        R = (double) def(REFLECTANCE).getValue();
    }
    
    public Insulator(AbsorptionModel m) {
        super(m);
        if(m instanceof Insulator) {
            R = (double) ((Insulator) m).getReflectance().getValue();
        } else {
            R = (double) def(REFLECTANCE).getValue();
        }
    }

    @Override
    public double absorption(SpectralRange spectrum, double x) {
        double a = (double) (this.getAbsorptivity(spectrum).getValue());
        return a * (Math.exp(-a * x) - R * Math.exp(-a * (2.0 - x))) / (1.0 - R * R * Math.exp(-2.0 * a));
    }

    public NumericProperty getReflectance() {
        return derive(REFLECTANCE, R);
    }

    public void setReflectance(NumericProperty a) {
        NumericProperty.requireType(a, REFLECTANCE);
        this.R = (double) a.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        if (type == REFLECTANCE) {
            setReflectance(property);
        }
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(REFLECTANCE);
        return set;
    }

    @Override
    public AbsorptionModel copy() {
        return new Insulator(this);
    }

}
