package pulse.io.export;

import java.io.FileOutputStream;

import pulse.input.ExperimentalData;

public class RawDataExporter implements Exporter<ExperimentalData> {

	private static RawDataExporter instance = new RawDataExporter();
	private static HeatingCurveExporter hcExporter = HeatingCurveExporter.getInstance();

	private RawDataExporter() {
		// intentionally left blank
	}

	public static RawDataExporter getInstance() {
		return instance;
	}

	@Override
	public Class<ExperimentalData> target() {
		return ExperimentalData.class;
	}

	@Override
	public void printToStream(ExperimentalData target, FileOutputStream fos, Extension extension) {
		hcExporter.printToStream(target, fos, extension);
	}

}
