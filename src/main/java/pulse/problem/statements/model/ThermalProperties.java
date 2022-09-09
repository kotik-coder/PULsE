package pulse.problem.statements.model;

import static java.lang.Math.PI;
import java.util.List;
import static pulse.input.InterpolationDataset.getDataset;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.*;

import java.util.Set;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.InterpolationDataset.StandartType;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.statements.Pulse2D;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.util.PropertyHolder;

public class ThermalProperties extends PropertyHolder {

    private double a;
    private double l;
    private double Bi;
    private double signalHeight;
    private double cP;
    private double rho;
    private double T;
    private double emissivity;

    public final static double STEFAN_BOTLZMAN = 5.6703E-08; // Stephan-Boltzmann constant

    /**
     * The <b>corrected</b> proportionality factor setting out the relation
     * between the thermal diffusivity and the half-rise time of an
     * {@code ExperimentalData} curve.
     *
     * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i>
     * Journal of Applied Physics <b>32</b> (1961) 1679</a>
     * @see <a href="https://doi.org/10.1016/j.ces.2019.01.014">Parker <i>et
     * al.</i>
     * Chem. Eng. Sci. <b>199</b> (2019) 546-551</a>
     */
    public final double PARKERS_COEFFICIENT = 0.1388; // in mm

    public ThermalProperties() {
        super();
        a = (double) def(DIFFUSIVITY).getValue();
        l = (double) def(THICKNESS).getValue();
        Bi = (double) def(HEAT_LOSS).getValue();
        signalHeight = (double) def(MAXTEMP).getValue();
        T = (double) def(TEST_TEMPERATURE).getValue();
        emissivity = (double) def(EMISSIVITY).getValue();
        initListeners();
        fill();
    }

    public ThermalProperties(ThermalProperties p) {
        super();
        this.l = p.l;
        this.a = p.a;
        this.Bi = p.Bi;
        this.T = p.T;
        this.signalHeight = p.signalHeight;
        this.emissivity = p.emissivity;
        initListeners();
        fill();
    }

    public List<NumericProperty> findMalformedProperties() {
        var list = this.numericData().stream()
                .filter(np -> !np.validate()).collect(Collectors.toList());
        return list;
    }

    private void fill() {
        var rhoCurve = getDataset(StandartType.DENSITY);
        var cpCurve = getDataset(StandartType.HEAT_CAPACITY);
        if (rhoCurve != null) {
            rho = rhoCurve.interpolateAt(T);
        }
        if (cpCurve != null) {
            cP = cpCurve.interpolateAt(T);
        }
    }

    /**
     * Calculates some or all of the following properties:
     * <math><i>C</i><sub>p</sub>, <i>&rho;</i>, <i>&labmda;</i>,
     * <i>&epsilon;</i></math>.
     * <p>
     * These properties will be calculated only if the necessary
     * {@code InterpolationDataset}s were previously loaded by the
     * {@code TaskManager}.
     * </p>
     */
    private void initListeners() {

        InterpolationDataset.addListener(e -> {
            if (getParent() == null) {
                return;
            }

            if (e == StandartType.DENSITY) {
                rho = getDataset(StandartType.DENSITY).interpolateAt(T);
            } else if (e == StandartType.HEAT_CAPACITY) {
                cP = getDataset(StandartType.HEAT_CAPACITY).interpolateAt(T);
            }

        });

    }

    public ThermalProperties copy() {
        return new ThermalProperties(this);
    }

    /**
     * Hides optimiser directives
     *
     * @return true
     */
    @Override
    public boolean areDetailsHidden() {
        return true;
    }

    /**
     * Used to change the parameter values of this {@code Problem}. It is only
     * allowed to use those types of {@code NumericPropery} that are listed by
     * the {@code listedParameters()}.
     *
     * @param value
     * @see listedTypes()
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty value) {
        switch (type) {
            case DIFFUSIVITY:
                setDiffusivity(value);
                break;
            case MAXTEMP:
                setMaximumTemperature(value);
                break;
            case THICKNESS:
                setSampleThickness(value);
                break;
            case HEAT_LOSS:
                setHeatLoss(value);
                break;
            case SPECIFIC_HEAT:
                setSpecificHeat(value);
                break;
            case DENSITY:
                setDensity(value);
                break;
            case TEST_TEMPERATURE:
                setTestTemperature(value);
                break;
            default:
                break;
        }
    }

    public void setHeatLoss(NumericProperty Bi) {
        requireType(Bi, HEAT_LOSS);
        this.Bi = (double) Bi.getValue();
        if(areThermalPropertiesLoaded()) {
            calculateEmissivity();
        }
        firePropertyChanged(this, Bi);
    }

    public NumericProperty getDiffusivity() {
        return derive(DIFFUSIVITY, a);
    }

    public void setDiffusivity(NumericProperty a) {
        requireType(a, DIFFUSIVITY);
        this.a = (double) a.getValue();
        firePropertyChanged(this, a);
    }

    public NumericProperty getMaximumTemperature() {
        return derive(MAXTEMP, signalHeight);
    }

    public void setMaximumTemperature(NumericProperty maxTemp) {
        requireType(maxTemp, MAXTEMP);
        this.signalHeight = (double) maxTemp.getValue();
        firePropertyChanged(this, maxTemp);
    }

    public NumericProperty getSampleThickness() {
        return derive(THICKNESS, l);
    }

    public void setSampleThickness(NumericProperty l) {
        requireType(l, THICKNESS);
        this.l = (double) l.getValue();
        firePropertyChanged(this, l);
    }

    /**
     * <p>
     * Assuming that <code>Bi<sub>1</sub> = Bi<sub>2</sub></code>, returns the
     * value of <code>Bi<sub>1</sub></code>. If <code>Bi<sub>1</sub> =
     * Bi<sub>2</sub></code>, this will print a warning message (but will not
     * throw an exception)
     * </p>
     *
     * @return Bi<sub>1</sub> as a {@code NumericProperty}
     */
    public NumericProperty getHeatLoss() {
        return derive(HEAT_LOSS, Bi);
    }

