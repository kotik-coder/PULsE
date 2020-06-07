package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.search.statistics.ResidualStatistic;
import pulse.ui.Messages;

public class ResidualStatisticExporter implements Exporter<ResidualStatistic> {

	private static ResidualStatisticExporter instance = new ResidualStatisticExporter();

	private ResidualStatisticExporter() {
		// intentionally left blank
	}

	@Override
	public Class<ResidualStatistic> target() {
		return ResidualStatistic.class;
	}

	@Override
	public void printToStream(ResidualStatistic rs, FileOutputStream fos, Extension extension) {
		switch (extension) {
		case HTML:
			printHTML(rs, fos);
			break;
		case CSV:
			printCSV(rs, fos);
			break;
		default:
			throw new IllegalArgumentException("Format not recognised: " + extension);
		}
	}

	/**
	 * The supported extensions for exporting the data contained in this object.
	 * Currently include {@code .html} and {@code .csv}.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { Extension.HTML, Extension.CSV };
	}

	private void printHTML(ResidualStatistic hc, FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);

		var residuals = hc.getResiduals();
		int residualsLength = residuals == null ? 0 : residuals.size();

		stream.print("<table>");
		stream.print("<tr>");

		final String TIME_LABEL = Messages.getString("HeatingCurve.6");
		final String RESIDUAL_LABEL = "Residual";

		stream.print("<td>" + TIME_LABEL + "\t</td>");
		stream.print("<td>" + RESIDUAL_LABEL + "\t</td>");

		stream.print("</tr>");

		stream.println("");

		double tr, Tr;

		for (int i = 0; i < residualsLength; i++) {
			stream.print("<tr>");

			stream.print("<td>");
			tr = residuals.get(i)[0];
			stream.printf("%.6f %n", tr);
			stream.print("\t</td><td>");
			Tr = residuals.get(i)[1];
			stream.printf("%.6f %n</td>", Tr);

			stream.println("</tr>");
		}

		stream.print("</table>");
		stream.close();

	}

	private void printCSV(ResidualStatistic hc, FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);

		var residuals = hc.getResiduals();
		int residualsLength = residuals == null ? 0 : residuals.size();

		final String TIME_LABEL = Messages.getString("HeatingCurve.6");
		final String RESIDUAL_LABEL = "Residual";

		stream.print(TIME_LABEL + "\t" + RESIDUAL_LABEL + "\t");
		double tr, Tr;

		for (int i = 0; i < residualsLength; i++) {
			tr = residuals.get(i)[0];
			stream.printf("%n%3.4f", tr);
			Tr = residuals.get(i)[1];
			stream.printf("\t%3.4f", Tr);
		}

		stream.close();

	}

	public static ResidualStatisticExporter getInstance() {
		return instance;
	}

}
