import java.net.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

/** 		Caesar  Cipher Client
	
	Description: This client is used with the Network Programming homework
				assignment. It will communicate with a server created by the
				student. 		<br/><br/>

				The client will first jbSend a command to the server. This
				command is ENCRYPT, DECRYPT, or ERROR. The reason for the 
				last command is to test the server's handling of an invalid command.	
				<br/><br/>
				If a valid command is sent, the text will be jrbEncrypted or 
				jrbDecrypted by the server and the text returned.   
				<br/><br/>
				
	Cavaets:	The host must be specified on the command line. This can be done
				either by IP address or by name. 
				
		<pre>	Example: java CaesarClient 123.234.123.12	</pre>	<br/>
	 Date:    July 11, 2001			<br/>
	 Author:  Kevin Bierre			<br/>
	 Modified: Michael Floeser    <br/>
    Version:  1.2
*/

public class CaesarClient extends JFrame {
	private JTextArea jtaSendText;
	private JTextArea jtaRecvText;
	private JPanel 	jpTextPanel;
	private JButton 	jbSend;
	private JButton 	jbExit;
	private JPanel 	jpButtonPanel;
	private JPanel 	jpRadioPanel;
	private ButtonGroup 	bgCmds;
	private JRadioButton jrbEncrypt;
	private JRadioButton jrbDecrypt;
	private JRadioButton jrbError;


	/** Validate command line argument and call create GUI constructor */
	public static void main(String [] args) {
		String host = null;
          // test the arguments - need a host to talk to
          if (args.length > 0) {
                  host = args[0];
          } else {
            //        System.out.println("You must specify a host.");
            //        System.exit(1);
            host = "localhost";
          }		
		// create the client
		CaesarClient cc = new CaesarClient(host);	
	}  // end main


	/** Constructor sets up the GUI */
	public CaesarClient(String host) {
		// setup the frame for the display
		setTitle("Caesar Cipher Client v1.2");
		
		// setup the frame components
		// Text areas first
		jtaSendText = new JTextArea("Send text",10,30);
		jtaSendText.setBorder(new EtchedBorder());
      jtaSendText.setLineWrap( true );           // wrap to new lines 
      jtaSendText.setWrapStyleWord( true );      // split on whole words

		jtaRecvText = new JTextArea("Recv text",10,30);
		jtaRecvText.setBorder(new EtchedBorder());
      jtaRecvText.setLineWrap( true );           // wrap to new lines 
      jtaRecvText.setWrapStyleWord( true );      // split on whole words

		jpTextPanel = new JPanel();
		jpTextPanel.setLayout(new GridLayout(2,1));

		// place the text areas in JScrollPanes
		JScrollPane jbSendPane = new JScrollPane(jtaSendText);

		JScrollPane recvPane = new JScrollPane(jtaRecvText);

		jpTextPanel.add(jbSendPane);
		jpTextPanel.add(recvPane);
		
		// Buttons send & next
		jbSend = new JButton("Send");
		jbExit = new JButton("Exit");
		jpButtonPanel = new JPanel();
		jpButtonPanel.add(jbSend);
		jpButtonPanel.add(jbExit);
		
		// handle the jbExit button
		jbExit.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent ae) {
				System.exit(0);
			}	
		});
	
		// [X] close handler
		addWindowListener(new WindowAdapter(){
			public void windowClosing(WindowEvent we){
				System.exit(0);	
			}	
		});
	
		// Radiobuttons last
		jrbEncrypt = new JRadioButton("Encrypt");
		jrbDecrypt = new JRadioButton("Decrypt");
		jrbError = new JRadioButton("Error");
		bgCmds = new ButtonGroup();
		bgCmds.add(jrbEncrypt);
		bgCmds.add(jrbDecrypt);
		bgCmds.add(jrbError);
		jpRadioPanel = new JPanel();
		jpRadioPanel.add(jrbEncrypt);
		jpRadioPanel.add(jrbDecrypt);
		jpRadioPanel.add(jrbError);
		
		// handle the jbSend button
		SendHandler sh = new SendHandler(host,jtaSendText,jtaRecvText,
													jrbEncrypt,jrbDecrypt,jrbError);
		jbSend.addActionListener(sh);
		
		// now add the components to the frame
		add(jpRadioPanel, BorderLayout.NORTH);
		add(jpButtonPanel,BorderLayout.SOUTH);
		add(jpTextPanel,  BorderLayout.CENTER);
		
      pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}  // end CaesarClient constructor

}  // end CaesarClient class

