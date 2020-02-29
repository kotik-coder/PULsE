package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.HeatingCurve;
import pulse.ui.Messages;

public class HeatingCurveExporter implements Exporter<HeatingCurve> {

	private static HeatingCurveExporter instance = new HeatingCurveExporter();
	
	private HeatingCurveExporter() {
		// Intentionally blank
	}
	
	@Override
	public void printToStream(HeatingCurve hc, FileOutputStream fos, Extension extension) {		
		if(hc.arraySize() < 1)
			return;
			
		switch(extension) {
			case HTML : printHTML(hc, fos); break;
			case CSV : printCSV(hc, fos); break;
			default : 
				throw new IllegalArgumentException("Format not recognised: " + extension);
		}		
	}
	
	/**
	 * The supported extensions for exporting the data contained in this object. Currently include {@code .html} and {@code .csv}.
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Extension.HTML, Extension.CSV};
	}
	
	private void printHTML(HeatingCurve hc, FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
				
		var residuals = hc.getResiduals();		
        int residualsLength = residuals == null ? 0 : residuals.size();
		
		stream.print("<table>"); 
		stream.print("<tr>"); 
	
		final String TIME_LABEL = Messages.getString("HeatingCurve.6"); 
		final String TEMPERATURE_LABEL = Messages.getString("HeatingCurve.7");
		final String TIME_LABEL_R = "Time signal (s)";
		final String RESIDUAL_LABEL = "Residual";
		
       	stream.print("<td>" + TIME_LABEL + "\t</td>");
       	stream.print("<td>" + TEMPERATURE_LABEL + "\t</td>");
       	
       	if(residualsLength > 0) {        	
	       	stream.print("<td>" + TIME_LABEL_R + "\t</td>");
	       	stream.print("<td>" + RESIDUAL_LABEL + "</td>");
       	}
       	
        stream.print("</tr>");

        stream.println("");
        
        double t, T, tr, Tr;

        int size = hc.actualDataPoints();
        int nominalSize = (int)hc.getNumPoints().getValue();        
        int finalSize = size < nominalSize ? size : nominalSize;
        int cycle = Math.max(finalSize, residualsLength);
        
        for (int i = 0; i < finalSize; i++) {
        	stream.print("<tr>"); 
            
        		stream.print("<td>");
            	t = hc.timeAt(i);
                stream.printf("%.6f %n", t);
                stream.print("\t</td><td>"); 
                T = hc.temperatureAt(i);
                stream.printf("%.6f %n</td>", T); 
                
                if(residualsLength > 0) {
                	tr = residuals.get(i)[0];
                	stream.printf("\t<td>%3.4f</td>", tr);
                	Tr = residuals.get(i)[1];
                	stream.printf("\t<td>%3.6f</td>", Tr);
                }
            
            stream.println("</tr>");
        }
        
        for (int i = finalSize; i < cycle; i++) {
        	stream.println("<tr>");
        	tr = residuals.get(i)[0];
        	stream.printf("<td></td>\t<td></td>\t<td>%3.4f</td>\t", tr);
        	Tr = residuals.get(i)[1];
        	stream.printf("<td>%3.6f</td>", Tr);
        	stream.print("</tr>");
        }    
        
        stream.print("</table>");
        stream.close();
        
	}
	
	private void printCSV(HeatingCurve hc, FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		
		var residuals = hc.getResiduals();
        int residualsLength = residuals == null ? 0 : residuals.size();
		
		final String TIME_LABEL = Messages.getString("HeatingCurve.6");
		final String TEMPERATURE_LABEL = hc.getPrefix();
		stream.print(TIME_LABEL + "\t" + TEMPERATURE_LABEL + "\t");
		if(residualsLength > 0) 
			stream.print(TIME_LABEL + "\tResidual");		
       	
        double t, T, tr, Tr;

        int size = hc.actualDataPoints();
        int nominalSize = (int)hc.getNumPoints().getValue();        
        int finalSize = size < nominalSize ? size : nominalSize;   
                
        int cycle = Math.max(finalSize, residualsLength);
        
        for (int i = 0; i < finalSize; i++) {        	
	        t = hc.timeAt(i);
	        stream.printf("%n%3.4f", t); 
	        T = hc.temperatureAt(i);
	        stream.printf("\t%3.4f", T);        	
            if(residualsLength > 0) {
            	tr = residuals.get(i)[0];
            	stream.printf("\t%3.4f", tr);
            	Tr = residuals.get(i)[1];
            	stream.printf("\t%3.6f", Tr);
            }
        }
        
        for (int i = finalSize; i < cycle; i++) {  
	        stream.printf("%n-"); 
	        stream.printf("\t-");   
            tr = residuals.get(i)[0];
            stream.printf("\t%3.4f", tr);
            Tr = residuals.get(i)[1];
            stream.printf("\t%3.6f", Tr);
        }
        
        stream.close();
        
	}

	public static HeatingCurveExporter getInstance() {
		return instance;
	}

	@Override
	public Class<HeatingCurve> target() {
		return HeatingCurve.class;
	}	

}