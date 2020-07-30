package pulse.ui.frames.dialogs;

import static java.awt.BorderLayout.CENTER;
import static java.lang.System.err;
import static javax.swing.UIManager.getColor;
import static pulse.ui.Messages.getString;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

@SuppressWarnings("serial")
public class AboutDialog extends JDialog {

	private final static int WIDTH = 570;
	private final static int HEIGHT = 370;

	public AboutDialog() {

		setTitle(getString("TaskControlFrame.AboutDialog"));
		setAlwaysOnTop(true);
		setSize(WIDTH, HEIGHT);

		var reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/About.html")));
		var sb = new StringBuilder();
		String str;

		try {
			while ((str = reader.readLine()) != null)
				sb.append(str);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		var textPane = new JTextPane();
		textPane.setBackground(getColor("Panel.background"));
		textPane.setEditable(false);
		textPane.setContentType("text/html");

		final var doc = (HTMLDocument) textPane.getDocument();
		final var kit = (HTMLEditorKit) textPane.getEditorKit();
		try {
			kit.insertHTML(doc, doc.getLength(), sb.toString(), 0, 0, null);
		} catch (BadLocationException e) {
			err.println(getString("LogPane.InsertError")); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			err.println(getString("LogPane.PrintError")); //$NON-NLS-1$
			e.printStackTrace();
		}

		getContentPane().add(new JScrollPane(textPane), CENTER);

	}

}
