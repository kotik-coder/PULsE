package pulse.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import pulse.ui.Messages;

/**
 * Provides utility methods for finding classes and instances of {@code Reflexive} in a {@code PULsE} package.
 *
 */

public class ReflexiveFinder {
	
	private ReflexiveFinder() { }
	
	private static List<File> listf(File directory) {

		var files = new ArrayList<File>();
		
	    // Get all files from a directory.
	    File[] fList = directory.listFiles();	    
	    
	    if(fList != null) {
	    	
	        for (File file : fList) {
	        	
	            if (file.isFile()) 
	            	files.add(file);
	            else if (file.isDirectory()) 
	            	files.addAll( listf(file) );
	            
	        }
	        
	    }
	
		return files;
		
	}
	
	/**
	 * Uses Java Reflection API to find all classes within the package named {@code pckgname}. Works well with .jar
	 * files.
	 * @param pckgname the name of the package.
 	 * @return a list of {@code Class} objects.
	 */
	
	public static List<Class<?>> classesIn(String pckgname) {		
        String name = "" + pckgname;
        if (!name.startsWith(File.separator))  
            name = File.separatorChar + name;
        name = name.replace('.',File.separatorChar);
       
        List<Class<?>> classes = new ArrayList<Class<?>>();
        
        String locationPath = null;
        
        try {
        	locationPath = new Object() {}. getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
    	} catch (URISyntaxException e) {
			System.err.println("Failed to initialise the path to the package " + pckgname);
			e.printStackTrace();
		}
     
       	File root = new File(locationPath + name);
       	if(root.isDirectory()) {
       		List<File> files = listf(root); 
       		
       		files.stream().map(f -> f.getParentFile().equals(root) ? 
       									f.getName() :
       									f.getParentFile().getName() + "." + f.getName()  
       								).forEach(path ->
       			{       		
       			if(path.endsWith(".class"))
					try {
						classes.add(
       							Class.forName(
       									pckgname + "." + path.substring(0, path.length() - 6)));
					} catch (ClassNotFoundException e) {
						System.err.println("Failed to find the .class file");
						e.printStackTrace();
					}
       		});
       		
		}
       	
       	else {
        
	        ZipInputStream zip = null;    
			try {
				zip = new ZipInputStream( new FileInputStream ( locationPath ) );
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		   		
			try {
	        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
	            if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
	                // This ZipEntry represents a class. Now, what class does it represent?
	                String className = entry.getName().replace('/', '.'); // including ".class"
	                if(!className.contains(pckgname))
	                	continue;
	                classes.add(
	                		Class.forName(
	                				className.substring(0, className.length() - ".class".length()))
	                				);
	            }
	        } }
			catch(IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
       	}
        
       return classes;        
	}	
	
	/**
	 * <p>Finds simple instances of {@code Reflexive} subclasses within {@code pckgname}. 
	 * A simple instance is either one that results from invoking a no-argument constructor or a {@code getInstance()} method.</p> 
	 * @param <V> a class implementing {@code Reflexive}
 	 * @param pckgname the name of the package for the search
	 * @return a list of classes implementing {@code Reflexive} that are found in {@code pckgname}.
	 */
	
	@SuppressWarnings("unchecked")
	public static <V extends Reflexive> List<V> simpleInstances(String pckgname) {
		List<V> instances = new LinkedList<V>();
		
        for (Class<?> aClass : ReflexiveFinder.classesIn(pckgname)) {

        	if( Modifier.isAbstract( aClass.getModifiers() ) )
        		continue;
        	        	
                    try {
                        // Try to create an instance of the object                    	
                    	
                    	Constructor<?>[] ctrs = aClass.getDeclaredConstructors();
                    	V instance = null;
                    	                                   	
                    	for (Constructor<?> ctr : ctrs) {
                    		if (!Modifier.isPublic(ctr.getModifiers()))
                    			continue;
                    	    if (ctr.getGenericParameterTypes().length == 0) {                    		                    	                  	
                    	    	try {
                    	    		Object o = ctr.newInstance();
									if(o instanceof Reflexive)
                    	    			instance = (V) o;
								} catch (InstantiationException e) {
									System.err.println(Messages.getString("ReflexiveFinder.ConstructorAccessError") + ctr); 
									e.printStackTrace();
								}    
                    	    	break;
                    	    }
                    	  
                    	}                    	
                    	
                    	if(instance != null) {    
                    		instances.add(instance);
                    		continue;
                    	}
                        
                        //if the class has a getInstance() method                    	
                        
                        Method[] methods = aClass.getMethods(); 
                    	instance = null;
                    	
                    	for (Method method : methods) {
                    	  if (method.getName().equals("getInstance")) { 
                    		Object o = method.invoke(null, new Object[0]);
                    		if(o instanceof Reflexive)
                    			instance = (V) o;
                    	    break;
                    	  }
                    	}
                    	
                    	if(instance != null)                    	                    	                    	                    	
                    		instances.add(instance);         
                        
                    } catch (IllegalAccessException iaex) {
                    	System.err.println("Cannot access: " + aClass); 
                    	iaex.printStackTrace();
                    } catch (SecurityException e) {
                    	System.err.println("Cannot access: " + aClass); 
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						System.err.println(Messages.getString("ReflexiveFinder.getInstanceArgumentError") + aClass); 
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						System.err.println(Messages.getString("ReflexiveFinder.getInstanceError") + aClass); 
						e.printStackTrace();
					}
                
            }               
        
        return instances;
        
	}
	
}