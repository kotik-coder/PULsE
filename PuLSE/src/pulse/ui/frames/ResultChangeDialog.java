package pulse.ui.frames;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import pulse.tasks.ResultFormat;
import pulse.ui.Messages;

import java.awt.Font;
import javax.swing.JTextPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;

public class ResultChangeDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JTextField textField;
	
	private final static int WIDTH = 500;
	private final static int HEIGHT = 500;
	
	public ResultChangeDialog() {
		
		JDialog reference = this;
		
		setTitle("Change of Output Format");
		
		setSize(WIDTH, HEIGHT);
				
		BufferedReader reader = new BufferedReader( new InputStreamReader(
				getClass().getResourceAsStream("/ResultFormatDescription.html"))
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
		textPane.setFont(new Font("Arial", Font.PLAIN, 14));
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
		
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(this.getWidth(), 35));
		getContentPane().add(panel, BorderLayout.SOUTH);
		panel.setLayout(new GridLayout(0, 2, 0, 0));
		
		textField = new JTextField();
		AbstractDocument absDoct = (AbstractDocument)textField.getDocument();
		absDoct.setDocumentFilter(new DocumentFilter() {
			
		    @Override
		    public void insertString(DocumentFilter.FilterBypass fb, int offset,
		            String text, AttributeSet attr)
		            throws BadLocationException {
		        
		    	StringBuilder buffer = new StringBuilder(text.length());
		        char[] allowedChars = ResultFormat.getAllowedCharacters();
		        
		        for (char ch : text.toCharArray()) {
			             
			        for(char allowedCh : allowedChars) {
			        	if(Character.toLowerCase(ch) == Character.toLowerCase(allowedCh)) {
			        	
			        		buffer.append(Character.toUpperCase(ch));
			        		break;
			        		
			        	}
			        }
		        
		        }
		        
		        super.insertString(fb, offset, buffer.toString(), attr);
		    
		    }

		    @Override
		    public void replace(DocumentFilter.FilterBypass fb,
		            int offset, int length, String string, AttributeSet attr) throws BadLocationException {
		        if (length > 0) {
		        fb.remove(offset, length);
		        }
		        insertString(fb, offset, string, attr);
		    }
		
			
		});
		
		textField.setText(ResultFormat.getFormat().toString());
		panel.add(textField);
		textField.setColumns(10);
		
		JButton btnApply = new JButton("Apply");
		panel.add(btnApply);
		
		btnApply.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				
				char[] fmt = textField.getText().toCharArray();
				
				//search for duplicates
				
				boolean duplicateFound = false;
				
				outer : for(int i = 0; i < fmt.length; i++) {
					
					for(int j = i + 1; j < fmt.length; j++) {
						
						if(fmt[i] == fmt[j]) {
							duplicateFound = true;
							break outer;
						}
						
					}
					
				}				
				
				if(duplicateFound) {
					JOptionPane.showMessageDialog(reference, "Duplicate symbol found in format string. Please correct!", "Duplicate Symbol", JOptionPane.WARNING_MESSAGE);
					textField.setText(ResultFormat.getFormat().toString());
					return;
				}
			
				boolean formatError = false;
				
				for(char cm : ResultFormat.getMinimumAllowedFormat()) {
					if( textField.getText().indexOf(cm) < 0) {
						formatError = true;
						break;
					}
				}
				
				if(formatError) {
					JOptionPane.showMessageDialog(reference, "The following characters are required: " + 
							new String(ResultFormat.getMinimumAllowedFormat()), "Wrong Format", JOptionPane.WARNING_MESSAGE);
					textField.setText(ResultFormat.getFormat().toString());
					return;
				}
				
				ResultFormat.generateFormat(textField.getText());
				
			}
			
		});
		
		
	}

}
