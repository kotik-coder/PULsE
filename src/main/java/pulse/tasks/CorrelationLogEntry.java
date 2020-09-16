package pulse.tasks;

import static pulse.properties.NumericProperties.def;

import pulse.properties.NumericPropertyKeyword;
import pulse.util.ImmutablePair;

public class CorrelationLogEntry extends LogEntry {

	public CorrelationLogEntry(SearchTask t) {
		super(t);
	}

	@Override
	public String toString() {
		SearchTask t = TaskManager.getTask(getIdentifier());
		var buffer = t.getCorrelationBuffer();
		var test = t.getCorrelationTest();
		var map = buffer.evaluate(test);

		if (map == null)
			return "";

		if (map.isEmpty())
			return "";

		StringBuilder sb = new StringBuilder();
		sb.append("<p>");
		sb.append("<table border='1'; width=90%><caption>Correlation table</caption>");
		sb.append("<tr> <th><i>x</i></th> <th><i>y</i></th> <th>Correlation</th> </tr>");

		for (ImmutablePair<NumericPropertyKeyword> key : map.keySet()) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append(def(key.getFirst()).getAbbreviation(false));
			sb.append("</td>");
			//
			sb.append("<td>");
			sb.append(def(key.getSecond()).getAbbreviation(false));
			sb.append("</td>");
			//
			sb.append("<td>");
			if (test.compareToThreshold(map.get(key)))
				sb.append("<font color='red'>");
			sb.append("<b>" + String.format("%3.2f", map.get(key)) + "</b>");
			if (test.compareToThreshold(map.get(key)))
				sb.append("</font>");
			sb.append("</td>");
			sb.append("</tr>");
		}

		sb.append("</table>");
		sb.append("</p>");

		return sb.toString();

	}

}
