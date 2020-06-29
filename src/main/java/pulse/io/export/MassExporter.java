package pulse.io.export;

import static java.io.File.separator;
import static java.lang.System.err;
import static pulse.io.export.ExportManager.export;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import pulse.util.Group;

public class MassExporter {

	private MassExporter() {

	}

	/**
	 * <p>
	 * Recursively analyses all {@code Accessible}s that this object owns (including
	 * only children) and chooses those that are {@code Saveable}.
	 * </p>
	 * 
	 * @return a full list of {@code Saveable}s.
	 */

	public static Set<Group> contents(Group root) {
		var contents = new HashSet<Group>();

		try {

			root.subgroups().stream().forEach(ph -> {
				/*
				 * Filter only children, not parents!
				 */

				if (root.getParent() != ph)
					contents.add(ph);

			}

			);

			for (var it = contents.iterator(); it.hasNext();) {
                            contents(it.next()).stream().forEach(a -> contents.add(a));
                        }

		} catch (IllegalArgumentException e) {
			err.println("Unable to generate saveable contents for " + root.getClass());
			e.printStackTrace();
		}

		return contents;
	}

	public static void exportGroup(Group ac, File directory, Extension extension) {
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + directory);

		var internalDirectory = new File(directory + separator + ac.describe() + separator);
		internalDirectory.mkdirs();

		export(ac, directory, extension);
		contents(ac).stream().forEach(internalHolder -> {
			export(internalHolder, internalDirectory, extension);
		});
	}

}