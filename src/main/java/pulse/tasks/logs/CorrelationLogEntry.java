package pulse.tasks.logs;

import pulse.math.ParameterIdentifier;
import static pulse.properties.NumericProperties.def;

import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.util.ImmutablePair;

public class CorrelationLogEntry extends LogEntry {

    public CorrelationLogEntry(SearchTask t) {
        super(t);
    }

    @Override
    public String toString() {
        var t = TaskManager.getManagerInstance().getTask(getIdentifier());
        var buffer = t.getCorrelationBuffer();
        var test = t.getCorrelationTest();
        var map = buffer.evaluate(test);

        if (map == null) {
            return "";
        }

        if (map.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<p><table border='1'; width=90%><caption>Correlation table</caption>");
        sb.append("<tr> <th><i>x</i></th> <th><i>y</i></th> <th>Correlation</th> </tr>");

        for (ImmutablePair<ParameterIdentifier> key : map.keySet()) {
            sb.append("<tr><td>");
            sb.append(def(key.getFirst().getKeyword()).getAbbreviation(false));
            if (key.getFirst().getIndex() > 0) {
                sb.append(" - ").append(key.getFirst().getIndex());
            }
            sb.append("</td><td>");
            sb.append(def(key.getSecond().getKeyword()).getAbbreviation(false));
            if (key.getSecond().getIndex() > 0) {
                sb.append(" - ").append(key.getSecond().getIndex());
            }
            sb.append("</td><td>");
            if (test.compareToThreshold(map.get(key))) {
                sb.append("<font color='red'>");
            }
            sb.append("<b>").append(String.format("%3.2f", map.get(key))).append("</b>");
            if (test.compareToThreshold(map.get(key))) {
                sb.append("</font>");
            }
            sb.append("</td></tr>");
        }

        sb.append("</table></p>");

        return sb.toString();

    }

}
