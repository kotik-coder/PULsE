package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import pulse.properties.NumericProperty;
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

	private List<NumericProperty> entry;

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
	 * Fills this {@code DataLogEtnry} with properties from the {@code SearchTask},
	 * which have types matching to those listed in the {@code LogFormat}.
	 * 
	 * @throws IllegalAccessException    if the call to
	 *                                   {@code task.numericProperties() fails}
	 * @throws IllegalArgumentException  if the call to
	 *                                   {@code task.numericProperties() fails}
	 * @throws InvocationTargetException if the call to
	 *                                   {@code task.numericProperties() fails}
	 */

	private void fill() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		SearchTask task = TaskManager.getTask(getIdentifier());

		entry = task.alteredParameters();
		Collections.sort(entry, (p1, p2) -> p1.getDescriptor(false).compareTo(p2.getDescriptor(false)));
		entry.add(0, task.getPath().getIteration());
	}

	public List<NumericProperty> getData() {
		return entry;
	}

	/**
	 * This {@code String} will be displayed by the {@code LogPane} if the verbose
	 * log option is enabled.
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

		for (NumericProperty p : entry) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append(p.getAbbreviation(false));
			sb.append("</td>");
			sb.append("<td>");
			sb.append(Messages.getString("DataLogEntry.FontTagNumber")); //$NON-NLS-1$
			sb.append("<b>");
			sb.append(p.formattedOutput());
			sb.append("</b>");
			sb.append(Messages.getString("DataLogEntry.FontTagClose")); //$NON-NLS-1$
			sb.append("</td>");
			sb.append("<br>");
			sb.append("</tr>");
		}

		sb.append("</table>");

		sb.append("<br>");
		sb.append("<hr>");

		return sb.toString();

	}

}