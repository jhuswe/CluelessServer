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
        allowedPlayers = 1; //debug value, real version will be 5
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
        
        //no more players allowed to join
        serverSocket.close();
        
        //variables for assigning player data
        int playerNum = 1;
        List<Card> rooms = new ArrayList<Card>();
        List<Card> characters = new ArrayList<Card>();
        List<Card> weapons = new ArrayList<Card>();
        List<Card> allCards = new ArrayList<Card>();
        List<List<Card>> hands = new ArrayList<List<Card>>();
        //build list of all starting locations
        List<Location> currentLocations = this.getAllInitialLocations();
        
        this.logMessage("Created starting location information");

        //build list of rooms
        for(int i = 1; i < 10; i++) {
        	rooms.add(Card.getCard(i));
        }
        
        this.logMessage("Built list of rooms");
        
        //pull room and put in solution list
        int answerRoom = this.getRandomNumber(0, 8);
        culpritCards.add(Card.getCard(rooms.remove(answerRoom).value()));
        
        this.logMessage("Added room to the solution list");
        
        //build list of characters
        for (int i = 22; i < 28; i++) {
        	characters.add(Card.getCard(i));
		}
        
        this.logMessage("Built list of characters");
        
        //pull character and put in solution list
        int answerCharacter = this.getRandomNumber(0, 5);
        culpritCards.add(Card.getCard(characters.remove(answerCharacter).value()));
        
        this.logMessage("Added character to the solution list");
        
        //build list of weapons
        for (int i = 28; i < 34; i++) {
        	weapons.add(Card.getCard(i));
		}
        
        this.logMessage("Built list of weapons");

        //pull weapon and put in solution list
        int answerWeapon = this.getRandomNumber(0, 5);
        culpritCards.add(Card.getCard(weapons.remove(answerWeapon).value()));
        
        this.logMessage("Added weapon to the solution list");
        this.logMessage("Solution list complete");
        
        //pull all remaining cards in one list for easier assignment
        allCards.addAll(rooms);
        allCards.addAll(characters);
        allCards.addAll(weapons);
        
        //start putting cards into the first list
        int currentDeck = 0;
        
        //initialize hands
        for (int i = 0; i < this.allowedPlayers; i++) {
			List<Card> temp = new ArrayList<Card>();
			hands.add(temp);
		}
        
        //assign cards until none are left
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
        		cardIndex = 0; //when cards get low offset causes an error just use zero for now
        	}
			
        	//pull from remaining cards
			Card card = allCards.remove(cardIndex);
			
			//put cards into currentDeck
			hands.get(currentDeck).add(card);
			
			currentDeck++;
		}
        
        this.logMessage("Created " + this.allowedPlayers + " piles of cards");
        
        int i = 22; //starting character id
        
      //assign characters
        while (playerNum < this.allowedPlayers + 1) {
        	//get character associated with id
        	Character character = new Character(i);
        	//get a pre built hand of cards for this player
        	List<Card> playerHand = hands.get(playerNum - 1);
        	
        	//create player with assigned character and hand of cards
        	Player player = new Player(character, playerHand);

        	//assign associated starting position to character
        	player.location = this.getInitialLocation(player.character);
        	
        	//assign players to clients in the order they arrived
        	this.clients.get(playerNum - 1).player = player;
        	
        	//build message for client
        	Message message = new Message();
        	message.action = Action.INITIATE_CHARACTER;
        	message.player = player;
        	message.playerLocations = currentLocations;
        	
        	//notify client of assigned player
        	this.sendMsg(message, this.clients.get(playerNum - 1).out); 
        	
        	this.logMessage("Player " + String.valueOf(playerNum) + " is " + Card.getCard(i).name());
        	playerNum++;
        	i++;
		}
        
        int turnMinValue = Card.MISS_SCARLET.value(); //lowest allowed value for player turn
        int currentPlayerNum = turnMinValue; 
        int turnMaxValue = turnMinValue + this.allowedPlayers; //highest allowed value for player turn
        
        while (!endGame) {
        	//if we have reached the last player start the cyle again
			if (currentPlayerNum > turnMaxValue) {
				currentPlayerNum = turnMinValue;
			}
        	
			this.logMessage("Starting to build message");
			
			//build message for current player
        	InOut currentPlayerData = this.getClient(currentPlayerNum);
			Message yourTurn = new Message();
        	yourTurn.action = Action.MOVE;
        	yourTurn.player = currentPlayerData.player;
        	yourTurn.availableMoves = this.getMoveCheckerResult(yourTurn.player);
        	yourTurn.playerLocations = currentLocations;
        	
        	this.logMessage("Finished building message");
        	
        	//send message to all clients
        	this.sendMsgToAll(yourTurn);
        	
        	//receive message from current player
        	Message playersChoice = this.recvMsg(currentPlayerData.in);
        	
        	if (playersChoice.action.value() == Action.MOVE.value()) {
				
			}
        	else if (playersChoice.action.value() == Action.MAKE_SUGGESTION.value()) {
				
			}
        	else if (playersChoice.action.value() == Action.DISPROVE.value()) {
				
			}
        	else if (playersChoice.action.value() == Action.ACCUSATION.value()) {
				
			}
        	else if (playersChoice.action.value() == Action.RECEIVE_DISPROVE_CARD.value()) {
				
			}
        	
			currentPlayerNum++;
		}
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
    public void sendMsgToAll(Message message) {
    	String jsonText = MessageBuilder.SerializeMsg(message);
    	
    	this.logMessage("Starting to message all clients");
    	
    	for(InOut client : this.clients) {
        	client.out.println(jsonText);
        }
    	
    	this.logMessage("Finshed messaging all clients");
    }
    
    //send a message to a single client
    public void sendMsg(Message message, PrintWriter out) {
    	String jsonText = MessageBuilder.SerializeMsg(message);
    	
    	this.logMessage("Starting to message a single client");
    	
    	out.println(jsonText);
    	
    	this.logMessage("Finished messaging a single client");
    }
    
    //convert jsonText to Message object
    public Message recvMsg(BufferedReader in) {
    	Message message = null;

    	this.logMessage("Starting to receive from client");
    	
    	try {
			String jsonText = in.readLine();
			message = (Message) MessageBuilder.DeserializeMsg(jsonText);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	this.logMessage("Finished recieving a message from the client");
    	
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
    
    private Location getInitialLocation(Character character) {
    	Location location;
    	Card card = Card.getCard(character.getId());

    	switch (card) {
		case MISS_SCARLET:
			location = new Room(Card.HALL_LOUNGE.value());
			break;
		case COL_MUSTARD:
			location = new Room(Card.LOUNGE_DINING.value());
			break;
		case MRS_WHITE:
			location = new Room(Card.BALL_KITCHEN.value());
			break;
		case MR_GREEN:
			location = new Room(Card.CONSERVATORY_BALL.value());
			break;
		case MRS_PEACOCK:
			location = new Room(Card.LIBRARY_CONSERVATORY.value());
			break;
		case PROF_PLUM:
			location = new Room(Card.STUDY_LIBRARY.value());
			break;
		default:
			location = null;
			break;
		}
    	
    	return location;
    }
    
    private List<Location> getAllInitialLocations() {
    	List<Location> playerLocations = new ArrayList<Location>();
    	
    	for (int i = 22; i < 28; i++) {
    		Character character = new Character(i);
    		playerLocations.add(this.getInitialLocation(character));
		}
    	
    	return playerLocations;
    }
    
    //get client data associated with id
    private InOut getClient(int id) {
    	InOut client = null;
    	
    	for (InOut inOut : this.clients) {
			if (inOut.player.character.getId() == id) {
				client = inOut;
			}
		}
    	
    	return client;
    }
}
