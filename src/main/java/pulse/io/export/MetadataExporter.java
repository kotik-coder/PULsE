package pulse.io.export;

import static pulse.io.export.Extension.HTML;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.input.Metadata;

/**
 * A singleton class used to export {@code Metadata} objects in a html format.
 *
 */

public class MetadataExporter implements Exporter<Metadata> {

	private static MetadataExporter instance = new MetadataExporter();

	private MetadataExporter() {
		// intentionally left blank
	}
	
	/**
	 * Retrieves the single instance of this class.
	 * @return a single instance of {@code MetadataExporter}.
	 */
	
	public static MetadataExporter getInstance() {
		return instance;
	}
	
	/**
	 * Prints the metadata content in html format in two columns, where the first column forms the 
	 * description of the entry and the second column gives its value. Extension is ignored, as 
	 * only html is supported.
	 */

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
                    stream.print(entry.formattedOutput());
                    // possible error typecast property -> object
                    stream.print("</td>");
                    
                    stream.println("</tr>");
                });
                
                stream.print("</table>");
                stream.print("</html>");
            }
	}
	
	/**
	 * Ignores metadata whose external IDs are negative, otherwise calls the superclass method.
	 */

	@Override
	public void export(Metadata metadata, File file, Extension extension) {
		if (metadata.getExternalID() > -1)
			Exporter.super.export(metadata, file, extension);
	}

	/**
	 * @return {@code Metadata.class}
	 */
	
	@Override
	public Class<Metadata> target() {
		return Metadata.class;
	}

	/**
	 * @return a single-element array containing {@code Extension.HTML}
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] { HTML};
	}

}