package pulse.problem.statements.model;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;

import java.util.List;
import java.util.Set;

import pulse.input.ExperimentalData;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
import pulse.properties.Property;

public class ThermoOpticalProperties extends ThermalProperties {

    private double opticalThickness;
    private double planckNumber;
    private double scatteringAlbedo;
    private double scatteringAnisotropy;

    public ThermoOpticalProperties() {
        super();
        this.opticalThickness = (double) def(OPTICAL_THICKNESS).getValue();
        this.planckNumber = (double) def(PLANCK_NUMBER).getValue();
        scatteringAnisotropy = (double) def(SCATTERING_ANISOTROPY).getValue();
        scatteringAlbedo = (double) def(SCATTERING_ALBEDO).getValue();
    }

    public ThermoOpticalProperties(ThermalProperties p) {
        super(p);
        this.opticalThickness = (double) def(OPTICAL_THICKNESS).getValue();
        this.planckNumber = (double) def(PLANCK_NUMBER).getValue();
        scatteringAlbedo = (double) def(SCATTERING_ALBEDO).getValue();
        scatteringAnisotropy = (double) def(SCATTERING_ANISOTROPY).getValue();
    }

    public ThermoOpticalProperties(ThermoOpticalProperties p) {
        super(p);
        this.opticalThickness = p.opticalThickness;
        this.planckNumber = p.planckNumber;
        this.scatteringAlbedo = p.scatteringAlbedo;
        this.scatteringAnisotropy = p.scatteringAnisotropy;
    }

    @Override
    public ThermalProperties copy() {
        return new ThermoOpticalProperties(this);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty value) {
        super.set(type, value);

        switch (type) {
            case PLANCK_NUMBER:
                setPlanckNumber(value);
                break;
            case OPTICAL_THICKNESS:
                setOpticalThickness(value);
                break;
            case SCATTERING_ALBEDO:
                setScatteringAlbedo(value);
                break;
            case SCATTERING_ANISOTROPY:
                setScatteringAnisotropy(value);
                break;
            default:
                break;
        }

    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(PLANCK_NUMBER);
        set.add(OPTICAL_THICKNESS);
        set.add(SCATTERING_ALBEDO);
        set.add(SCATTERING_ANISOTROPY);
        return set;
    }

    public double maxNp() {
        final double l = (double) getSampleThickness().getValue();
        final double T = (double) getTestTemperature().getValue();
        return thermalConductivity() / (4.0 * STEFAN_BOTLZMAN * fastPowLoop(T, 3) * l);
    }

    public NumericProperty getOpticalThickness() {
        return derive(OPTICAL_THICKNESS, opticalThickness);
    }

    public void setOpticalThickness(NumericProperty tau0) {
        requireType(tau0, OPTICAL_THICKNESS);
        this.opticalThickness = (double) tau0.getValue();
        firePropertyChanged(this, tau0);
    }

    public NumericProperty getPlanckNumber() {
        return derive(PLANCK_NUMBER, planckNumber);
    }

    public void setPlanckNumber(NumericProperty planckNumber) {
        requireType(planckNumber, PLANCK_NUMBER);
        this.planckNumber = (double) planckNumber.getValue();
        firePropertyChanged(this, planckNumber);
    }

    public NumericProperty getScatteringAnisostropy() {
        return derive(SCATTERING_ANISOTROPY, scatteringAnisotropy);
    }

    public void setScatteringAnisotropy(NumericProperty A1) {
        requireType(A1, SCATTERING_ANISOTROPY);
        this.scatteringAnisotropy = (double) A1.getValue();
        firePropertyChanged(this, A1);
    }

    public NumericProperty getScatteringAlbedo() {
        return derive(SCATTERING_ALBEDO, scatteringAlbedo);
    }

    public void setScatteringAlbedo(NumericProperty omega0) {
        requireType(omega0, SCATTERING_ALBEDO);
        this.scatteringAlbedo = (double) omega0.getValue();
        firePropertyChanged(this, omega0);
    }

    @Override
    public void useTheoreticalEstimates(ExperimentalData c) {
        super.useTheoreticalEstimates(c);
        if (areThermalPropertiesLoaded()) {
            final double nSq = 4;
            final double lambda = thermalConductivity();
            final double l = (double) getSampleThickness().getValue();
            final double T = (double) getTestTemperature().getValue();
            final double nP = lambda / (4.0 * nSq * STEFAN_BOTLZMAN * fastPowLoop(T, 3) * l);
            setPlanckNumber(derive(PLANCK_NUMBER, nP));
        }
    }

    @Override
    public String getDescriptor() {
        return "Thermo-Physical & Optical Properties";
    }

}
