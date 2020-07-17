package pulse.io.export;

import static pulse.io.export.Extension.CSV;
import static pulse.io.export.Extension.HTML;
import static pulse.ui.Messages.getString;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.HeatingCurve;
import pulse.ui.Messages;

/**
 * A singleton exporter allows writing the data contained in a heating curve in a two-column format 
 * to create files conforming to either csv or html extension. The first column always represents the time
 * sequence, which may be shifted if the associated property of the heating curve is 
 * non-zero. The second column represents the baseline-adjusted signal.
 *
 */

public class HeatingCurveExporter implements Exporter<HeatingCurve> {

	private static HeatingCurveExporter instance = new HeatingCurveExporter();

	private HeatingCurveExporter() {
		// Intentionally blank
	}

	@Override
	public void printToStream(HeatingCurve hc, FileOutputStream fos, Extension extension) {
		if (hc.adjustedSize() < 1)
			return;

		switch (extension) {
		case HTML:
			printHTML(hc, fos);
			break;
		case CSV:
			printCSV(hc, fos);
			break;
		default:
			throw new IllegalArgumentException("Format not recognised: " + extension);
		}
	}
	
	/**
	 * Currently {@code html} and {@code csv} extensions are supported.
	 */

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML, CSV};
	}

	private void printHTML(HeatingCurve hc, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
            	stream.print(Messages.getString("ResultTableExporter.style"));
        		stream.print("<caption>Time-temperature profile</caption>");
        		stream.print("<thead><tr>");
                
                final String TIME_LABEL = getString("HeatingCurve.6");
                final String TEMPERATURE_LABEL = getString("HeatingCurve.7");
                
                stream.print("<th>" + TIME_LABEL + "\t</th>");
                stream.print("<th>" + TEMPERATURE_LABEL + "\t</th>");
                
                stream.print("</tr></thead>");
                                
                double t;
                double T;
                
                int size = hc.actualDataPoints();
                int nominalSize = (int) hc.getNumPoints().getValue();
                int finalSize = size < nominalSize ? size : nominalSize;
                
                for (int i = 0; i < finalSize; i++) {
                    stream.print("<tr>");
                    
                    stream.print("<td>");
                    t = hc.timeAt(i);
                    stream.printf("%.6f %n", t);
                    stream.print("\t</td><td>");
                    T = hc.signalAt(i);
                    stream.printf("%.6f %n</td>", T);
                    
                    stream.println("</tr>");
                }
                
                stream.print("</table>");
            }

	}

	private void printCSV(HeatingCurve hc, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                final String TIME_LABEL = getString("HeatingCurve.6");
                final String TEMPERATURE_LABEL = hc.getPrefix();
                stream.print(TIME_LABEL + "\t" + TEMPERATURE_LABEL + "\t");
                
                double t;
                double T;
                
                int size = hc.actualDataPoints();
                int nominalSize = (int) hc.getNumPoints().getValue();
                int finalSize = size < nominalSize ? size : nominalSize;
                
                for (int i = 0; i < finalSize; i++) {
                    t = hc.timeAt(i);
                    stream.printf("%n%3.4f", t);
                    T = hc.signalAt(i);
                    stream.printf("\t%3.4f", T);
                }
            }

	}

	/**
	 * Returns the single instance of this subclass.
	 * @return an instance of {@code HeatingCurveExporter}.
	 */
	
	public static HeatingCurveExporter getInstance() {
		return instance;
	}
	
	/**
	 * @return the {@code HeatingCurve} class.
	 */

	@Override
	public Class<HeatingCurve> target() {
		return HeatingCurve.class;
	}

}