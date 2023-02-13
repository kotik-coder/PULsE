package pulse.tasks.processing;

import java.io.Serializable;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;

/**
 * <p>
 * A singleton {@code ResultFormat}, which contains a list of
 * {@code NumericPropertyKeyword}s used for identification of
 * {@code NumericPropert}ies. The format is constructed using a string of unique
 * characters.
 * </p>
 */
public class ResultFormat implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -3155104011585735097L;

    private List<NumericPropertyKeyword> nameMap;

    private final static NumericPropertyKeyword[] minimalArray = new NumericPropertyKeyword[]{IDENTIFIER,
        TEST_TEMPERATURE, DIFFUSIVITY};

    /**
     * <p>
     * The default format specified by the
     * {@code Messages.getString("ResultFormat.DefaultFormat")}. See file
     * messages.properties in {@code pulse.ui}.
     * </p>
     */
    private static ResultFormat format = new ResultFormat();
    private static List<ResultFormatListener> listeners = new ArrayList<ResultFormatListener>();

    private ResultFormat() {
        this(asList(minimalArray));
    }

    private ResultFormat(List<NumericPropertyKeyword> keys) {
        nameMap = new ArrayList<>();
        keys.forEach(key
                -> nameMap.add(key)
        );
        TaskManager.addSessionListener(() -> format = this);
    }

    public static void addResultFormatListener(ResultFormatListener rfl) {
        listeners.add(rfl);
    }

    public static void removeListeners() {
        if (listeners != null) {
            listeners.clear();
        }
    }

    public static ResultFormat generateFormat(List<NumericPropertyKeyword> keys) {
        format = new ResultFormat(keys);

        var rfe = new ResultFormatEvent(format);
        for (var rfl : listeners) {
            rfl.resultFormatChanged(rfe);
        }

        return format;
    }

    /**
     * This class uses a singleton pattern, meaning there is only instance of
     * this class.
     *
     * @return the single (static) instance of this class
     */
    public static ResultFormat getInstance() {
        return format;
    }

    /**
     * Retrieves the list of keyword associated with this {@code ResultFormat}
     *
     * @return a list of keywords that can be used to access
     * {@code NumericProperty} objects
     */
    public List<NumericPropertyKeyword> getKeywords() {
        return nameMap;
    }

    /**
     * Creates a {@code List<String>} of default abbreviations corresponding to
     * the list of keywords specific to {@code NumericProperty} objects.
     *
     * @return a list of abbreviations (typically, for filling the result table
     * headers)
     */
    public List<String> abbreviations() {
        return nameMap.stream().map(keyword -> def(keyword).getAbbreviation(true)).collect(toList());
    }

    /**
     * Creates a {@code List<String>} of default descriptions corresponding to
     * the list of keywords specific to {@code NumericProperty} objects.
     *
     * @return a list of abbreviations (typically, for filling the result table
     * tooltips)
     */
    public List<String> descriptors() {
        return nameMap.stream().map(keyword -> def(keyword).getDescriptor(true)).collect(toList());
    }

    /**
     * Finds a {@code NumericPropertyKeyword} contained in the {@code nameMap},
     * the description of which matches {@code descriptor}.
     *
     * @param descriptor a {@code String} describing the
     * {@code NumericPropertyKeyword}
     * @return the {@code NumericPropertyKeyword} object
     */
    public NumericPropertyKeyword fromAbbreviation(String descriptor) {
        return nameMap.stream().filter(keyword -> def(keyword).getAbbreviation(true).equals(descriptor))
                .findFirst().get();
    }

    /**
     * Calculates the length of the format string, which is the same as the size
     * of the keyword list.
     *
     * @return an integer, representing the size of the format string.
     */
    public int size() {
        return nameMap.size();
    }

    public int indexOf(NumericPropertyKeyword key) {
        if (nameMap.contains(key)) {
            return nameMap.indexOf(key);
        }
        return -1;
    }

    public static NumericPropertyKeyword[] getMinimalArray() {
        return minimalArray;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o.getClass().equals(o.getClass()))) {
            return false;
        }

        var another = (ResultFormat) o;

        return (another.nameMap.containsAll(this.nameMap)) && (this.nameMap.containsAll(another.nameMap));

    }

}