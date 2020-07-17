package pulse.io.export;

import java.io.FileOutputStream;

import pulse.input.ExperimentalData;

/**
 * A wrapper singleton class that is made specifically to handle export requests of {@code ExperimentalData}.
 * Does exactly the same as the {@code HeatingCurveExporter}, except that its target is specifically set
 * to {@code ExperimentalData}.
 * @see pulse.ui.dialogs.ExportDialog
 *
 */

public class RawDataExporter implements Exporter<ExperimentalData> {

	private static RawDataExporter instance = new RawDataExporter();
	private static HeatingCurveExporter hcExporter = HeatingCurveExporter.getInstance();

	private RawDataExporter() {
		// intentionally left blank
	}

	/**
	 * Retrieves the single static instance of this class
	 * @return an instance of {@code RawDataExporter}.
	 */
	
	public static RawDataExporter getInstance() {
		return instance;
	}
	
	/**
	 * @return {@code ExperimentalData.class}
	 */

	@Override
	public Class<ExperimentalData> target() {
		return ExperimentalData.class;
	}
	
	/**
	 * Invokes the {@code printToStream(...)} method of the {@code HeatingCurveExporter} instance.
	 */

	@Override
	public void printToStream(ExperimentalData target, FileOutputStream fos, Extension extension) {
		hcExporter.printToStream(target, fos, extension);
	}
	
	/**
	 * Currently {@code html} and {@code csv} extensions are supported.
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { Extension.HTML, Extension.CSV };
	}

}