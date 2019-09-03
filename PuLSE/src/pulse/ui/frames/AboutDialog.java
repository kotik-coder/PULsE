package pulse.ui.frames;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.ui.Messages;

public class AboutDialog extends JDialog {

	private final static int WIDTH = 570;
	private final static int HEIGHT = 370;
	
	public AboutDialog() {
		
		setTitle(Messages.getString("TaskControlFrame.AboutDialog"));
		setAlwaysOnTop(true);
		setSize(WIDTH, HEIGHT);
		
		BufferedReader reader = new BufferedReader( new InputStreamReader(
				getClass().getResourceAsStream("/About.html"))
				);
		
		 StringBuilder sb = new StringBuilder();
		 String str; 
		 
		 try {
			while ((str = reader.readLine()) != null)     
			     sb.append(str);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		JTextPane textPane = new JTextPane();
		textPane.setBackground(UIManager.getColor("Panel.background"));
		textPane.setEditable(false);
		textPane.setContentType("text/html");
		
		final HTMLDocument doc	= (HTMLDocument) textPane.getDocument();
		final HTMLEditorKit kit = (HTMLEditorKit) textPane.getEditorKit();
		try {
			kit.insertHTML(doc, doc.getLength(), sb.toString(), 0, 0, null);
		} catch (BadLocationException e) {
			System.err.println(Messages.getString("LogPane.InsertError")); //$NON-NLS-1$
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println(Messages.getString("LogPane.PrintError")); //$NON-NLS-1$
			e.printStackTrace();
		}
		
		getContentPane().add(new JScrollPane(textPane), BorderLayout.CENTER);
		
	}
	
}
