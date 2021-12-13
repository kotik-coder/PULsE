package pulse.util;

import static java.lang.Runtime.getRuntime;
import static java.lang.System.err;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Provides unified means of storage and methods of access to runtime system
 * information, such as CPU usage, memory usage, an number of available threads.
 *
 */
public class ResourceMonitor {

    private double memoryUsage;
    private double cpuUsage;
    private int threadsAvailable;

    private static ResourceMonitor instance = new ResourceMonitor();

    private ResourceMonitor() {
        threadsAvailable();
    }

    public void update() {
        cpuUsage();
        memoryUsage();
    }

    /**
     * <p>
     * This will calculate the ratio {@code totalMemory/maxMemory} using the
     * standard {@code Runtime}. Note this memory usage depends on heap
     * allocation for the JVM.
     * </p>
     *
     */
    public void memoryUsage() {
        final double totalMemory = getRuntime().totalMemory();
        final double maxMemory = getRuntime().maxMemory();
        memoryUsage = (totalMemory / maxMemory * 100);
    }

    /**
     * <p>
     * This will calculate the CPU load for the machine running {@code PULsE}.
     * Note this is rather code-intensive, so it is recommended for use only at
     * certain time intervals.
     * </p>
     *
     */
    public void cpuUsage() {

        var mbs = getPlatformMBeanServer();
        ObjectName name = null;
        try {
            name = ObjectName.getInstance("java.lang:type=OperatingSystem");
        } catch (MalformedObjectNameException | NullPointerException e1) {
            err.println("Error while calculating CPU usage:");
            e1.printStackTrace();
        }

        AttributeList list = null;
        try {
            list = mbs.getAttributes(name, new String[]{"ProcessCpuLoad"});
        } catch (InstanceNotFoundException | ReflectionException e) {
            err.println("Error while calculating CPU usage:");
            e.printStackTrace();
        }

        if (!list.isEmpty()) {

            var att = (Attribute) list.get(0);
            var value = (double) att.getValue();

            cpuUsage = value < 0 ? 0 : (value * 100);

        }

    }

    /**
     * Finds the number of threads available for calculation. This will be used
     * by the {@code TaskManager} when allocating the {@code ForkJoinPool} for
     * running several tasks in parallel. The number of threads is greater or
     * equal to the number of cores
     *
     * @see pulse.tasks.TaskManager
     */
    public void threadsAvailable() {
        final int number = getRuntime().availableProcessors();
        threadsAvailable = number > 1 ? (number - 1) : 1;
    }

    public double getCpuUsage() {
        return cpuUsage;
    }

    public int getThreadsAvailable() {
        return threadsAvailable;
    }

    public double getMemoryUsage() {
        return memoryUsage;
    }

    public static ResourceMonitor getInstance() {
        return instance;
    }

}
