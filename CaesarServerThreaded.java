import java.lang.StringBuilder;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

//implemets Runnable-> the class has to have a run method
public class CaesarServerThreaded implements Runnable, CaesarConstants {

	protected ServerSocket serverSocket = null;
	protected int serverPort = 0;
	protected static int shiftValue = 0;
	protected boolean isStopped = false;
	protected Thread runningThread = null;
   //creatinga thread poll of ten threads=> making sure it only creates a max of ten threads or less
   //limit resources your using 
	protected ExecutorService threadPool = Executors.newFixedThreadPool(10);

	private static String client = null;
	private static long clientCount = 0;

	public CaesarServerThreaded(int port) {
		this.serverPort = port;
		System.out.println("Starting server on port " + port);
	}
   //all threads start at the run method
	public void run() {

		synchronized(this) {
			this.runningThread = Thread.currentThread();
		}
		openServerSocket();				// go open designated port on this server
		while ( !isStopped() ) {
			Socket clientSocket = null;
			try {
				System.out.println("waiting for client to connect");
				clientSocket = this.serverSocket.accept();			 //.accept -> will block until client connects
				client = "client " + ++clientCount;
			} catch (java.net.SocketException e) {
				System.out.println("connection reset");
			} catch (NumberFormatException | IOException e) {
				if (isStopped()) {
					System.out.println("Server stopped");
					break;
				}
				throw new RuntimeException("Error accepting client connection", e);
			}

			//start new thread to service this client
			this.threadPool.execute( new WorkerRunnable(clientSocket, "Thread Pooled Server"));
			
		}
		this.threadPool.shutdown();
		System.out.println("Server Stopped.") ;
	}

	private synchronized boolean isStopped() {
		return this.isStopped;
	}

	public synchronized void stop(){
		this.isStopped = true;
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			throw new RuntimeException("Error closing server", e);
		}
	}

	private void openServerSocket() {
		try {
			this.serverSocket = new ServerSocket(this.serverPort);
		} catch (IOException e) {
			throw new RuntimeException("Cannot open port " + this.serverPort, e);
		}
	}

	public static void main(String[] args) {

		if (args.length > 0 ) {
			shiftValue = Integer.parseInt(args[0]);		// use shift value if passed in
			if (shiftValue < 1 || shiftValue > 25) {
				shiftValue = DEFAULT_SHIFT;				// limit to default if out of range
			}
		} else {
			shiftValue = DEFAULT_SHIFT;					// else use default shift
		}
		int portNum = PORT_NUMBER;

		CaesarServerThreaded server = new CaesarServerThreaded(portNum);	// create server instance
		new Thread(server).start();											// start server
	}
     //InNER CLASS
	// Threads come here when a connection happens
	class WorkerRunnable implements Runnable{// Runnable has a run method at 118

		protected Socket clientSocket = null;
		protected String serverText   = null;

		public WorkerRunnable(Socket clientSocket, String serverText) {
			this.clientSocket = clientSocket;
			this.serverText   = serverText;
		}

		public void run() {
			try {
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
				String line;
				String commandWord;
				String textIn;
				String response;
				String processNext = "";

				// read incoming lines
				while ((line = in.readLine()) != null) {
					String lineIn = line.trim();
					System.out.println("Server received: " + lineIn);

					//String[] parsedLine = lineIn.split(" ");
					//commandWord = parsedLine[0].toUpperCase();	// command word is 1st word

					if (!processNext.isEmpty()) {		// if this is 2nd part of command
						commandWord = processNext;		// go process that command		
						textIn = lineIn;
						
					} else {
						commandWord = lineIn.trim().toUpperCase();		// else get 1st part of command
						textIn = "";
                  //Gets command of encrypt or decrypt and send ok to client
						if (commandWord.equals("ENCRYPT") || commandWord.equals("DECRYPT")) {
							processNext = commandWord;	// if valid command
							out.println( "OK" );		// send OK back to client
							continue;					// skip rest of loop
						} else if (commandWord.equals("BYE")) {
							System.out.println("client says BYE");
							out.println("Server hanging up");	// echo back to client
							break;								// exit loop
						} else {
							processNext = "";
							out.println( "ERROR" );		// send ERROR back to client
							continue;					// skip rest of loop				
						}
					}
					
					switch (commandWord) {		// branch on command word
					case "ENCRYPT":				// do encrypt
						System.out.println("in switch for ENCRYPT");
						response = doEncryption( textIn );		// do encryption
						out.println(response);					// send response back to client
						processNext = "";
						break;
					
					case "DECRYPT":				// do decrypt
						System.out.println("in switch for DECRYPT");
						response = doDecryption( textIn );		// do decryption// send response back to client
						out.println(response);					
						processNext = "";
						break;

					default :
						response = "in switch DEFAULT";
						System.out.println(response);
						out.println( "ERROR" );				// send ERROR back to client
						processNext = "";
					}
				}
				in.close();
				out.close();

			} catch (SocketException e) {
				System.out.println("Connection reset");
				e.printStackTrace();
			} catch (IOException e) {
				//report exception somewhere.
				e.printStackTrace();
			}

		}
		
		public String doEncryption(String textIn) {
			System.out.println("Server in Encryption  shift value " + shiftValue);
			System.out.println("text in >" + textIn + "<");
			char ch;
			StringBuilder sb = new StringBuilder();
			
			for(int i = 0; i < textIn.length(); ++i){
				ch = textIn.charAt(i);
				
				if(ch >= 'a' && ch <= 'z'){			// if lower case
		            ch = (char)(ch + shiftValue);	// encrypt char	
		            if(ch > 'z'){
		                ch = (char)(ch - 'z' + 'a' - 1);	// handle wraparound
		            }
		            sb.append(ch);
		            
		        } else if(ch >= 'A' && ch <= 'Z'){	// if upper case
		            ch = (char)(ch + shiftValue);	// encrypt char
		            if(ch > 'Z'){
		                ch = (char)(ch - 'Z' + 'A' - 1);	// handle wraparound
		            }
		            sb.append(ch);
		        }
		        else {
		        	sb.append(ch);
		        }
			}
			System.out.println("Encrypted text = " + sb.toString());
			return sb.toString();
		}
		
		public String doDecryption(String textIn) {
			System.out.println("Server in Decryption  shift value " + shiftValue);
			System.out.println("text in >" + textIn + "<");
			char ch;
			StringBuilder sb = new StringBuilder();
			
			for(int i = 0; i < textIn.length(); ++i){
				ch = textIn.charAt(i);
				
				if (ch >= 'a' && ch <= 'z') {
		            ch = (char)(ch - shiftValue);
		            if (ch < 'a') {
		                ch = (char)(ch + 'z' - 'a' + 1);
		            }
		            sb.append(ch);
		            
		        } else if (ch >= 'A' && ch <= 'Z') {
		            ch = (char)(ch - shiftValue);
		            if(ch < 'A'){
		                ch = (char)(ch + 'Z' - 'A' + 1);
		            }
		            sb.append(ch);
		        }
		        else {
		        	sb.append(ch);
		        }
			}
			System.out.println("Decrypted text = " + sb.toString());
			return sb.toString();
		}
	}
}
