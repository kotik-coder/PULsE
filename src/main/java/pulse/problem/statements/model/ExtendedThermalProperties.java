package pulse.problem.statements.model;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.DIAMETER;
import static pulse.properties.NumericPropertyKeyword.FOV_INNER;
import static pulse.properties.NumericPropertyKeyword.FOV_OUTER;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;

import java.util.Set;

import pulse.input.ExperimentalData;
import pulse.properties.NumericProperties;
import static pulse.properties.NumericProperties.def;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_COMBINED;

public class ExtendedThermalProperties extends ThermalProperties {

    private static final long serialVersionUID = 452882822074681811L;
    private double d;
    private double Bi3;
    private double fovOuter;
    private double fovInner;

    public ExtendedThermalProperties() {
        super();
        Bi3 = (double) def(HEAT_LOSS_SIDE).getValue();
        d = (double) def(DIAMETER).getValue();
        fovOuter = (double) def(FOV_OUTER).getValue();
        fovInner = (double) def(FOV_INNER).getValue();
        defaultValues();
    }

    public ExtendedThermalProperties(ThermalProperties sdd) {
        super(sdd);
        defaultValues();
    }

    private void defaultValues() {
        Bi3 = (double) def(HEAT_LOSS_SIDE).getValue();
        d = (double) def(DIAMETER).getValue();
        fovOuter = (double) def(FOV_OUTER).getValue();
        fovInner = (double) def(FOV_INNER).getValue();
    }

    public ExtendedThermalProperties(ExtendedThermalProperties sdd) {
        super(sdd);
        this.d = sdd.d;
        this.Bi3 = sdd.Bi3;
        this.fovOuter = sdd.fovOuter;
        this.fovInner = sdd.fovInner;
    }

    @Override
    public ThermalProperties copy() {
        return new ExtendedThermalProperties(this);
    }

    @Override
    public void useTheoreticalEstimates(ExperimentalData c) {
        super.useTheoreticalEstimates(c);
        if (areThermalPropertiesLoaded()) {
            Bi3 = radiationBiot();
        }
    }

    public NumericProperty getSampleDiameter() {
        return derive(DIAMETER, d);
    }

    public void setSampleDiameter(NumericProperty d) {
        requireType(d, DIAMETER);
        this.d = (double) d.getValue();
        firePropertyChanged(this, d);
    }

    public NumericProperty getSideLosses() {
        return derive(HEAT_LOSS_SIDE, Bi3);
    }

    public void setSideLosses(NumericProperty bi3) {
        requireType(bi3, HEAT_LOSS_SIDE);
        this.Bi3 = (double) bi3.getValue();
        firePropertyChanged(this, bi3);
    }

    public NumericProperty getCombinedLosses() {
        return derive(HEAT_LOSS_COMBINED, (double) this.getHeatLoss().getValue());
    }

    public void setCombinedLosses(NumericProperty bic) {
        requireType(bic, HEAT_LOSS_COMBINED);
        double value = (double) bic.getValue();
        setSideLosses(NumericProperties.derive(HEAT_LOSS_SIDE, value));
        setHeatLoss(NumericProperties.derive(HEAT_LOSS, value));
    }

    public NumericProperty getFOVOuter() {
        return derive(FOV_OUTER, fovOuter);
    }

    public void setFOVOuter(NumericProperty fovOuter) {
        requireType(fovOuter, FOV_OUTER);
        this.fovOuter = (double) fovOuter.getValue();
        firePropertyChanged(this, fovOuter);
    }

    public NumericProperty getFOVInner() {
        return derive(FOV_INNER, fovInner);
    }

    public void setFOVInner(NumericProperty fovInner) {
        requireType(fovInner, FOV_INNER);
        this.fovInner = (double) fovInner.getValue();
        firePropertyChanged(this, fovInner);
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(HEAT_LOSS_SIDE);
        set.add(HEAT_LOSS_COMBINED);
        set.add(DIAMETER);
        set.add(FOV_OUTER);
        set.add(FOV_INNER);
        return set;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        switch (type) {
            case FOV_OUTER:
                setFOVOuter(property);
                break;
            case FOV_INNER:
                setFOVInner(property);
                break;
            case DIAMETER:
                setSampleDiameter(property);
                break;
            case HEAT_LOSS_SIDE:
                setSideLosses(property);
                break;
            //extracts the value from the combined heat loss and sets the facial and side heat losses
            case HEAT_LOSS_COMBINED:
                setCombinedLosses(property);
                break;
            default:
                break;
        }
    }

    @Override
    public String getDescriptor() {
        return "Sample Thermo-Physical Properties (2D)";
    }

}
