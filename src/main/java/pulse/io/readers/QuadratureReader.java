package pulse.io.readers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;

import pulse.problem.schemes.rte.dom.OrdinateSet;
import pulse.ui.Messages;

public class QuadratureReader implements AbstractReader<OrdinateSet> {

	private final static String SUPPORTED_EXTENSION = "quad";

	private static QuadratureReader instance = new QuadratureReader();

	private QuadratureReader() {
	}

        @Override
	public OrdinateSet read(File file) throws IOException {
		Objects.requireNonNull(file, Messages.getString("TBLReader.1"));

		//ignore extension!
		
		String name = file.getName().split("\\.")[0];

		OrdinateSet set = null;

		try (var fr = new FileReader(file); var reader = new BufferedReader(fr)) {

			String delims = Messages.getString("}{,\t ");
			StringTokenizer tokenizer;

			List<Double> nodes = new ArrayList<>();
			List<Double> weights = new ArrayList<>();

			String line = "";

			// first line with declarations (e.g. IGNORE, etc.)
			tokenizer = new StringTokenizer(reader.readLine());

			while (tokenizer.hasMoreTokens())
				if (tokenizer.nextToken(delims).equalsIgnoreCase("IGNORE"))
					return null;

			for (line = reader.readLine(); line != null; line = reader.readLine()) {
				tokenizer = new StringTokenizer(line);
				nodes.add((ExpressionParser.evaluate(tokenizer.nextToken(delims))));
				weights.add((ExpressionParser.evaluate(tokenizer.nextToken(delims))));
			}

			set = new OrdinateSet(name, nodes.stream().mapToDouble(d -> d).toArray(),
					weights.stream().mapToDouble(d -> d).toArray());

			reader.close();

		}

		return set;

	}

	@Override
	public String getSupportedExtension() {
		return SUPPORTED_EXTENSION;
	}

	public static QuadratureReader getInstance() {
		return instance;
	}

}
