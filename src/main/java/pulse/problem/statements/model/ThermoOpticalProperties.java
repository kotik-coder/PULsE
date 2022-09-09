package pulse.problem.statements.model;

import static pulse.math.MathUtils.fastPowLoop;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ALBEDO;
import static pulse.properties.NumericPropertyKeyword.SCATTERING_ANISOTROPY;

import java.util.Set;

import pulse.input.ExperimentalData;
import pulse.math.Parameter;
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.math.transforms.Transformable;
import pulse.problem.schemes.solvers.SolverException;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_CONVECTIVE;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import pulse.search.Optimisable;

public class ThermoOpticalProperties extends ThermalProperties implements Optimisable {

    private double opticalThickness;
    private double planckNumber;
    private double scatteringAlbedo;
    private double scatteringAnisotropy;
    private double convectiveLosses;

    public ThermoOpticalProperties() {
        super();
        this.opticalThickness   = (double) def(OPTICAL_THICKNESS).getValue();
        this.planckNumber       = (double) def(PLANCK_NUMBER).getValue();
        scatteringAnisotropy    = (double) def(SCATTERING_ANISOTROPY).getValue();
        scatteringAlbedo        = (double) def(SCATTERING_ALBEDO).getValue();
        convectiveLosses        = (double) def(HEAT_LOSS_CONVECTIVE).getValue();
    }

    public ThermoOpticalProperties(ThermalProperties p) {
        super(p);
        opticalThickness        = (double) def(OPTICAL_THICKNESS).getValue();
        planckNumber            = (double) def(PLANCK_NUMBER).getValue();
        scatteringAlbedo        = (double) def(SCATTERING_ALBEDO).getValue();
        scatteringAnisotropy    = (double) def(SCATTERING_ANISOTROPY).getValue();
        convectiveLosses        = (double) def(HEAT_LOSS_CONVECTIVE).getValue();
    }

    public ThermoOpticalProperties(ThermoOpticalProperties p) {
        super(p);
        this.opticalThickness       = p.opticalThickness;
        this.planckNumber           = p.planckNumber;
        this.scatteringAlbedo       = p.scatteringAlbedo;
        this.scatteringAnisotropy   = p.scatteringAnisotropy;
        this.convectiveLosses       = p.convectiveLosses;
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
            case HEAT_LOSS_CONVECTIVE:
                setConvectiveLosses(value);
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
        set.add(HEAT_LOSS_CONVECTIVE);
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
    
    public void setConvectiveLosses(NumericProperty losses) {
        requireType(losses, HEAT_LOSS_CONVECTIVE);
        this.convectiveLosses = (double) losses.getValue();
        firePropertyChanged(this, losses);
    }

    public NumericProperty getConvectiveLosses() {
        return derive(HEAT_LOSS_CONVECTIVE, convectiveLosses);
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
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(super.toString());
        sb.append(String.format("%n %-25s", this.getConvectiveLosses()));
        sb.append(String.format("%n %-25s", this.getOpticalThickness()));
        sb.append(String.format("%n %-25s", this.getPlanckNumber()));
        sb.append(String.format("%n %-25s", this.getScatteringAlbedo()));
        sb.append(String.format("%n %-25s", this.getScatteringAnisostropy()));
        sb.append(String.format("%n %-25s", this.getSpecificHeat()));
        sb.append(String.format("%n %-25s", this.getDensity()));
        return sb.toString();
    }
    
    @Override
    public void optimisationVector(ParameterVector output) {
        Segment bounds = null;
        double value;
        Transformable transform;

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            switch (key) {
                case PLANCK_NUMBER:
                    final double lowerBound = Segment.boundsFrom(PLANCK_NUMBER).getMinimum();
                    bounds = new Segment(lowerBound, maxNp());
                    value = planckNumber;
                    break;
                case OPTICAL_THICKNESS:
                    value = opticalThickness;
                    bounds = Segment.boundsFrom(OPTICAL_THICKNESS);
                    break;
                case SCATTERING_ALBEDO:
                    value = scatteringAlbedo;
                    bounds = Segment.boundsFrom(SCATTERING_ALBEDO);
                    break;
                case SCATTERING_ANISOTROPY:
                    value = scatteringAnisotropy;
                    bounds = Segment.boundsFrom(SCATTERING_ANISOTROPY);
                    break;
                case HEAT_LOSS_CONVECTIVE:
                    value = convectiveLosses;
                    bounds = Segment.boundsFrom(HEAT_LOSS_CONVECTIVE);
                    break;
                case HEAT_LOSS:
                    value  = (double) getHeatLoss().getValue();
                    bounds = new Segment(0.0, maxRadiationBiot() );
                    break;   
                default:
                    continue;

            }

            transform = new StickTransform(bounds);
            p.setTransform(transform);
            p.setValue(value);
            p.setBounds(bounds);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {

        for (Parameter p : params.getParameters()) {

            var type = p.getIdentifier().getKeyword();

            switch (type) {

                case PLANCK_NUMBER:
                case SCATTERING_ALBEDO:
                case SCATTERING_ANISOTROPY:
                case OPTICAL_THICKNESS:
                case HEAT_LOSS_CONVECTIVE:
                    set(type, derive(type, p.inverseTransform()));
                    break;
                default:
                    break;

            }

        }

    }

}
