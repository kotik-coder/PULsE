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

public class ResultTableExporter implements Exporter<ResultTable> {

	private static ResultTableExporter instance = new ResultTableExporter();

	private ResultTableExporter() {
		// intentionally blank
	}

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { Extension.HTML, Extension.CSV };
	}

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
					printIndividualHTML( (NumericProperty) table.getValueAt(i, j) , stream);
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
						printIndividualHTML( props.get(j), stream);
					stream.print("</tr>");
					
					stream.println();
				}));

				stream.print("</table>");
			}

		}
	}

	public static ResultTableExporter getInstance() {
		return instance;
	}

	@Override
	public Class<ResultTable> target() {
		return ResultTable.class;
	}
}
