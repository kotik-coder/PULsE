package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.tasks.TaskManager;
import pulse.tasks.processing.AbstractResult;
import pulse.tasks.processing.AverageResult;
import pulse.ui.Messages;
import pulse.ui.Version;
import pulse.ui.components.ResultTable;
import pulse.ui.components.models.ResultTableModel;

/**
 * A singleton {@code Exporter} which can process the results table. Invoked
 * when the user selects to export the calculation results. The output is a
 * summary file in either {@code csv} or {@code html} format.
 *
 */
public class ResultTableExporter implements Exporter<ResultTable> {

    private static final ResultTableExporter instance = new ResultTableExporter();

    private ResultTableExporter() {
        // intentionally blank
    }

    /**
     * Both {@code html} and {@code csv} are suported.
     */
    @Override
    public Extension[] getSupportedExtensions() {
        return new Extension[]{Extension.HTML, Extension.CSV};
    }

    /**
     * This will create a single file with the output. Depending on whether the
     * results table contain average results (with the respective error margins)
     * or only individual results, the file might consist of one or two tables,
     * first listing the average results and then finding what individual
     * results have been used to calculate the latter. In the {@code html}
     * format, the errors are given in the same table cells as the values and
     * are delimited by a plus-minus sign. The {@code html} table gives a pretty
     * representation of the results whereas the {@code csv}, while difficult to
     * read by a human, can be interpreted by most external tools such as LaTeX
     * pgfplots, gnuplot, or excel.
     */
    @Override
    public void printToStream(ResultTable table, FileOutputStream fos, Extension extension) {
        switch (extension) {
            case HTML:
                printHTML(table, fos);
                break;
            case CSV:
                printCSV(table, fos);
                break;
            default:
                System.err.println("Extension not supported: " + extension);
        }
    }

    private void printHeaderCSV(ResultTable table, PrintStream stream) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        stream.println("Summary report on "
                + LocalDateTime.now().format(formatter));
        stream.println("PULsE Version: " + Version.getCurrentVersion().toString());
        stream.println("Sample: " + TaskManager.getManagerInstance().getSampleName());
        stream.println("Ouput format sequence below: ");

        var fmt = ((ResultTableModel) table.getModel()).getFormat();

        for (int col = 0; col < table.getColumnCount(); col++) {
            var colName = fmt.fromAbbreviation(table.getColumnName(col));
            stream.println("Col. no.: " + col + " - " + colName);
        }

        stream.println("Note: average results are formatted as <value> ; <error> in the list below.");
        stream.println();
    }

    private void printIndividualCSV(NumericProperty p, PrintStream stream) {
        String fmt = p.getValue() instanceof Double ? "%-2.5e" : "%-6d";
        String s1 = String.format(fmt, p.getValue()).trim();
        String s2 = "";
        if (p.getError() != null) {
            s2 = String.format(fmt, p.getError()).trim();
            stream.print(s1 + " ; " + s2 + " ");
        } else {
            stream.print(s1 + " ");
        }
    }

    private void printCSV(ResultTable table, FileOutputStream fos) {
        try (PrintStream stream = new PrintStream(fos)) {

            printHeaderCSV(table, stream);

            for (int i = 0; i < table.getRowCount(); i++) {
                for (int j = 0; j < table.getColumnCount(); j++) {
                    printIndividualCSV((NumericProperty) table.getValueAt(i, j), stream);
                }
                stream.println();
            }

            List<AbstractResult> results = ((ResultTableModel) table.getModel()).getResults();

            if (results.stream().anyMatch(r -> r instanceof AverageResult)) {

                stream.println("");
                stream.print(Messages.getString("ResultTable.SeparatorCSV"));
                stream.println("");

                results.stream().filter(r -> r instanceof AverageResult)
                        .forEach(ar -> ((AverageResult) ar).getIndividualResults().stream().forEach(ir -> {
                    var props = AbstractResult.filterProperties(ir);

                    for (int j = 0; j < table.getColumnCount(); j++) {
                        printIndividualCSV(props.get(j), stream);
                    }

                    stream.println();
                }));

            }

        }

    }

    private void printHeaderHTML(ResultTable table, PrintStream stream, String caption) {
        stream.print(Messages.getString("ResultTableExporter.style"));
        stream.print("<caption>" + caption + "</caption>");
        stream.print("<thead><tr>");

        for (int col = 0; col < table.getColumnCount(); col++) {
            stream.print("<th>");
            stream.print(table.getColumnName(col) + "\t");
            stream.print("</th>");
        }

        stream.print("</tr></thead>");
    }

    private void printIndividualHTML(NumericProperty p, PrintStream stream) {
        stream.print("<td>");
        stream.print(p.formattedOutput());
        stream.print("</td>");
    }

    private void printHTML(ResultTable table, FileOutputStream fos) {
        try (PrintStream stream = new PrintStream(fos)) {
            printHeaderHTML(table, stream, "Exported table (contains either averaged or individual results)");

            stream.println("");

            for (int i = 0; i < table.getRowCount(); i++) {
                stream.print("<tr>");
                for (int j = 0; j < table.getColumnCount(); j++) {
                    printIndividualHTML((NumericProperty) table.getValueAt(i, j), stream);
                }
                stream.println("</tr>");
            }

            stream.print("</table>");

            List<AbstractResult> results = ((ResultTableModel) table.getModel()).getResults();

            if (results.stream().anyMatch(r -> r instanceof AverageResult)) {

                stream.print(Messages.getString("ResultTable.IndividualResults"));
                printHeaderHTML(table, stream, "Exported individual results for each processed task");

                stream.println("");

                results.stream().filter(r -> r instanceof AverageResult)
                        .forEach(ar -> ((AverageResult) ar).getIndividualResults().stream().forEach(ir -> {
                    var props = AbstractResult.filterProperties(ir);

                    stream.print("<tr>");
                    for (int j = 0; j < table.getColumnCount(); j++) {
                        printIndividualHTML(props.get(j), stream);
                    }
                    stream.print("</tr>");

                    stream.println();
                }));

                stream.print("</table>");
            }

        }
    }

    /**
     * Gets the single instance of this class.
     *
     * @return the single instance of {@code ResultTableExporter}.
     */
    public static ResultTableExporter getInstance() {
        return instance;
    }

    /**
     * @return {@code ResultTable.class}.
     */
    @Override
    public Class<ResultTable> target() {
        return ResultTable.class;
    }
}
