package pulse.util;

import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An object is declared {@code Reflexive} if {@code PULsE} needs to know all
 * its available subclasses.
 *
 */
public interface Reflexive {

    public static <T extends Reflexive> List<T> instancesOf(Class<T> reflexiveType, Object... params) {
        return instancesOf(reflexiveType, reflexiveType.getPackage().getName(), params);
    }

    public static <T extends Reflexive> List<T> instancesOf(Class<T> reflexiveType, String pckgname) {
        return instancesOf(reflexiveType, pckgname, new Object[0]);
    }

    /**
     * Uses the {@code ReflexiveFinder} to create a list of simple instance of
     * {@code reflexiveType} generated by any classes listed in the package
     * {@code pckgname}.
     *
     * @see ReflexiveFinder.simpleInstances(String)
     * @param <T> a class implementing {@code Reflexive}
     * @param reflexiveType a class that extends {@code T}
     * @param pckgname the String with the package name
     * @return a list of {@code Reflexive} conforming with the conditions above.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Reflexive> List<T> instancesOf(Class<T> reflexiveType, String pckgname, Object... params) {
        return (List<T>) ReflexiveFinder.simpleInstances(pckgname, params).stream()
                .filter(r -> reflexiveType.isAssignableFrom(r.getClass())).collect(Collectors.toList());
    }

    /**
     * Uses the {@code ReflexiveFinder} to create a list of simple instance of
     * {@code reflexiveType} generated by any classes listed in the same package
     * where the {@code reflexiveType} is found.
     *
     * @see ReflexiveFinder.simpleInstances(String)
     * @param <T> a class implementing {@code Reflexive}
     * @param reflexiveType a class that extends {@code T}
     * @return a list of {@code Reflexive} conforming with the conditions above.
     */
    public static <T extends Reflexive> List<T> instancesOf(Class<T> reflexiveType) {
        return Reflexive.instancesOf(reflexiveType, reflexiveType.getPackage().getName());
    }

    public static <T extends PropertyHolder & Reflexive> T instantiate(Class<T> c, String descriptor) {
        var opt = Reflexive.instancesOf(c).stream().filter(test -> test.getDescriptor().equals(descriptor)).findFirst();
        return opt.get();
    }

    public static <T extends PropertyHolder & Reflexive> Set<String> allDescriptors(Class<T> c) {
        return Reflexive.instancesOf(c).stream().map(t -> t.getDescriptor()).collect(Collectors.toSet());
    }

    public static <T extends Reflexive> Set<String> allSubclassesNames(Class<T> c) {
        var classes = ReflexiveFinder.classesIn(c.getPackageName());
        return classes.stream().filter(cl -> c.isAssignableFrom(cl) && !Modifier.isAbstract(cl.getModifiers()))
                .map(aClass -> aClass.getSimpleName()).collect(Collectors.toSet());
    }

}
