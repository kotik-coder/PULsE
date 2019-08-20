package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import pulse.input.ExperimentalData;
import pulse.input.PropertyCurve;
import pulse.util.Reflexive;
import pulse.util.ReflexiveFinder;

public final class ReaderManager {
	
	private static AbstractReader[] allReaders = findAllReaders();
	private static CurveReader[] curveReaders = findHeatingCurveReaders();
	private static PropertyTableReader[] tableReaders = findPropertyTableReaders();
	
	private static List<String> supportedHeatingCurveExtensions = supportedHeatingCurveExtensions();
	private static List<String> supportedThermalDataExtensions = supportedThermalDataExtensions();
		
	private ReaderManager() {
		
	}
	
	private static List<String> supportedHeatingCurveExtensions() {
			CurveReader[] readers = ReaderManager.findHeatingCurveReaders();
			
			List<String> extensions = new LinkedList<String>();
			
			for(CurveReader reader : readers) 
				extensions.add(reader.getSupportedExtension());
			
			return extensions;			
	}
	
	public static List<String> getHeatingCurveExtensions() {
		return supportedHeatingCurveExtensions;
	}
	
	private static List<String> supportedThermalDataExtensions() {
		PropertyTableReader[] readers = ReaderManager.findPropertyTableReaders(); 
		List<String> extensions = new LinkedList<String>();
		
		for(PropertyTableReader reader : readers) 
			extensions.add(reader.getSupportedExtension());
		
		return extensions;			
	}

	public static List<String> getThermalDataExtensions() {
		return supportedThermalDataExtensions;
	}
	
	public static AbstractReader[] findAllReaders(String pckgname) {
        List<AbstractReader> readers = new LinkedList<AbstractReader>();
        List<Reflexive> ref = ReflexiveFinder.simpleInstances(pckgname);       
        
        for(Reflexive r : ref) {
        	if(r instanceof AbstractReader) 
        		readers.add((AbstractReader) r);
        }       
        
        return (AbstractReader[])readers.toArray(new AbstractReader[readers.size()]);
        
	}
	
	public static AbstractReader[] findAllReaders() {
		return findAllReaders(ReaderManager.class.getPackage().getName());
	}
	
	public static CurveReader[] findHeatingCurveReaders(String pckgname) {
		if(allReaders == null)
			allReaders = findAllReaders(pckgname);
		
		List<CurveReader> curveReaders = new LinkedList<CurveReader>();
		
		for(AbstractReader reader : allReaders) 
			if(reader instanceof CurveReader)
				curveReaders.add((CurveReader) reader);
		
		return (CurveReader[])curveReaders.toArray(new CurveReader[curveReaders.size()]);
		
	}
	
	public static CurveReader[] findHeatingCurveReaders() {
		return findHeatingCurveReaders(ReaderManager.class.getPackage().getName());
	}
	
	public static PropertyTableReader[] findPropertyTableReaders(String pckgname) {
		if(allReaders == null)
			allReaders = findAllReaders(pckgname);
		
		List<PropertyTableReader> tableReaders = new LinkedList<PropertyTableReader>(); 
		
		for(AbstractReader reader : allReaders) 
			if(reader instanceof PropertyTableReader)
				tableReaders.add((PropertyTableReader) reader);
		
		return (PropertyTableReader[])tableReaders.toArray(new PropertyTableReader[tableReaders.size()]);
		
	}
	
	public static PropertyTableReader[] findPropertyTableReaders() {
		return findPropertyTableReaders(ReaderManager.class.getPackage().getName());
	}
	
	public static List<ExperimentalData> extractData(File file) throws IOException {
		if(curveReaders == null)
			curveReaders = findHeatingCurveReaders();		
		
		String supportedExtension;
		CurveReader selected = null;		
		
		first : for(CurveReader reader : curveReaders) {
			supportedExtension = reader.getSupportedExtension();
			
			if(AbstractReader.checkExtensionSupported(file, supportedExtension)) {
				selected = reader;
				break first;
			}
		
		supportedExtension = ""; 
		
		}
		
		if(selected == null)
			throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName()); //$NON-NLS-1$
		
		return selected.read(file);
		
		
	}
	
	public static PropertyCurve readPropertyTable(File file) throws IOException {
		if(tableReaders == null)
			tableReaders = findPropertyTableReaders();		
		
		String supportedExtension;
		PropertyTableReader selected = null;		
		
		first : for(PropertyTableReader reader : tableReaders) {
			supportedExtension = reader.getSupportedExtension();
			
			if(AbstractReader.checkExtensionSupported(file, supportedExtension)) {
					selected = reader;
					break first;
			}
			
			supportedExtension = ""; //$NON-NLS-1$
		}
		
		if(selected == null)
			throw new IllegalArgumentException(Messages.getString("ReaderManager.1") + file.getName()); //$NON-NLS-1$
		
		return selected.read(file);
		
		
	}
    	
}
