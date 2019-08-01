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

public class ReflexiveFinder {
	
	private ReflexiveFinder() {
		
	}

	public static String[] packageDirectoryList(String pckgname) {		
        String name = new String(pckgname);
        if (!name.startsWith(File.separator))  
            name = File.separatorChar + name;
        name = name.replace('.',File.separatorChar);
       
        List<String> classNames = new ArrayList<String>();
        
        String locationPath = null;
        
        try {
        	locationPath = new Object() {}. getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
    	} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
     
       	File root = new File(locationPath + name);
       	if(root.isDirectory()) {
       		String[] files = root.list(); 
       		for(String file : files) {
       			if(file.endsWith(".class"))
       					classNames.add(pckgname + "." + file.substring(0, file.length() - 6));
       		}
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
	                classNames.add(className.substring(0, className.length() - ".class".length()));
	            }
	        } }
			catch(IOException e) {
				e.printStackTrace();
			}
		
       	}
        
       return classNames.toArray(new String[classNames.size()]);        
	}	
	
	public static <V extends Reflexive> List<V> findAllInstances(String pckgname) {
		List<V> instances = new LinkedList<V>();
		
        for (String classname : ReflexiveFinder.packageDirectoryList(pckgname)) {        	
                 
                    try {
                        // Try to create an instance of the object                    	
                    	
                    	Constructor<?>[] ctrs = Class.forName(classname).getDeclaredConstructors(); //$NON-NLS-1$);
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
									System.err.println(Messages.getString("ReflexiveFinder.ConstructorAccessError") + ctr); //$NON-NLS-1$
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
                        
                        Method[] methods = Class.forName(classname).getMethods(); //$NON-NLS-1$
                    	instance = null;
                    	
                    	for (Method method : methods) {
                    	  if (method.getName().equals("getInstance")) { //$NON-NLS-1$
                    		Object o = method.invoke(null, new Object[0]);
                    		if(o instanceof Reflexive)
                    			instance = (V) o;
                    	    break;
                    	  }
                    	}
                    	
                    	if(instance != null)                    	                    	                    	                    	
                    		instances.add(instance);         
                        
                    } catch (ClassNotFoundException cnfex) {
                    	System.err.println(Messages.getString("ReflexiveFinder.SearchError") + classname); //$NON-NLS-1$
                        cnfex.printStackTrace();
                    } catch (IllegalAccessException iaex) {
                    	System.err.println("Cannot access: " + classname); //$NON-NLS-1$
                    	iaex.printStackTrace();
                    } catch (SecurityException e) {
                    	System.err.println("Cannot access: " + classname); //$NON-NLS-1$
						e.printStackTrace();
					} catch (IllegalArgumentException e) {
						System.err.println(Messages.getString("ReflexiveFinder.getInstanceArgumentError") + classname); //$NON-NLS-1$
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						System.err.println(Messages.getString("ReflexiveFinder.getInstanceError") + classname); //$NON-NLS-1$
						e.printStackTrace();
					}
                
            }               
        
        return instances;
        
	}
	
	public static <V extends Reflexive> List<V> findAllInstances(V v) {
		return findAllInstances(v.getClass().getPackage().getName());
	}
	
}