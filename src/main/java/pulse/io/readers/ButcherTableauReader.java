package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
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
	}

	public ButcherTableau read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("TBLReader.1"));

		if(! AbstractReader.super.isExtensionSupported(file) )
			throw new IllegalArgumentException("Extension not supported for: " + file);
		
		String name = file.getName();
		if( name.indexOf(".") > 0 )
			name = name.substring(0, name.lastIndexOf("."));
		
		ButcherTableau bt = null;
		
		try ( var fr = new FileReader(file) ; BufferedReader reader = new BufferedReader(fr) ) {
		
			String delims = Messages.getString("}{,\t ");
			StringTokenizer tokenizer;

			//first line with declarations (e.g. FSAL, etc.)
			tokenizer = new StringTokenizer(reader.readLine());
			
			boolean fsal = false;
			
			while(tokenizer.hasMoreTokens())
				if(tokenizer.nextToken(delims).equalsIgnoreCase("FSAL"))
					fsal = true;
			
			int lineno = 0;
			int dimension = 1;
	
			List<Double> lineDouble = new ArrayList<Double>();
	
			/*
			 * Read matrix first
			 */
	
			String line = "";
	
			for (line = reader.readLine(); lineno < dimension; line = reader.readLine(), lineno++) {
				tokenizer = new StringTokenizer(line);
				while (tokenizer.hasMoreTokens())
					lineDouble.add((Double) (ExpressionParser.evaluate(tokenizer.nextToken(delims))));
				if (lineno == 0)
					dimension = lineDouble.size();
			}
	
			double[][] aMatrix = new double[dimension][dimension];
	
			for (int i = 0; i < dimension; i++)
				for (int j = 0; j < dimension; j++)
					aMatrix[i][j] = lineDouble.get(i * dimension + j);
	
			/*
			 * Read vectors
			 */
	
			var v = new double[3][dimension];
			
			lineno = 0;
	
			for (; lineno < 3 && line != null; line = reader.readLine(), lineno++) {
				tokenizer = new StringTokenizer(line);
	
				for (int i = 0; i < dimension; i++)
					v[lineno][i] = (Double) (ExpressionParser.evaluate(tokenizer.nextToken(delims)));
			}
		
			bt = new ButcherTableau(name, aMatrix, v[0], v[1], v[2], fsal);
			
			reader.close();
			
		}

		return bt;

	}

	@Override
	public String getSupportedExtension() {
		return SUPPORTED_EXTENSION;
	}

	public static void main(String[] args) {
		File f = null;
		try {
			f = new File(ButcherTableauReader.class.getResource("/solvers/RKF45.rk").toURI());
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			System.out.println(ButcherTableauReader.getInstance().read(f));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

	}

	public static ButcherTableauReader getInstance() {
		return instance;
	}

}