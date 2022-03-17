package pulse.input;

import static java.lang.System.lineSeparator;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.DETECTOR_GAIN;
import static pulse.properties.NumericPropertyKeyword.DETECTOR_IRIS;
import static pulse.properties.NumericPropertyKeyword.DIAMETER;
import static pulse.properties.NumericPropertyKeyword.LASER_ENERGY;
import static pulse.properties.NumericPropertyKeyword.PULSE_WIDTH;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;
import static pulse.tasks.Identifier.externalIdentifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import pulse.problem.laser.NumericPulseData;
import pulse.problem.laser.PulseTemporalShape;
import pulse.problem.laser.RectangularPulse;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.FOV_OUTER;
import pulse.properties.Property;
import pulse.properties.SampleName;
import pulse.tasks.Identifier;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * <p>
 * {@code Metadata} is the information relating to a specific experiment, which
 * can be used by a {@code SearchTask} to process an instance of
 * {@code ExperimentalData}. It is used to populate the associated
 * {@code Problem}, {@code DifferenceScheme}, and {@code Range} of the
 * {@code ExperimentalData}.
 * </p>
 *
 */
public class Metadata extends PropertyHolder implements Reflexive {

    private Set<NumericProperty> data;
    private SampleName sampleName;
    private int externalID;

    private InstanceDescriptor<? extends PulseTemporalShape> pulseDescriptor = new InstanceDescriptor<PulseTemporalShape>(
            "Pulse Shape Selector", PulseTemporalShape.class);

    private NumericPulseData pulseData;

    /**
     * Creates a {@code Metadata} with the specified parameters and a default
     * rectangular pulse shape. Properties are stored in a {@code TreeSet}.
     *
     * @param temperature the NumericProperty of the type
     * {@code NumericPropertyKeyword.TEST_TEMPERATURE}
     * @param externalId an integer, specifying the external ID recorded by the
     * experimental setup.
     */
    public Metadata(NumericProperty temperature, int externalId) {
        sampleName = new SampleName();
        setExternalID(externalId);
        pulseDescriptor.setSelectedDescriptor(RectangularPulse.class.getSimpleName());
        data = new TreeSet<NumericProperty>();
        set(TEST_TEMPERATURE, temperature);
    }

    /**
     * Gets the external ID usually specified in the experimental files. Note
     * this is not a {@code NumericProperty}
     *
     * @return an integer, representing the external ID
     */
    public int getExternalID() {
        return externalID;
    }

    /**
     * Sets the external ID in this {@code Metadata} to {@code externalId}
     *
     * @param externalId the value of the external ID
     */
    private void setExternalID(int externalId) {
        this.externalID = externalId;
    }

    /**
     * Retrieves the pulse shape recorded in this {@code Metadata}
     *
     * @return a {@code PulseShape} object
     */
    public InstanceDescriptor<? extends PulseTemporalShape> getPulseDescriptor() {
        return pulseDescriptor;
    }

    /**
     * Retrieves the sample name. This name is used to create directories when
     * exporting the data and also to fill the legend when plotting.
     *
     * @return the sample name
     */
    public SampleName getSampleName() {
        return sampleName;
    }

    /**
     * Sets the sample name property.
     *
     * @param sampleName the sample name
     */
    public void setSampleName(SampleName sampleName) {
        this.sampleName = sampleName;
    }

    public void setPulseData(NumericPulseData pulseData) {
        this.pulseData = pulseData;
    }

    /**
     * If a Numerical Pulse has been loaded (for example, when importing from
     * Proteus), this will return an object describing this data.
     */
    public NumericPulseData getPulseData() {
        return pulseData;
    }

    /**
     * Searches the internal list of this class for a property with the
     * {@code key} type.
     *
     * @return if present, returns a property belonging to this {@code Metadata}
     * with the specified type, otherwise return null.
     */
    @Override
    public NumericProperty numericProperty(NumericPropertyKeyword key) {
        var optional = data.stream().filter(p -> p.getType() == key).findFirst();
        return optional.isPresent() ? optional.get() : null;
    }

    /**
     * If {@code type} is listed by this {@code Metadata}, will attempt to
     * either set a value to the property belonging to this {@code Metadata} and
     * identified by {@code type} or add {@code property} to the internal
     * repository of this {@code Metadata}. Triggers {@code firePropertyChanged}
     * upon successful completion.
     *
     * @param type the type to be searched for
     * @param property a property with the type specified by its first argument.
     * The value of this property will be used to update its counterpart in this
     * {@code Metadata}. The signature of this method is dictated by the use of
     * Reflection API.
     * @throws IllegalArgumentException if the types of the arguments do not
     * match or if {@code} property is not a listed parameter
     * @see PropertyHolder.isListedParameter()
     * @see PropertyHolder.firePropertyChanged()
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {

        if (type != property.getType() || !isListedParameter(property)) {
            return; //ingore unrecognised properties
        }
        var optional = numericProperty(type);

        if (optional != null) {
            optional.setValue((Number) property.getValue());
        } else {
            data.add(property);
        }

        firePropertyChanged(this, property);

    }

    /**
     * The listed types include {@code TEST_TEMPERATURE}, {@code THICKNESS},
     * {@code DIAMETER}, {@code PULSE_WIDTH}, {@code SPOT_DIAMETER},
     * {@code LASER_ENERGY}, {@code DETECTOR_GAIN}, {@code DETECTOR_IRIS},
     * sample name and the types listed by the pulse descriptor.
     */
    @Override
    public List<Property> listedTypes() {
        List<Property> list = super.listedTypes();
        list.add(new SampleName());
        list.add(pulseDescriptor);
        return list;
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(TEST_TEMPERATURE);
        set.add(THICKNESS);
        set.add(DIAMETER);
        set.add(PULSE_WIDTH);
        set.add(SPOT_DIAMETER);
        set.add(LASER_ENERGY);
        set.add(DETECTOR_GAIN);
        set.add(DETECTOR_IRIS);
        set.add(FOV_OUTER);
        return set;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(sampleName + " [" + externalID + "]");
        sb.append(lineSeparator());
        sb.append(lineSeparator());

        data.forEach(entry -> {
            sb.append(entry.toString());
            sb.append(lineSeparator());
        });

        sb.append(pulseDescriptor.toString());

        return sb.toString();

    }

    /**
     * Creates a list of data that contain all {@code NumericProperty} objects
     * belonging to this {@code Metadata} and an {@code InstanceDescriptor}
     * relating to the pulse shape.
     */
    @Override
    public List<Property> data() {
        var list = new ArrayList<Property>();
        list.addAll(data);
        list.add(pulseDescriptor);
        return list;
    }

    /**
     * @return If this {@code Metadata} is NOT assigned to a {@code SearchTask},
     * returns a new {@code Identifier} based on the {@code externalID}.
     * Otherwise, calls {@code super.identify()}.
     * @see Identifier.externalIdentifier()
     */
    @Override
    public Identifier identify() {
        return getParent() == null ? externalIdentifier(externalID) : super.identify();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Metadata)) {
            return false;
        }

        var other = (Metadata) o;

        if (other.getExternalID() != this.getExternalID()) {
            return false;
        }

        if (!sampleName.equals(other.getSampleName())) {
            return false;
        }

        return this.data().containsAll(other.data());

    }

}
