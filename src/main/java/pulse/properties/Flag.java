package pulse.properties;

import static pulse.properties.NumericProperties.def;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A {@code Flag} is a {@code Property} that has a {@code type} represented by a
 * {@code NumericPropertyKeyword}, a {@code boolean value}, and a {@code String}
 * with a short abbreviation describing the type of this flag (this is usually
 * defined by the corresponding {@code NumericProperty}).
 */
public class Flag implements Property {

    private NumericPropertyKeyword index;
    private boolean value;
    private String descriptor;

    /**
     * Creates a {@code Flag} with the type {@code type}. The default
     * {@code value} is set to {@code false}.
     *
     * @param type the {@code NumericPropertyKeyword} associated with this
     * {@code Flag}
     */
    public Flag(NumericPropertyKeyword type) {
        this.index = type;
        value = false;
    }

    /**
     * Creates a {@code Flag} with the following pre-specified parameters: type
     * {@code type}, short description {@code abbreviations}, and {@code value}.
     *
     * @param property the {@code NumericProperty} parameter containing the
     * {@code NumericPropertyKeyword} identifier
     * @param value the {@code boolean} value of this {@code flag}
     */
    public Flag(NumericProperty property, boolean value) {
        this.index = property.getType();
        this.descriptor = property.getDescriptor(true);
        this.value = value;
    }

    /**
     * A static method for converting enabled flags to a {@code List} of
     * {@code NumericPropertyKeyword}s. Each keyword in this list corresponds to
     * an enabled flag in the {@code flags} {@code List}.
     *
     * @param flags the list of flags that needs to be analysed
     * @return a list of {@code NumericPropertyKeyword}s corresponding to
     * enabled {@code flag}s.
     */
    public static List<NumericPropertyKeyword> convert(List<Flag> flags) {
        var filtered = flags.stream().filter(flag -> (boolean) flag.getValue());
        return filtered.map(flag -> flag.getType()).collect(Collectors.toList());
    }

    /**
     * List of all possible {@code Flag}s that can be used in finding the
     * reverse solution of the heat conduction problems. Includes all flags that
     * correspond to {@code NumericPropert}ies satisfying
     * {@code p.isOptimisable() = true}. The default value of the flag is set to
     * {@code p.isDefaultSearchVariable()} -- based on the information contained
     * in the {@code NumericProperty.xml} file.
     *
     * @return a {@code List} of all possible {@code Flag}s
     * @see
     */
    public static List<Flag> allFlags() {
        return NumericProperties.defaultList().stream()
                .filter(p -> p.isOptimisable())
                .map(p -> new Flag(p, p.isDefaultSearchVariable()))
                .collect(Collectors.toList());
    }

    /**
     * Returns the type of this {@code Flag}.
     *
     * @return a {@code NumericPropertyKeyword} representing the type of this
     * {@code Flag}.
     */
    public NumericPropertyKeyword getType() {
        return index;
    }

    /**
     * Creates a new {@code Flag} object based on this {@code Flag}, but with a
     * different {@code value}.
     *
     * @param value either {@code true} or {@code false}
     * @return a {@code Flag} that replicates the {@code type} and
     * {@code abbreviation} of this {@code Flag}, but sets a new {@code value}
     */
    public Flag derive(boolean value) {
        return new Flag(def(index), value);
    }

    /**
     * Creates a short description for the GUI.
     */
    @Override
    public String getDescriptor(boolean addHtmlTags) {
        return addHtmlTags ? "<html><b>Search for </b>" + descriptor + "</html>" : "<b>Search for </b>" + descriptor;
    }

    /**
     * The value for this {@code Property} is a {@code boolean}.
     */
    @Override
    public Object getValue() {
        return value;
    }

    public void setValue(boolean value) {
        this.value = (boolean) value;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + index.name();
    }

    public String abbreviation(boolean addHtmlTags) {
        return addHtmlTags ? "<html>" + descriptor + "</html>" : descriptor;
    }

    public void setAbbreviation(String abbreviation) {
        this.descriptor = abbreviation;
    }

    @Override
    public Object identifier() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Flag)) {
            return false;
        }

        Flag f = (Flag) o;

        return (f.getType() == this.getType())
                && (f.getValue().equals(this.getValue()));

    }

    @Override
    public boolean attemptUpdate(Object value) {
        // TODO Auto-generated method stub
        return false;
    }

    public static List<Flag> selectActive(List<Flag> flags) {
        return flags.stream().filter(flag -> (boolean) flag.getValue()).collect(Collectors.toList());
    }

}
