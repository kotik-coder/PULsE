package pulse.search.direction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import pulse.input.ExperimentalData;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.Calculation;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;

public class ActiveFlags implements Serializable {

    private static final long serialVersionUID = -8711073682010113698L;
    private static List<Flag> flags;

    static {
        reset();
    }

    private ActiveFlags() {
        //empty constructor
    }

    public static void reset() {
        flags = Flag.allFlags();
    }

    public static List<Flag> getAllFlags() {
        return flags;
    }

    public static Set<Flag> availableProperties() {
        var set = new HashSet<Flag>();

        var t = TaskManager.getManagerInstance().getSelectedTask();

        if (t == null) {
            return set;
        }

        var p = ((Calculation) t.getResponse()).getProblem();

        if (p != null) {

            var fullList = p.listedKeywords();
            fullList.addAll(((ExperimentalData) t.getInput()).listedKeywords());
            NumericPropertyKeyword key;

            for (Flag property : flags) {
                key = property.getType();
                if (fullList.contains(key)) {
                    set.add(property);
                }

            }

        }

        return set;
    }

    public static Flag get(NumericPropertyKeyword key) {
        var flag = flags.stream().filter(f -> f.getType() == key).findAny();
        return flag.isPresent() ? flag.get() : null;
    }

    /**
     * Creates a deep copy of the flags collection.
     *
     * @return a deep copy of the flags
     */
    public static List<Flag> storeState() {
        var copy = new ArrayList<Flag>();
        for (Flag f : flags) {
            copy.add(new Flag(f));
        }
        return copy;
    }

    /**
     * Loads the argument into the current list of flags. This will update any
     * matching flags and assign values correpon
     *
     * @param flags
     */
    public static void loadState(List<Flag> flags) {
        for (Flag f : ActiveFlags.flags) {
            Optional<Flag> existingFlag = flags.stream().filter(fl
                    -> fl.getType() == f.getType()).findFirst();
            if (existingFlag.isPresent()) {
                f.setValue((boolean) existingFlag.get().getValue());
            }
        }
    }

    public static List<NumericPropertyKeyword> selectActiveAndListed(List<Flag> flags, PropertyHolder listed) {
        //return empty list
        if (listed == null) {
            return new ArrayList<>();
        }

        return selectActiveTypes(flags).stream()
                .filter(type -> listed.isListedNumericType(type))
                .collect(Collectors.toList());
    }

    public static List<NumericPropertyKeyword> selectActiveTypes(List<Flag> flags) {
        return Flag.selectActive(flags).stream().map(flag -> flag.getType()).collect(Collectors.toList());
    }

}
