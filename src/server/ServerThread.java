package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.*;

public class ServerThread extends Thread {

    private final Socket socket;
    private Server server;
    
    //initialize private variables
    public ServerThread(Socket clientSocket, Server server) {
        this.socket = clientSocket;
        this.server = server;
    }
    
    //the main worker function for a ServerThread
    public void run () {
        BufferedReader in = null;
        PrintWriter out = null;
        try {        	
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            //this.sendMessage(out, "Welcome the echo server\n\rType \"bye\" to disconnect\n\r");
            //send welcome to server message here that indicates waiting for additional players
            
            synchronized(this) {
            	server.addClient(out);	
            }
            
            String outputLine = null;
            while (!socket.isClosed()) {
                outputLine = in.readLine();
                
                //quit on empty input
                if (outputLine == null) {
                    break;
                }
                
                //quit on text 'bye'
                if (outputLine.equalsIgnoreCase("bye")) {
                    break;
                } 
                //return input text
                else {
            		server.messageAll("Echo: " + outputLine);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
        	//cleanup created data
            try {
                if (out != null) {
                	synchronized(this) {
                		server.removeClient(out);
                	}
                	
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void sendMessage(PrintWriter out, String message) {
    	 out.println(message);
    }
}
