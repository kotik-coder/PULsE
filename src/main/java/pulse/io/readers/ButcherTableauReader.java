package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import pulse.problem.schemes.rte.dom.ButcherTableau;
import pulse.ui.Messages;

public class ButcherTableauReader implements AbstractReader<ButcherTableau> {

	private final static String SUPPORTED_EXTENSION = "rk";
	private static ButcherTableauReader instance = new ButcherTableauReader();

	private ButcherTableauReader() {
		// intentionally blank
	}

	/**
	 * Reads the Butcher tableau stored in {@code file}. The file contents should
	 * be arranged as follows: first row contains specific keywords (e.g. FSAL), second and 
	 * subsequent rows contain the matrix coefficients (the matrix is assumed to be quadratic),
	 * so the number of columns should be equal to the number of rows; the three final rows 
	 * correspond to {@code c}, {@code b} and {@code b^} vectors. Consistency should be maintained
	 *  between the corresponding dimensions.
	 */
	
	@Override
	public ButcherTableau read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("TBLReader.1"));

		// ignore extension!

		String name = file.getName().split("\\.")[0];

		ButcherTableau bt = null;

		String delims = Messages.getString("}{,\t ");
		StringTokenizer tokenizer;
						
		try (var fr = new FileReader(file); BufferedReader reader = new BufferedReader(fr)) {
			// first line with declarations (e.g. FSAL, etc.)
			tokenizer = new StringTokenizer(reader.readLine());

			boolean fsal = false;

			while (tokenizer.hasMoreTokens())
				if (tokenizer.nextToken(delims).equalsIgnoreCase("FSAL"))
					fsal = true;

			var aMatrix = readMatrix(reader, tokenizer, delims);
			var v		= readVectors(reader, tokenizer, delims, aMatrix.length);

			bt = new ButcherTableau(name, aMatrix, v[0], v[1], v[2], fsal);

			reader.close();
		}

		return bt;

	}
	
	private double[][] readMatrix(BufferedReader reader, StringTokenizer tokenizer, String delims) throws IOException {
		List<Double> lineDouble = new ArrayList<>();
		String line = "";
		int lineno = 0;
		int dimension = -1;
		
		for (line = reader.readLine(); lineno < dimension; line = reader.readLine(), lineno++) {
			tokenizer = new StringTokenizer(line);
			while (tokenizer.hasMoreTokens())
				lineDouble.add((ExpressionParser.evaluate(tokenizer.nextToken(delims))));
			if (lineno == 0)
				dimension = lineDouble.size();
		}

		double[][] aMatrix = new double[dimension][dimension];

		for (int i = 0; i < dimension; i++) 
			for (int j = 0; j < dimension; j++) 
				aMatrix[i][j] = lineDouble.get(i * dimension + j);
		
		return aMatrix;
	}
	
	private double[][] readVectors(BufferedReader reader, StringTokenizer tokenizer, String delims, int dimension) throws IOException {
		var v = new double[3][dimension];

		String line = "";
		int lineno = 0;

		for (; lineno < 3 && line != null; line = reader.readLine(), lineno++) {
			tokenizer = new StringTokenizer(line);

			for (int i = 0; i < dimension; i++) 
				v[lineno][i] = (ExpressionParser.evaluate(tokenizer.nextToken(delims)));
			
		}
		
		return v;
		
	}

	@Override
	public String getSupportedExtension() {
		return SUPPORTED_EXTENSION;
	}

	public static ButcherTableauReader getInstance() {
		return instance;
	}

}