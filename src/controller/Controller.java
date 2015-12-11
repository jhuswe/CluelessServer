package controller;

import java.io.*;
import java.net.*;
import objects.*;
import objects.Character; //imported explicitly to prevent ambiguous type error
import java.util.concurrent.ThreadLocalRandom;

import java.util.*;

public class Controller {
    private int clientCount;
    private List<InOut> clients;
    private int allowedPlayers;
    private Map<Integer, Location> locationList;
    private Map<Integer, Player> playerList;
    private Player orderSet;
    private Player currentPlayerPointer;
    private Player disprovePlayerPointer;
    private List<Card> culpritCards;
    private boolean endGame;
    private MoveChecker moveChecker;
    
    //initialize private variables
    public Controller() {
        clientCount = 0;
        clients = new ArrayList<InOut>();
        allowedPlayers = 5; //debug value, real version will be 5
        culpritCards = new ArrayList<Card>();
        locationList = new HashMap<Integer, Location>();
        playerList = new HashMap<Integer, Player>();
        moveChecker = new MoveChecker();
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
        
        //variables for assigning player data
        int playerNum = 1;
        List<Card> rooms = new ArrayList<Card>();
        List<Card> characters = new ArrayList<Card>();
        List<Card> weapons = new ArrayList<Card>();
        List<Card> allCards = new ArrayList<Card>();
        List<List<Card>> hands = new ArrayList<List<Card>>();

        //build list of rooms
        for(int i = 1; i < 10; i++) {
        	rooms.add(Card.getCard(i));
        }
        
        //pull room and put in solution list
        int answerRoom = this.getRandomNumber(0, 8);
        culpritCards.add(Card.getCard(rooms.remove(answerRoom).value()));
        
        //build list of characters
        for (int i = 22; i < 28; i++) {
        	characters.add(Card.getCard(i));
		}
        
        //pull character and put in solution list
        int answerCharacter = this.getRandomNumber(0, 6);
        culpritCards.add(Card.getCard(characters.remove(answerCharacter).value()));
        
        //build list of weapons
        for (int i = 28; i < 34; i++) {
        	weapons.add(Card.getCard(i));
		}

        //pull weapon and put in solution list
        int answerWeapon = this.getRandomNumber(0, 6);
        culpritCards.add(Card.getCard(weapons.remove(answerWeapon).value()));
        
        allCards.addAll(rooms);
        allCards.addAll(characters);
        allCards.addAll(weapons);
        
        int currentDeck = 0;
        
        //intialize hands
        for (int i = 0; i < this.allowedPlayers; i++) {
			List<Card> temp = new ArrayList<Card>();
			hands.add(temp);
		}
        
        while (!allCards.isEmpty()) {
        	//start over at first set of cards
        	if (currentDeck > (allowedPlayers - 1)) {
				currentDeck = 0;
			}
        	
        	int cardIndex;
        	
        	//choose next card to put into hand
        	if (allCards.size() > 2) {
				cardIndex = this.getRandomNumber(0, allCards.size() - 2); //minus two since random number includes end boundary, and zero based list 
			}
        	else {
        		cardIndex = 0;
        	}
			
        	//pull from remaining cards
			Card card = allCards.remove(cardIndex);
			
			//put cards into currentDeck
			hands.get(currentDeck).add(card);
			
			currentDeck++;
		}
        
        int i = 22; //starting character id
        
      //assign characters
        while (playerNum < this.allowedPlayers + 1) {
        	Character character = new Character(i);
        	List<Card> playerHand = hands.get(playerNum - 1);
        	Player player = new Player(character, playerHand);
        	
        	this.logMessage("Player " + String.valueOf(playerNum) + " is " + Card.getCard(i).name());
        	playerNum++;
        	i++;
		}
        
//        Message temp = new Message();
//        temp.action = Action.CHARACTER;
//        temp.character = Card.MISS_SCARLET;
//        
//        String text = MessageBuilder.SerializeMsg(temp);
//        
//        clients.get(0).out.println(text);
    }
    
    //returns list of clients
    public List<InOut> getClients() {
    	return this.clients;
    }
    
    //returns number of clients
    public int getClientCount() {
    	return this.clientCount;
    }
    
    //add a client to the list of clients then notifies clients and server
    public void addClient(InOut client) {
		//this.clientCount++;
    	this.clients.add(client);
    	this.logMessage("Client connected. Total Clients => " + this.getClientCount());
    	//this.sendMsg("Client connected\n\rThere are " + this.getClientCount() + " clients connected\n\r");
    }
    
    //removes a client from the list of clients then notifies clients and server
    public void removeClient(PrintWriter client) {
    	this.clientCount--;
    	this.clients.remove(client);
    	this.logMessage("Client disconnected. Total Clients => " + this.clientCount);
    	//this.sendMsg("Client disconnected\n\rThere are " + this.getClientCount() + " clients connected\n\r");
    }
    
    //wrapper for printing messages to the console
    public void logMessage(String message) {
    	System.out.println(message);
    }
    
    //send a message to all connected clients
    public void sendMsg(Message message) {
    	String jsonText = MessageBuilder.SerializeMsg(message);
    	
    	for(InOut client : this.clients) {
        	client.out.println(jsonText);
        }
    }
    
    //convert jsonText to Message object
    public Message recvMsg(String jsonText) {
    	Message message = (Message) MessageBuilder.DeserializeMsg(jsonText);
    	
    	return message;
    }
    
    private void setCurrentPlayerPointer(Player player) {
    	this.currentPlayerPointer = player;
    }
    
    private void setDisprovePlayerPointer(Player player) {
    	this.disprovePlayerPointer = player;
    }
    
    private void setCulpritCards(List<Card> cards) {
    	this.culpritCards = cards;
    }
    
    private List<Location> getMoveCheckerResult(Player player) {
    	return this.moveChecker.getAvailableMoves(player);
    }
    
    private void updateLocationList(Location location) {
    	locationList.put(location.getId(), location);
    }
    
    private void updatePlayerList(Player player) {
    	playerList.put(player.getId(), player);
    }
    
    private int getRandomNumber(int min, int max) {
    	return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
}