/**
	This class contains the code that communicates with the server
	and gets resulting data back. It will be called as a result of the
	jbSend button being pressed.
*/
class SendHandler implements ActionListener, CaesarConstants{
	String host;
	JTextArea jtaSendText;
	JTextArea jtaRecvText;
	JRadioButton jrbEncrypt;
	JRadioButton jrbDecrypt;
	JRadioButton jrbError;
	
	/** constructor sets attributes to references to the client */
	public SendHandler(String h, JTextArea st, JTextArea rt, 
						JRadioButton enc, JRadioButton dec, JRadioButton jrbError) {
		host = h;
		jtaSendText = st;
		jtaRecvText = rt;
		jrbEncrypt = enc;
		jrbDecrypt = dec;
		this.jrbError = jrbError;	
	}
	
	/**  performs all the connection work  */
	public void actionPerformed(ActionEvent ae) {
		// setup the socket connection to the host
		Socket s = null;
		BufferedReader in = null;
		PrintWriter out = null;
		try {
			s = new Socket(host,PORT_NUMBER);	// Change this to the interface's PORT_NUMBER
			in = new BufferedReader(new InputStreamReader(s.getInputStream()));
			out = new PrintWriter(new OutputStreamWriter(s.getOutputStream()));
		}
		catch(UnknownHostException uhe) {
			jtaRecvText.setText("Unable to connect to host.");
			return;
		}
		catch(IOException ie) {
			jtaRecvText.setText(ie.getMessage() + "\nIOException communicating with host.");
			return;
		}
		
		// Send the command based on the selected radio button option
		try {
			if(jrbEncrypt.isSelected()) {
				out.println("ENCRYPT");
				out.flush();
			}
		
			if(jrbDecrypt.isSelected()) {
				out.println("DECRYPT");
				out.flush();
			}
		
			if(jrbError.isSelected()) {
				out.println("ERROR");
				out.flush();
			}
		
		    if(!jrbEncrypt.isSelected() && !jrbDecrypt.isSelected() &&
		       !jrbError.isSelected()) {
		       	jtaRecvText.setText("You must first select a command " +
												"(Encrypt/Decrypt/Error)");
               jrbError.setSelected( true );
		       	s.close();
		       	return;
		    }
		    
			// get the results back
			String cmdResponse = in.readLine();
			if(!cmdResponse.equals("OK")) {
				jtaRecvText.setText("Invalid response from server - no text processed.");
				s.close();
				return;
			}
		
			// Send over the text
			jtaRecvText.setText(""); // clear text area
			String orig = jtaSendText.getText();
			if(orig.indexOf('\n',0) == -1) { // no embedded newlines
				out.println(orig);
				out.flush();
				jtaRecvText.append(in.readLine() + "\n");
			}
			else { // embedded newlines detected
				// loop to jbSend over all text
				int start = 0;
				int location;
				while ((location = orig.indexOf('\n',start)) != -1) {
					String tmpStr = orig.substring(start,location);
					out.println(tmpStr);
					out.flush();
					jtaRecvText.append(in.readLine() + "\n");			
					start = location + 1;
				}
				
				if(start < orig.length() - 1) 		 // str does not end in a newline
				{
					String tmpStr = orig.substring(start);
					out.println(tmpStr);
					out.flush();					
					jtaRecvText.append(in.readLine() + "\n");
				}
			}  // end else
			
			// close the connection
			s.close();
		}
		catch(IOException ie) {
			jtaRecvText.setText(ie.getMessage() + "\nIOException communicating with host.");
			return;
		}
      
	}  // end actionPerformed()

} // end SendHandler class