    public NumericProperty getSpecificHeat() {
        return derive(SPECIFIC_HEAT, cP);
    }

    public void setSpecificHeat(NumericProperty cP) {
        requireType(cP, SPECIFIC_HEAT);
        this.cP = (double) cP.getValue();
        firePropertyChanged(this, cP);
    }

    public NumericProperty getDensity() {
        return derive(DENSITY, rho);
    }

    public void setDensity(NumericProperty p) {
        requireType(p, DENSITY);
        this.rho = (double) (p.getValue());
        firePropertyChanged(this, p);
    }

    public NumericProperty getTestTemperature() {
        return derive(TEST_TEMPERATURE, T);
    }

    public void setTestTemperature(NumericProperty T) {
        requireType(T, TEST_TEMPERATURE);
        this.T = (double) T.getValue();

        var heatCapacity = getDataset(HEAT_CAPACITY);

        if (heatCapacity != null) {
            cP = heatCapacity.interpolateAt(this.T);
        }

        var density = getDataset(StandartType.DENSITY);

        if (density != null) {
            rho = density.interpolateAt(this.T);
        }

        firePropertyChanged(this, T);
    }

    /**
     * Listed parameters include:
     * <code>MAXTEMP, DIFFUSIVITY, THICKNESS, HEAT_LOSS_FRONT, HEAT_LOSS_REAR</code>.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(MAXTEMP);
        set.add(DIFFUSIVITY);
        set.add(THICKNESS);
        set.add(HEAT_LOSS);
        set.add(DENSITY);
        set.add(SPECIFIC_HEAT);
        set.add(EMISSIVITY);
        return set;
    }

    public final double thermalConductivity() {
        return a * getThermalMass();
    }

    public NumericProperty getThermalConductivity() {
        return derive(CONDUCTIVITY, thermalConductivity());
    }

    public void calculateEmissivity() {
        double newEmissivity = Bi * thermalConductivity() / (4. * Math.pow(T, 3) * l * STEFAN_BOTLZMAN);
        var transform = new StickTransform(Segment.boundsFrom(EMISSIVITY));
        setEmissivity(derive(EMISSIVITY, 
                transform.transform(newEmissivity))
        );
    }
    
    /**
     * Calculates the radiative Biot number.
     * @return the radiative Biot number.
     */
    
    public double radiationBiot() {
        double lambda = thermalConductivity();
        return 4.0 * emissivity * STEFAN_BOTLZMAN * Math.pow(T, 3) * l / lambda;
    }
    
    /**
     * Calculates the maximum Biot number at these conditions, which
     * corresponds to an emissivity of unity. If emissivity is non-positive,
     * returns the maximum Biot number defined in the XML file.
     * @return the maximum Biot number
     */
    
    public double maxRadiationBiot() {
        double absMax = Segment.boundsFrom(HEAT_LOSS).getMaximum();
        return emissivity > 0 ? radiationBiot() / emissivity : absMax;         
    }

    /**
     * Performs simple calculation of the <math><i>l<sup>2</sup>/a</i></math>
     * factor that is commonly used to evaluate the dimensionless time
     * {@code t/timeFactor}.
     *
     * @return the time factor
     */
    public double timeFactor() {
        return l * l / a;
    }
    
    public double getThermalMass() {
        return cP * rho;
    }

    /**
     * Calculates the half-rise time <i>t</i><sub>1/2</sub> of {@code c} and
     * uses it to estimate the thermal diffusivity of this problem:
     * <code><i>a</i>={@value PARKERS_COEFFICIENT}*<i>l</i><sup>2</sup>/<i>t</i><sub>1/2</sub></code>.
     *
     * @param c the {@code ExperimentalData} used to estimate the thermal
     * diffusivity value
     * @see pulse.input.ExperimentalData.halfRiseTime()
     */
    public void useTheoreticalEstimates(ExperimentalData c) {
        final double t0 = c.getHalfTimeCalculator().getHalfTime();
        this.a = PARKERS_COEFFICIENT * l * l / t0;
        if (areThermalPropertiesLoaded()) {
            Bi = radiationBiot();
        }
    }

    public final boolean areThermalPropertiesLoaded() {
        return (Double.compare(cP, 0.0) > 0 && Double.compare(rho, 0.0) > 0);
    }

    public double maximumHeating(Pulse2D pulse) {
        final double Q = (double) pulse.getLaserEnergy().getValue();
        final double dLas = (double) pulse.getSpotDiameter().getValue();
        return 4.0 * emissivity * Q / (PI * dLas * dLas * l * getThermalMass() );
    }

    public NumericProperty getEmissivity() {
        return derive(EMISSIVITY, emissivity);
    }

    public void setEmissivity(NumericProperty e) {
        requireType(e, EMISSIVITY);
        this.emissivity = (double) e.getValue();
        firePropertyChanged(this, e);
    }

    @Override
    public String getDescriptor() {
        return "Sample Thermo-Physical Properties";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getDescriptor());
        sb.append(":");
        sb.append(String.format("%n %-25s", this.getDiffusivity()));
        sb.append(String.format("%n %-25s", this.getMaximumTemperature()));
        sb.append(String.format("%n %-25s", this.getHeatLoss()));
        return sb.toString();
    }

}