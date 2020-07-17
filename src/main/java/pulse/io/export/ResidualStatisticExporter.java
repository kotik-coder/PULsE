package pulse.io.export;

import static pulse.io.export.Extension.CSV;
import static pulse.io.export.Extension.HTML;
import static pulse.ui.Messages.getString;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.search.statistics.ResidualStatistic;
import pulse.ui.Messages;

/**
 * Exports the residuals, where each residual value refers to a specific point in time. 
 * Implements both the csv and html formats.
 *
 */

public class ResidualStatisticExporter implements Exporter<ResidualStatistic> {

	private static ResidualStatisticExporter instance = new ResidualStatisticExporter();

	private ResidualStatisticExporter() {
		// intentionally left blank
	}

	/**
	 * @return {@code ResidualStatistic.class}
	 */
	
	@Override
	public Class<ResidualStatistic> target() {
		return ResidualStatistic.class;
	}

	/**
	 * Prints the residuals in a two-column format in a {@code html} or {@code csv} file (accepts both extensions).
	 */
	
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
		return new Extension[] { HTML, CSV};
	}

	private void printHTML(ResidualStatistic hc, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                var residuals = hc.getResiduals();
                int residualsLength = residuals == null ? 0 : residuals.size();
            	stream.print(Messages.getString("ResultTableExporter.style"));
        		stream.print("<caption>Time profile of residuals</caption>");
        		stream.print("<thead><tr>");
                final String TIME_LABEL = getString("HeatingCurve.6");
                final String RESIDUAL_LABEL = "Residual";
                stream.print("<th>" + TIME_LABEL + "\t</th>");
                stream.print("<th>" + RESIDUAL_LABEL + "\t</th>");
                stream.print("</tr></thead>");
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
            }

	}

	private void printCSV(ResidualStatistic hc, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                var residuals = hc.getResiduals();
                int residualsLength = residuals == null ? 0 : residuals.size();
                final String TIME_LABEL = getString("HeatingCurve.6");
                final String RESIDUAL_LABEL = "Residual";
                stream.print(TIME_LABEL + "\t" + RESIDUAL_LABEL + "\t");
                double tr, Tr;
                for (int i = 0; i < residualsLength; i++) {
                    tr = residuals.get(i)[0];
                    stream.printf("%n%3.4f", tr);
                    Tr = residuals.get(i)[1];
                    stream.printf("\t%3.4f", Tr);
                }
            }

	}

	/**
	 * Retrieves the single instance of this class.
	 * @return a single instance of {@code ResidualStatisticExporter}.
	 */
	
	public static ResidualStatisticExporter getInstance() {
		return instance;
	}

}
