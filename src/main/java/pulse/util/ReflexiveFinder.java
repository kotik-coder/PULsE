package pulse.util;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static java.lang.Class.forName;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPublic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Provides utility methods for finding classes and instances of
 * {@code Reflexive} in a {@code PULsE} package.
 *
 */

public class ReflexiveFinder {
	
	private static Map<String, List<Class<?>>> classMap = new HashMap<>();

	private ReflexiveFinder() {
		// intentionall blank
	}

	private static List<File> listf(File directory) {

		var files = new ArrayList<File>();

		// Get all files from a directory.
		var fList = directory.listFiles();

		if (fList != null) {

			for (var file : fList) {

				if (file.isFile())
					files.add(file);
				else if (file.isDirectory())
					files.addAll(listf(file));

			}

		}

		return files;

	}

	private static String adjustClassName(String name) {
		var result = "";
		if (!name.startsWith(separator))
			result = separatorChar + name;
		return result.replace('.', separatorChar);
	}

	private static String initialiseLocationPath() {
		String result = null;
		try {
			result = ReflexiveFinder.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			System.err.println("Failed to initialise the general path to ReflxeiveFinder");
			e.printStackTrace();
		}
		return result;
	}

	private static List<Class<?>> listClassesInDirectory(File root, String pckgname) {
		List<Class<?>> classes = new ArrayList<>();
		var files = listf(root);

		files.stream().map(f -> {

			var pathName = f.getName();

			for (var parent = f.getParentFile(); !parent.equals(root); parent = parent.getParentFile()) {
				pathName = parent.getName() + "." + pathName;
			}

			return pathName;

		}).forEach(path -> {
			if (path.endsWith(".class"))
				try {
					classes.add(forName(pckgname + "." + path.substring(0, path.length() - 6)));
				} catch (ClassNotFoundException e) {
					System.err.println("Failed to find the .class file");
					e.printStackTrace();
				}
		});

		return classes;
	}

	private static List<Class<?>> listClassesInJar(String locationPath, String pckgname) {
		ZipInputStream zip = null;
		List<Class<?>> classes = new ArrayList<>();
		try {
			zip = new ZipInputStream(new FileInputStream(locationPath));
		} catch (FileNotFoundException e1) {
			System.err.println("Cannt find the main jar file at " + locationPath);
			e1.printStackTrace();
		}

		try {
			for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
					// This ZipEntry represents a class. Now, what class does it represent?
					var className = entry.getName().replace('/', '.'); // including ".class"
					if (!className.contains(pckgname))
						continue;
					classes.add(forName(className.substring(0, className.length() - ".class".length())));
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return classes;
	}

	/**
	 * Uses Java Reflection API to find all classes within the package named
	 * {@code pckgname}. Works well with .jar files.
	 * 
	 * @param pckgname the name of the package.
	 * @return a list of {@code Class} objects.
	 */

	public static List<Class<?>> classesIn(String pckgname) {
		var name = adjustClassName(pckgname);
		String locationPath = initialiseLocationPath();

		var root = new File(locationPath + name);

		return root.isDirectory() ? listClassesInDirectory(root, pckgname) : listClassesInJar(locationPath, pckgname);
	}

	@SuppressWarnings("unchecked")
	private static <V extends Reflexive> V instanceMethod(Class<?> aClass) {
		// if the class has a getInstance() method
		var methods = aClass.getMethods();

		for (var method : methods) {
			if (method.getName().equals("getInstance")) {
				Object o = null;
				try {
					o = method.invoke(null, new Object[0]);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					e.printStackTrace();
				}
				if (o instanceof Reflexive)
					return (V) o;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <V extends Reflexive> V instanceConstructor(Class<?> aClass, Object... params) {
		var ctrs = aClass.getDeclaredConstructors();

		outer: for (var ctr : ctrs) {

			if (isPublic(ctr.getModifiers())) {

				var types = ctr.getParameterTypes();

				if (Integer.compare(types.length, params.length) == 0) {

					for (int i = 0; i < types.length; i++) {
						if (!types[i].equals(params[i].getClass()))
							if (!types[i].isAssignableFrom(params[i].getClass()))
								continue outer;
					}

					try {
						var o = ctr.newInstance(params);
						if (o instanceof Reflexive)
							return (V) o;
					} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
							| InvocationTargetException e) {
						e.printStackTrace();
					}

				}

			}

		}

		return null;

	}

	/**
	 * <p>
	 * Finds simple instances of {@code Reflexive} subclasses within
	 * {@code pckgname}. A simple instance is either one that results from invoking
	 * a no-argument constructor or a {@code getInstance()} method.
	 * </p>
	 * 
	 * @param <V>      a class implementing {@code Reflexive}
	 * @param pckgname the name of the package for the search
	 * @return a list of classes implementing {@code Reflexive} that are found in
	 *         {@code pckgname}.
	 */

	public static <V extends Reflexive> List<V> simpleInstances(String pckgname, Object... params) {
		List<V> instances = new ArrayList<>();

		//generate a class list only once
		if(classMap.get(pckgname) == null)
			classMap.put( pckgname, classesIn(pckgname) );
		
		for (var aClass : classMap.get(pckgname)) {

			if (isAbstract(aClass.getModifiers()))
				continue;

			// Try to create an instance of the object
			V instance = instanceConstructor(aClass, params);
			if (instance != null)
				instances.add(instance);
			else {
				// if the class has a getInstance() method
				instance = instanceMethod(aClass);
				if (instance != null)
					instances.add(instance);
			}

		}

		return instances;

	}

	public static <V extends Reflexive> List<V> simpleInstances(String pckgname) {
		return simpleInstances(pckgname, new Object[0]);
	}

}