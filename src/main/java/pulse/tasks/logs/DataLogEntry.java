package pulse.tasks.logs;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import pulse.math.Parameter;
import pulse.math.ParameterIdentifier;
import pulse.properties.NumericProperties;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.OBJECTIVE_FUNCTION;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

/**
 * <p>
 * A {@code LogEntry} with a list of {@code NumericPropert}ies. Can be created
 * from a {@code SearchTask}. The output is accessible via the
 * {@code toString()} method.
 * </p>
 *
 */
public class DataLogEntry extends LogEntry {

    private List<Parameter> entry;

    /**
     * Creates a new {@code DataLogEntry} based on the current values of the
     * properties from {@code task} which match the currently selected
     * {@code LogFormat}.
     *
     * @param task a task, which will be used to build the {@code DataLogEntry}
     */
    public DataLogEntry(SearchTask task) {
        super(task);
        try {
            fill();
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            System.err.println("Failed to fill this log entry with data. Details below.");
            e.printStackTrace();
        }
    }

    /**
     * Fills this {@code DataLogEtnry} with properties from the
     * {@code SearchTask}, which have types matching to those listed in the
     * {@code LogFormat}.
     *
     * @throws IllegalAccessException if the call to
     * {@code task.numericProperties() fails}
     * @throws IllegalArgumentException if the call to
     * {@code task.numericProperties() fails}
     * @throws InvocationTargetException if the call to
     * {@code task.numericProperties() fails}
     */
    private void fill() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        var task = TaskManager.getManagerInstance().getTask(getIdentifier());
        entry = task.searchVector().getParameters();
        //iteration
        var pval = task.getIterativeState().getIteration();
        var pid = new Parameter(new ParameterIdentifier(pval.getType()));
        pid.setValue((int) pval.getValue());
        //cost
        var costId = new Parameter(new ParameterIdentifier(OBJECTIVE_FUNCTION));
        var costval = task.getIterativeState().getCost();
        //
        entry.add(0, pid);
        if (NumericProperties.isValueSensible(def(OBJECTIVE_FUNCTION), costval)) {
            costId.setValue(costval);
            entry.add(costId);
        }
    }

    public List<Parameter> getData() {
        return entry;
    }

    /**
     * This {@code String} will be displayed by the {@code LogPane} if the
     * verbose log option is enabled.
     *
     * @see pulse.ui.components.LogPane
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        /*
		 * // UNCOMMENT THIS TO PRODUCE EASY-TO-READ DATA ENTRIES sb.append("\n");
		 * 
		 * for(NumericProperty p : entry) { sb.append((p.getValue() instanceof Double ?
		 * String.format("%2.3e",p.getValue()) : p.getValue())); sb.append("\t");
		 * sb.append("\n"); }
		 * 
		 * return sb.toString();
         */
        sb.append("<table>");

        for (Parameter p : entry) {
            sb.append("<tr><td>");
            var def = NumericProperties.def(p.getIdentifier().getKeyword());
            boolean b = def.getValue() instanceof Integer;
            Number val;
            if (b) {
                val = (int) Math.rint(p.getApparentValue());
            } else {
                val = p.getApparentValue();
            }
            def.setValue(val);
            sb.append(def.getAbbreviation(false));
            int index = p.getIdentifier().getIndex();
            if (index > 0) {
                sb.append(" - ").append(index);
            }
            sb.append("</td><<td>");
            sb.append(Messages.getString("DataLogEntry.FontTagNumber")); //$NON-NLS-1$
            sb.append("<b>");
            sb.append(def.formattedOutput());
            sb.append("</b>");
            sb.append(Messages.getString("DataLogEntry.FontTagClose")); //$NON-NLS-1$
            sb.append("</td><br></tr>");
        }

        sb.append("</table><br><hr>");

        return sb.toString();

    }

}