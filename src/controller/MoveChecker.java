package controller;

import java.util.ArrayList;
import java.util.List;

import objects.*;
import objects.Character;

public class MoveChecker 
{
	/**
	 * Default constructor
	 */
	public MoveChecker()
	{
	}
	
	public List<Location> getAvailableMoves(Player player)
	{
		List<Location> availLoc = new ArrayList<Location>();
		Location currentLoc = player.location;
		
		for( Location l : currentLoc.getConnectedLocations() )
		{
			if( l instanceof Room || (l instanceof Hallway && !((Hallway)l).isOccupied))
				availLoc.add( l );
		}
		
		return availLoc;
	}
	
	/**
	 * Test!!!
	 * @param a
	 */
//	public static void main (String a[])
//	{
//		Room room1 = new Room( Card.BALL.value(), Card.BALL.getName() );
//		Hallway ballKitchen = new Hallway( Card.BALL_KITCHEN.value() );
//		room1.connectedHallways.add( ballKitchen );
//		room1.connectedHallways.add( new Hallway( Card.CONSERVATORY_BALL.value() ) );
//		room1.connectedHallways.add( new Hallway( Card.BILLIARD_BALL.value() ) );
//		
//		Player player1 = new Player();
//		player1.location = room1;
//		
//		Player player2 = new Player();
//		player2.location = new Hallway( Card.CONSERVATORY_BALL.value() );
//		player2.character = new Character( Card.MISS_SCARLET.value() );
//		
//		ballKitchen.addOccupant( player2.character );
//		
//		MoveChecker checker = new MoveChecker();
//		List<Location> loc = checker.getAvailableMoves( player1 );
//		
//		
//		for( Location l : loc )
//		{
//			System.out.println( l.getName() );
//		}
//		
//		Room bill = new Room( Card.BILLIARD.value(), Card.BILLIARD.getName() );
//		Hallway billBall = new Hallway( Card.BILLIARD_BALL.value() );
//		bill.connectedHallways.add( new Hallway( Card.HALL_BILLIARD.value() ) );
//		bill.connectedHallways.add( new Hallway( Card.BILLIARD_DINING.value() ) );
//		bill.connectedHallways.add( new Hallway( Card.LIBRARY_BILLIARD.value() ) );
//		bill.connectedHallways.add( billBall );
//		
//		Player player3 = new Player();
//		player3.location = bill;
//		
//		Player player4 = new Player();
//		player4.character = new Character( Card.MR_GREEN.value() );
//		player4.location = new Hallway( Card.BILLIARD_BALL.value() );
//		billBall.addOccupant( player4.character );
//		
//		loc = checker.getAvailableMoves( player3 );
//		
//		System.out.println("CASE 2");
//		for( Location l : loc )
//		{
//			System.out.println( l.getName() );
//		}
//		
//		Room study = new Room( Card.STUDY.value(), Card.STUDY.getName() );
//		Hallway studyHall = new Hallway( Card.STUDY_HALL.value() );
//		study.connectedHallways.add( new Hallway( Card.STUDY_LIBRARY.value() ) );
//		study.addSecretRoom(new Room( Card.KITCHEN.value(), Card.KITCHEN.getName()));
//			
//		Player player5 = new Player();
//		player5.location = study;
//	
//		Player player6 = new Player();
//		player6.character = new Character( Card.MR_GREEN.value() );
//		player6.location = new Hallway( Card.STUDY_HALL.value() );
//		studyHall.addOccupant( player6.character );
//		
//		loc = checker.getAvailableMoves( player5 );
//		
//		System.out.println("CASE 3");
//		for( Location l : loc )
//		{
//			System.out.println( l.getName() );
//		}	
//	}
}
