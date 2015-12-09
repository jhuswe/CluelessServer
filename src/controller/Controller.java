package controller;

import java.io.*;
import java.net.*;
import objects.Hallway;
import java.util.*;

public class Controller {
    private int clientCount;
    private List<PrintWriter> clients;
    private int allowedPlayers;
    
    //initialize private variables
    public Controller() {
        clientCount = 0;
        clients = new ArrayList<PrintWriter>();
        allowedPlayers = 2; //debug value, real version will be 5
    }
    
    //the starting point of the application
    public static void main(String[] args) {
    	Controller server = new Controller();
    	
    	try {
			server.listen();
		} catch (IOException e) {
			server.logMessage(e.toString());
		}
    }
    
    //listens for and processes new connections
    public void listen() throws IOException {
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(8889);
            this.logMessage("Listening on port: 8889");
        } catch (IOException e) {
            this.logMessage("Could not listen on port: 8889.");
            System.exit(1);
        }

        Socket clientSocket = null;
        while (this.getClientCount() < this.allowedPlayers) {
                clientSocket = serverSocket.accept();
                
                synchronized(this) {
            		this.clientCount++;
            	}
                
                ControllerThread thread = new ControllerThread(clientSocket, this);
                thread.start();
                this.logMessage("New server thread started");
        }
        
        serverSocket.close();
        
        //main game loop goes here?
    }
    
    //returns list of clients
    public List<PrintWriter> getClients() {
    	return this.clients;
    }
    
    //returns number of clients
    public int getClientCount() {
    	return this.clientCount;
    }
    
    //add a client to the list of clients then notifies clients and server
    public void addClient(PrintWriter client) {
		//this.clientCount++;
    	this.clients.add(client);
    	this.logMessage("Client connected. Total Clients => " + this.getClientCount());
    	this.messageAll("Client connected\n\rThere are " + this.getClientCount() + " clients connected\n\r");
    }
    
    //removes a client from the list of clients then notifies clients and server
    public void removeClient(PrintWriter client) {
    	this.clientCount--;
    	this.clients.remove(client);
    	this.logMessage("Client disconnected. Total Clients => " + this.clientCount);
    	this.messageAll("Client disconnected\n\rThere are " + this.getClientCount() + " clients connected\n\r");
    }
    
    //wrapper for printing messages to the console
    public void logMessage(String message) {
    	System.out.println(message);
    }
    
    //send a message to all connected clients
    public void messageAll(String message) {
    	for(PrintWriter out : this.clients) {
        	out.println(message);
        }
    }
}
