package repository;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import pulse.problem.schemes.rte.exact.NonscatteringRadiativeTransfer;

public class TestProfileLoader {
	
	private TestProfileLoader() {
		//intentionally blank
	}

	public static List<Double> loadTestProfileSparse() {
		return loadTestProfile("/test/TestSolution_Sharp.dat");
	}

	public static List<Double> loadTestProfileDense() {
		return loadTestProfile("/test/TestSolution.dat");
	}

	private static List<Double> loadTestProfile(String testString) {
		File test = null;
		try {
			test = new File(NonscatteringRadiativeTransfer.class.getResource(testString).toURI());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		Scanner scanner = null;
		try {
			scanner = new Scanner(test);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		var testProfile = new ArrayList<Double>();

		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			var numbersStrings = line.split(" ");
			testProfile.add(Double.valueOf(numbersStrings[0]));
		}

		scanner.close();

		return testProfile;
	}

}
