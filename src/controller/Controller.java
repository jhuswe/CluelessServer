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
    private List<Location> currentLocations;
    
    //initialize private variables
    public Controller() {
        clientCount = 0;
        clients = new ArrayList<InOut>();
        allowedPlayers = 2; //debug value, real version will be 5
        culpritCards = new ArrayList<Card>();
        locationList = new HashMap<Integer, Location>();
        playerList = new HashMap<Integer, Player>();
        moveChecker = new MoveChecker();
        currentLocations = new ArrayList<Location>();
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
        currentLocations = this.getAllInitialLocations();
        
        this.logMessage("Created starting location information");

        //build list of rooms
        for(int i = 1; i < 10; i++) {
        	rooms.add(Card.getCard(i));
        }
        
        this.logMessage("Built list of rooms");
        
        //pull room and put in solution list
        int answerRoom = this.getRandomNumber(0, 8);
        culpritCards.add(Card.getCard(rooms.remove(answerRoom).value()));
        
        this.logMessage("Added room card to the solution list");
        
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
        	player.location = this.getPlayerLocation(player.character);
        	
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
        		Player player = playersChoice.player;
				Location newLocation = this.moveCharacter(player);
				
				if (this.isRoom(newLocation)) {
					yourTurn.action = Action.MAKE_SUGGESTION;
					this.sendMsgToAll(yourTurn);
					
					playersChoice = this.recvMsg(currentPlayerData.in);
					
					if (playersChoice.action.value() == Action.MAKE_SUGGESTION.value()) {
//						List<Integer> guess = playersChoice.SDAInfo;
//						boolean suggestionDisproved = this.promptForDisproval(currentPlayerData, guess);
						this.processSuggestion(playersChoice, currentPlayerData);
					}
					else if (playersChoice.action.value() == Action.ACCUSATION.value()) {
						this.processAccusation(playersChoice);
//						List<Integer> accusation = playersChoice.SDAInfo;
//						boolean isCorrect = this.checkAccusation(accusation);
//						
//						if (isCorrect) {
//							Message winMessage = new Message();
//							
//							winMessage.action = Action.WIN;
//							winMessage.player = playersChoice.player;
//							winMessage.SDAInfo = accusation;
//							
//							this.sendMsgToAll(winMessage);
//							
//							this.endGame = true;
//						}
//						else {
//							Message loseMessage = new Message();
//							
//							loseMessage.action = Action.LOSE;
//							playersChoice.player.isOutOfGame = true;
//							loseMessage.player = playersChoice.player;
//							loseMessage.SDAInfo	 = accusation;
//
//							this.sendMsgToAll(loseMessage);
//						}
					}
				}
			}
//        	else if (playersChoice.action.value() == Action.DISPROVE.value()) { 
//				
//			}
        	else if (playersChoice.action.value() == Action.MAKE_SUGGESTION.value()) {
				this.processSuggestion(playersChoice, currentPlayerData);
			}
        	else if (playersChoice.action.value() == Action.ACCUSATION.value()) {
				this.processAccusation(playersChoice);
			}
