package pulse.search.direction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pulse.problem.statements.Problem;
import pulse.properties.Flag;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;

public class ActiveFlags {

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

        var p = t.getCurrentCalculation().getProblem();

        if (p != null) {

            var fullList = p.listedKeywords();
            fullList.addAll(t.getExperimentalCurve().listedKeywords());
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

    /**
     * Finds what properties are being altered in the search
     *
     * @param t task for which the active parameters should be listed
     * @return a {@code List} of property types represented by
     * {@code NumericPropertyKeyword}s
     */
    public static List<NumericPropertyKeyword> activeParameters(SearchTask t) {
        var c = t.getCurrentCalculation();
        //problem dependent
        var allActiveParams = selectActiveAndListed(flags, c.getProblem()); 
        //problem independent (lower/upper bound)
        var listed = selectActiveAndListed(flags, t.getExperimentalCurve().getRange() );
        allActiveParams.addAll(listed);          
        return allActiveParams;
    }

    public static List<NumericPropertyKeyword> selectActiveAndListed(List<Flag> flags, PropertyHolder listed) {
        //return empty list
        if(listed == null) {
            return new ArrayList<NumericPropertyKeyword>();
        }
            
        return selectActiveTypes(flags).stream()
                .filter(type -> listed.isListedNumericType(type))
                .collect(Collectors.toList());
    }
   
    public static List<NumericPropertyKeyword> selectActiveTypes(List<Flag> flags) {
        return Flag.selectActive(flags).stream().map(flag -> flag.getType()).collect(Collectors.toList());
    }

}
