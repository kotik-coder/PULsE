package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.AbstractResult;
import pulse.tasks.AverageResult;
import pulse.ui.Messages;
import pulse.ui.components.ResultTable;
import pulse.ui.components.models.ResultTableModel;

/**
 * A singleton {@code Exporter} which can process the results table. Invoked when the user selects to export 
 * the calculation results. The output is a summary file in either {@code csv} or {@code html} format.
 *
 */

public class ResultTableExporter implements Exporter<ResultTable> {

	private static ResultTableExporter instance = new ResultTableExporter();

	private ResultTableExporter() {
		// intentionally blank
	}

	/**
	 * Both {@code html} and {@code csv} are suported.
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { Extension.HTML, Extension.CSV };
	}
	
	/**
	 * This will create a single file with the output. Depending on whether the 
	 * results table contain average results (with the respective error margins) or only individual results,
	 * the file might consist of one or two tables, first listing the average results and then finding
	 * what individual results have been used to calculate the latter. In the {@code html} format, the errors
	 * are given in the same table cells as the values and are delimited by a plus-minus sign. The {@code html}
	 * table gives a pretty representation of the results whereas the {@code csv}, while difficult to read by a human,
	 * can be interpreted by most external tools such as LaTeX pgfplots, gnuplot, or excel.  
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
		NumericPropertyKeyword p = null;
		for (int col = 0; col < table.getColumnCount(); col++) {
			p = ((ResultTableModel) table.getModel()).getFormat().fromAbbreviation(table.getColumnName(col));
			stream.print(p + "\t");
			stream.print("STDEV" + "\t");
		}
		stream.println("");
	}

	private void printIndividualCSV(NumericProperty p, PrintStream stream) {
		stream.print(p.valueOutput() + "\t" + p.errorOutput() + "\t");
	}

	private void printCSV(ResultTable table, FileOutputStream fos) {
		try (PrintStream stream = new PrintStream(fos)) {

			printHeaderCSV(table, stream);

			for (int i = 0; i < table.getRowCount(); i++) {
				for (int j = 0; j < table.getColumnCount(); j++)
					printIndividualCSV((NumericProperty) table.getValueAt(i, j), stream);
				stream.println();
			}

			List<AbstractResult> results = ((ResultTableModel) table.getModel()).getResults();

			if (results.stream().anyMatch(r -> r instanceof AverageResult)) {

				stream.println("");
				stream.print(Messages.getString("ResultTable.SeparatorCSV"));
				stream.println("");

				printHeaderCSV(table, stream);

				results.stream().filter(r -> r instanceof AverageResult)
						.forEach(ar -> ((AverageResult) ar).getIndividualResults().stream().forEach(ir -> {
							var props = AbstractResult.filterProperties(ir);

							for (int j = 0; j < table.getColumnCount(); j++)
								printIndividualCSV(props.get(j), stream);

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
				for (int j = 0; j < table.getColumnCount(); j++)
					printIndividualHTML((NumericProperty) table.getValueAt(i, j), stream);
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
							for (int j = 0; j < table.getColumnCount(); j++)
								printIndividualHTML(props.get(j), stream);
							stream.print("</tr>");

							stream.println();
						}));

				stream.print("</table>");
			}

		}
	}
	
	/**
	 * Gets the single instance of this class.
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
