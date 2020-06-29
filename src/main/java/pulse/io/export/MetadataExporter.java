package pulse.io.export;

import static pulse.io.export.Extension.HTML;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.input.Metadata;

public class MetadataExporter implements Exporter<Metadata> {

	private static MetadataExporter instance = new MetadataExporter();

	private MetadataExporter() {
		// intentionally left blank
	}

	public static MetadataExporter getInstance() {
		return instance;
	}

	@Override
	public void printToStream(Metadata metadata, FileOutputStream fos, Extension extension) {
		printHTML(metadata, fos);
	}

	private void printHTML(Metadata meta, FileOutputStream fos) {
            try (var stream = new PrintStream(fos)) {
                stream.print("<table>");
                stream.print("<tr>");
                
                final String METADATA_LABEL = "Metadata";
                final String VALUE_LABEL = "Value";
                
                stream.print("<html>");
                stream.print("<td>");
                stream.print(METADATA_LABEL + "\t");
                stream.print("</td>");
                stream.print("<td>");
                stream.print(VALUE_LABEL + "\t");
                stream.print("</td>");
                
                stream.print("</tr>");
                
                stream.println(" ");
                
                var data = meta.data();
                
                data.forEach(entry -> {
                    stream.print("<tr>");
                    
                    stream.print("<td>");
                    stream.print(entry.getDescriptor(false));
                    stream.print("</td><td>");
                    stream.print(entry.formattedValue());
                    // possible error typecast property -> object
                    stream.print("</td>");
                    
                    stream.println("</tr>");
                });
                
                stream.print("</table>");
                stream.print("</html>");
            }
	}

	@Override
	public void export(Metadata metadata, File file, Extension extension) {
		if (metadata.getExternalID() > -1)
			Exporter.super.export(metadata, file, extension);
	}

	@Override
	public Class<Metadata> target() {
		return Metadata.class;
	}

	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML};
	}

}