//        	else if (playersChoice.action.value() == Action.RECEIVE_DISPROVE_CARD.value()) {
//				
//			}
        	
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
    
    private Location getPlayerLocation(Character character) {
    	Location characterLocation = null;

    	//does not stop searching when found, optimize this
    	for (Location location : this.currentLocations) {
			for (Character searchCharacter : location.getOccupants()) {
				if (character.getId() == searchCharacter.getId()) {
					characterLocation = location;
				}
			}
		}
    	
    	return characterLocation;
    }
    
    private List<Location> getAllInitialLocations() {
    	List<Location> locations = new ArrayList<Location>();
    	
    	//create all rooms
    	Room study = new Room(Card.STUDY.value());
    	Room hall = new Room(Card.HALL.value());
    	Room lounge = new Room(Card.LOUNGE.value());
    	Room library = new Room(Card.LIBRARY.value());
    	Room billiardRoom = new Room(Card.BILLIARD.value());
    	Room diningRoom = new Room(Card.DINING.value());
    	Room conservatory = new Room(Card.CONSERVATORY.value());
    	Room ballRoom = new Room(Card.BALL.value());
    	Room kitchen = new Room(Card.KITCHEN.value());
    	
    	//create all hallways
    	Hallway studyToHall = new Hallway(Card.STUDY_HALL.value());
    	Hallway hallToLounge = new Hallway(Card.HALL_LOUNGE.value());
    	Hallway studyToLibrary = new Hallway(Card.STUDY_LIBRARY.value());
    	Hallway hallToBilliard = new Hallway(Card.HALL_BILLIARD.value());
    	Hallway loungeToDining = new Hallway(Card.LOUNGE_DINING.value());
    	Hallway libraryToBilliard = new Hallway(Card.LIBRARY_BILLIARD.value());
    	Hallway billiardToDining = new Hallway(Card.BILLIARD_DINING.value());
    	Hallway libraryToConservatory = new Hallway(Card.LIBRARY_CONSERVATORY.value());
    	Hallway billiardToBall = new Hallway(Card.BILLIARD_BALL.value());
    	Hallway diningToKitchen = new Hallway(Card.DINING_KITCHEN.value());
    	Hallway conservatoryToBall = new Hallway(Card.CONSERVATORY_BALL.value());
    	Hallway ballToKitchen = new Hallway(Card.BALL_KITCHEN.value());
    	
    	//place characters in hallways
    	hallToLounge.addOccupant( new Character( Card.MISS_SCARLET.value() ) );
    	loungeToDining.addOccupant( new Character( Card.COL_MUSTARD.value() ) );
    	ballToKitchen.addOccupant( new Character( Card.MRS_WHITE.value() ) );
    	conservatoryToBall.addOccupant( new Character( Card.MR_GREEN.value() ) );
    	libraryToConservatory.addOccupant( new Character( Card.MRS_PEACOCK.value() ) );
    	studyToLibrary.addOccupant( new Character( Card.PROF_PLUM.value() ) );
    	
    	//connect study to its hallways and secret room (kitchen)
    	this.connectRoomToHallway(study, studyToHall);
    	this.connectRoomToHallway(study, studyToLibrary);
    	this.connectRoomToRoom(study, kitchen);
    	
    	//connect hall to its hallways
    	this.connectRoomToHallway(hall, studyToHall);
    	this.connectRoomToHallway(hall, hallToLounge);
    	this.connectRoomToHallway(hall, hallToBilliard);
    	
    	//connect lounge to its hallways and secret room (conservatory)
    	this.connectRoomToHallway(lounge, hallToLounge);
    	this.connectRoomToHallway(lounge, loungeToDining);
    	this.connectRoomToRoom(lounge, conservatory);
    	
    	//connect library to its hallways
    	this.connectRoomToHallway(library, studyToLibrary);
    	this.connectRoomToHallway(library, libraryToBilliard);
    	this.connectRoomToHallway(library, libraryToConservatory);
    	
    	//connect billiard room to its hallways
    	this.connectRoomToHallway(billiardRoom, hallToBilliard);
    	this.connectRoomToHallway(billiardRoom, libraryToBilliard);
    	this.connectRoomToHallway(billiardRoom, billiardToDining);
    	this.connectRoomToHallway(billiardRoom, billiardToBall);
    	
    	//connect dining room to its hallways
    	this.connectRoomToHallway(diningRoom, loungeToDining);
    	this.connectRoomToHallway(diningRoom, billiardToDining);
    	this.connectRoomToHallway(diningRoom, diningToKitchen);
    	
    	//connect conservatory to its hallways (already connected to its secret room)
    	this.connectRoomToHallway(conservatory, libraryToConservatory);
    	this.connectRoomToHallway(conservatory, conservatoryToBall);
    	
    	//connect ballroom to its hallways
    	this.connectRoomToHallway(ballRoom, conservatoryToBall);
    	this.connectRoomToHallway(ballRoom, billiardToBall);
    	this.connectRoomToHallway(ballRoom, ballToKitchen);
    	
    	//connect kitchen to its hallways (already connected to its secret room)
    	this.connectRoomToHallway(kitchen, ballToKitchen);
    	this.connectRoomToHallway(kitchen, diningToKitchen);
    	
    	//put all rooms in list
    	locations.add(study);
    	locations.add(hall);
    	locations.add(lounge);
    	locations.add(library);
    	locations.add(billiardRoom);
    	locations.add(diningRoom);
    	locations.add(conservatory);
    	locations.add(ballRoom);
    	locations.add(kitchen);
    	
    	//put all hallways in list
    	locations.add(studyToHall);
    	locations.add(hallToLounge);
    	locations.add(studyToLibrary);
    	locations.add(hallToBilliard);
    	locations.add(loungeToDining);
    	locations.add(libraryToBilliard);
    	locations.add(billiardToDining);
    	locations.add(libraryToConservatory);
    	locations.add(billiardToBall);
    	locations.add(diningToKitchen);
    	locations.add(conservatoryToBall);
    	locations.add(ballToKitchen);
    	
    	return locations;
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
    
    //connect a room and hallway to one another
    private void connectRoomToHallway(Room room, Hallway hallway) {
    	room.addHallway(hallway);
    	hallway.addConnectedRoom(room);
    }
    
    //connect a room and a room together
    private void connectRoomToRoom(Room room1, Room room2) {
    	room1.addSecretRoom(room2);
    	room2.addSecretRoom(room1);
    }
    
    //move player from current location 
    private Location  moveCharacter(Player player) {
    	Location oldLocation = this.getPlayerLocation(player.character);
    	Location newLocation = player.location;
    	
    	oldLocation.removeOccupant(player.character);
    	newLocation.addOccupant(player.character);
    	
    	return newLocation;
    }
    
    //returns true if location is a room, false otherwise
    private boolean isRoom(Location location) {
    	boolean isRoom = false;
    	
    	if (location.getId() >= 1 && location.getId() <= 9) {
			isRoom = true;
		}
    	
    	return isRoom;
    }
    
    //prompts all players except one who made suggestion
    private boolean promptForDisproval(InOut currentClient, List<Integer> guess) {
    	boolean cardShown = false;
    	
    	Message message = new Message();
    	message.action = Action.DISPROVE;
    	message.SDAInfo = guess;
    	
    	for (InOut client : this.clients) {
			if (client.player.getId() != currentClient.player.getId()) {
				message.player = client.player;
				this.sendMsgToAll(message);
				
				Message playersChoice = this.recvMsg(client.in);
				
				if (playersChoice.character != null) {
					cardShown = true;
					Message disprovalMessage = new Message();
					
					disprovalMessage.action = Action.RECEIVE_DISPROVE_CARD;
					disprovalMessage.player = currentClient.player;
					disprovalMessage.character = playersChoice.character;
							
					this.sendMsgToAll(message);
				}
			}
		}
    	
    	return cardShown;
    }
    
    //checks an accusation returns true if correct, false if not
    private boolean checkAccusation(List<Integer> accusation) {
    	boolean isCorrect = true;
    	
    	//when we find a card that isnt correct the accusation is wrong
    	for (Integer cardId : accusation) {
			if (! culpritCards.contains( Card.getCard(cardId) )) {
				isCorrect = false;
			}
		}
    	
    	return isCorrect;
    }
    
    private void processSuggestion(Message playersChoice, InOut currentPlayerData) {
		List<Integer> guess = playersChoice.SDAInfo;
		boolean suggestionDisproved = this.promptForDisproval(currentPlayerData, guess);
    }
    
    private void processAccusation(Message playersChoice) {
    	List<Integer> accusation = playersChoice.SDAInfo;
		boolean isCorrect = this.checkAccusation(accusation);
		
		if (isCorrect) {
			Message winMessage = new Message();
			
			winMessage.action = Action.WIN;
			winMessage.player = playersChoice.player;
			winMessage.SDAInfo = accusation;
			
			this.sendMsgToAll(winMessage);
			
			this.endGame = true;
		}
		else {
			Message loseMessage = new Message();
			
			loseMessage.action = Action.LOSE;
			playersChoice.player.isOutOfGame = true;
			loseMessage.player = playersChoice.player;
			loseMessage.SDAInfo	 = accusation;

			this.sendMsgToAll(loseMessage);
		}
    }
}
