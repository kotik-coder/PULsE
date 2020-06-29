package pulse.util;

import static java.io.File.separator;
import static java.io.File.separatorChar;
import static java.lang.Class.forName;
import static java.lang.reflect.Modifier.isAbstract;
import static java.lang.reflect.Modifier.isPublic;
import static pulse.ui.Messages.getString;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * Provides utility methods for finding classes and instances of
 * {@code Reflexive} in a {@code PULsE} package.
 *
 */

public class ReflexiveFinder {

	private ReflexiveFinder() {
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

	/**
	 * Uses Java Reflection API to find all classes within the package named
	 * {@code pckgname}. Works well with .jar files.
	 * 
	 * @param pckgname the name of the package.
	 * @return a list of {@code Class} objects.
	 */

	public static List<Class<?>> classesIn(String pckgname) {
		var name = "" + pckgname;
		if (!name.startsWith(separator))
			name = separatorChar + name;
		name = name.replace('.', separatorChar);

		List<Class<?>> classes = new ArrayList<>();

		String locationPath = null;

		try {
			locationPath = new Object() {
			}.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
		} catch (URISyntaxException e) {
			System.err.println("Failed to initialise the path to the package " + pckgname);
			e.printStackTrace();
		}

		var root = new File(locationPath + name);
		if (root.isDirectory()) {
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

		}

		else {

			ZipInputStream zip = null;
			try {
				zip = new ZipInputStream(new FileInputStream(locationPath));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
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
                    // TODO Auto-generated catch block
                    

		}

		return classes;
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

	@SuppressWarnings("unchecked")
	public static <V extends Reflexive> List<V> simpleInstances(String pckgname, Object... params) {
		List<V> instances = new LinkedList<>();

		for (var aClass : classesIn(pckgname)) {

			if (isAbstract(aClass.getModifiers()))
				continue;

			try {
				// Try to create an instance of the object

				var ctrs = aClass.getDeclaredConstructors();
				V instance = null;

				outer: for (var ctr : ctrs) {

					if (!isPublic(ctr.getModifiers()))
						continue outer;

					var types = ctr.getParameterTypes();

					if (types.length != params.length)
						continue outer;

					for (int i = 0; i < types.length; i++) {
                                            if (!types[i].equals(params[i].getClass()))
                                                if (!types[i].isAssignableFrom(params[i].getClass()))
                                                    continue outer;
                                        }

					try {
						var o = ctr.newInstance(params);
						if (o instanceof Reflexive)
							instance = (V) o;
					} catch (InstantiationException e) {
						System.err.println(getString("ReflexiveFinder.ConstructorAccessError") + ctr);
						e.printStackTrace();
					}

					break;

				}

				if (instance != null) {
					instances.add(instance);
					continue;
				}

				// if the class has a getInstance() method

				var methods = aClass.getMethods();
				instance = null;

				for (var method : methods) {
					if (method.getName().equals("getInstance")) {
						var o = method.invoke(null, new Object[0]);
						if (o instanceof Reflexive)
							instance = (V) o;
						break;
					}
				}

				if (instance != null)
					instances.add(instance);

			} catch (IllegalAccessException | SecurityException iaex) {
				System.err.println("Cannot access: " + aClass);
				iaex.printStackTrace();
			} catch (IllegalArgumentException e) {
				System.err.println(getString("ReflexiveFinder.getInstanceArgumentError") + aClass);
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				System.err.println(getString("ReflexiveFinder.getInstanceError") + aClass);
				e.printStackTrace();
			}

		}

		return instances;

	}

	public static <V extends Reflexive> List<V> simpleInstances(String pckgname) {
		return simpleInstances(pckgname, new Object[0]);
	}

}