package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import objects.InOut;

import java.util.*;

public class ControllerThread extends Thread {

    private final Socket socket;
    private Controller server;
    
    //initialize private variables
    public ControllerThread(Socket clientSocket, Controller server) {
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
            
            InOut thisClient = new InOut(in, out);
            
            //this.sendMessage(out, "Welcome the echo server\n\rType \"bye\" to disconnect\n\r");
            //send welcome to server message here that indicates waiting for additional players
            
            synchronized(this) {
            	server.addClient(thisClient);	
            }
            
            String outputLine = null;
            while (!socket.isClosed()) {
                outputLine = thisClient.in.readLine();
                
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
            		//server.sendMsg("Echo: " + outputLine);
                	//thisClient.out.println("Echo: " + outputLine);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ControllerThread.class.getName()).log(Level.SEVERE, null, ex);
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
                Logger.getLogger(ControllerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    private void sendMessage(PrintWriter out, String message) {
    	 out.println(message);
    }
}
