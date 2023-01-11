package pulse.problem.statements;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;

import java.util.List;
import java.util.Set;
import pulse.input.ExperimentalData;

import pulse.problem.laser.PulseTemporalShape;
import pulse.problem.laser.RectangularPulse;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolder;

/**
 * A {@code Pulse} stores the parameters of the laser pulse, but does not
 * provide the calculation facilities.
 *
 * @see pulse.problem.laser.DiscretePulse
 *
 */
public class Pulse extends PropertyHolder {

    private double pulseWidth;
    private double laserEnergy;

    private PulseTemporalShape pulseShape;

    private InstanceDescriptor<? extends PulseTemporalShape> instanceDescriptor = new InstanceDescriptor<PulseTemporalShape>(
            "Pulse Shape Selector", PulseTemporalShape.class);

    /**
     * Creates a {@code Pulse} with default values of pulse width and laser spot
     * diameter (as per XML specification) and with a default pulse temporal
     * shape (rectangular).
     *
     */
    public Pulse() {
        super();
        pulseWidth = (double) def(PULSE_WIDTH).getValue();
        laserEnergy = (double) def(LASER_ENERGY).getValue();
        instanceDescriptor.setSelectedDescriptor(RectangularPulse.class.getSimpleName());
        initShape();
        instanceDescriptor.addListener(() -> {
            initShape();
            this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
        });
        addListeners();
    }

    /**
     * Copy constructor
     *
     * @param p the pulse, parameters of which will be copied.
     */
    public Pulse(Pulse p) {
        super();
        this.pulseShape = p.getPulseShape();
        this.pulseWidth = p.pulseWidth;
        this.laserEnergy = p.laserEnergy;
        addListeners();
    }

    private void addListeners() {
        instanceDescriptor.addListener(() -> {
            initShape();
            this.firePropertyChanged(instanceDescriptor, instanceDescriptor);
        });
        addListener((PropertyEvent event) -> {

            //when a property of the pulse is changed
            if (event.getProperty() instanceof NumericProperty) {

                var np = (NumericProperty) event.getProperty();
                var type = np.getType();

                //when this property is a pulse width
                if (type == NumericPropertyKeyword.PULSE_WIDTH) {
                    //find the specific SearchTask ancestor
                    var corrTask = (SearchTask) specificAncestor(SearchTask.class);
                    //new lower bound
                    NumericProperty pw = NumericProperties
                            .derive(NumericPropertyKeyword.LOWER_BOUND,
                                    (Number) np.getValue());
                    
                    var range = ( (ExperimentalData) corrTask.getInput() ).getRange();
                    
                    if( range.getLowerBound().compareTo(pw) < 0 ) {

                    //update lower bound of the range for that SearchTask
                    range.setLowerBound(pw);
                    
                    }
                    
                }

            }

        });
    }

    public Pulse copy() {
        return new Pulse(this);
    }

    public void initFrom(Pulse pulse) {
        this.pulseWidth = pulse.pulseWidth;
        this.laserEnergy = pulse.laserEnergy;
        this.pulseShape = pulse.pulseShape;
    }

    private void initShape() {
        setPulseShape(instanceDescriptor.newInstance(PulseTemporalShape.class));
        parameterListChanged();
    }

    public NumericProperty getPulseWidth() {
        return derive(PULSE_WIDTH, pulseWidth);
    }

    public void setPulseWidth(NumericProperty pulseWidth) {
        requireType(pulseWidth, PULSE_WIDTH);

        double newValue = (double) pulseWidth.getValue();
        
        double relChange = Math.abs((newValue - this.pulseWidth) / (this.pulseWidth + newValue));
        final double EPS = 1E-3;
        
        //do not update -- if new value is the same as the previous one
        if (relChange > EPS && newValue > 0) {
            
            //validate -- do not update if the new pulse width is greater than 2 half-times
            SearchTask task         = (SearchTask) this.specificAncestor(SearchTask.class);
            ExperimentalData data   = (ExperimentalData) task.getInput();
            
            if(newValue < 2.0 * data.getHalfTimeCalculator().getHalfTime()) {
                this.pulseWidth = (double) pulseWidth.getValue();
                firePropertyChanged(this, pulseWidth);
            }
            
        }
        
    }

    public NumericProperty getLaserEnergy() {
        return derive(LASER_ENERGY, laserEnergy);
    }

    public void setLaserEnergy(NumericProperty laserEnergy) {
        requireType(laserEnergy, LASER_ENERGY);
        this.laserEnergy = (double) laserEnergy.getValue();
        firePropertyChanged(this, laserEnergy);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Pulse:");
        sb.append(String.format("%n %-25s", getPulseShape()));
        sb.append(String.format("%n %-25s", getPulseWidth()));
        sb.append(String.format("%n %-25s", getLaserEnergy()));
        return sb.toString();
    }

    /**
     * The listed parameters for {@code Pulse} are:
     * <code>PulseShape, PULSE_WIDTH, SPOT_DIAMETER</code>.
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(PULSE_WIDTH);
        set.add(LASER_ENERGY);
        return set;
    }

    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(instanceDescriptor);
        return list;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == PULSE_WIDTH) {
            setPulseWidth(property);
        } else if (type == LASER_ENERGY) {
            setLaserEnergy(property);
        }
    }

    public InstanceDescriptor<? extends PulseTemporalShape> getPulseDescriptor() {
        return instanceDescriptor;
    }

    public void setPulseDescriptor(InstanceDescriptor<? extends PulseTemporalShape> shapeDescriptor) {
        this.instanceDescriptor = shapeDescriptor;
        //TODO
        initShape();
    }

    public PulseTemporalShape getPulseShape() {
        return pulseShape;
    }

    public void setPulseShape(PulseTemporalShape pulseShape) {
        this.pulseShape = pulseShape;
        pulseShape.setParent(this);    
        
    }

}
