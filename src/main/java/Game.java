/* Game.java (aka Quoridor) - CIS 405 - teams
 * Last Edit: March 29, 2015
 * ____________________________________________________________________________
 * 
 * implements the GameEngine and Messenger to create and run the game Quoridor
 */

import java.util.*;

public class Game {
    
    private static int numPlayers;     // how many players are in the game
    private static final int WALL_POOL = 20; // total collection of walls
    private static Queue<Player> players = new LinkedList<Player>(); // players
    
    /**
     * prints a friendly message and exits
     * @param an int to return to the OS
     */
    public static void usage(int error) {
        System.err.println("usage: java Game <host> <port> <host> <port> " + 
                           "[<host> <port> <host> <port>]");
        System.exit(error);
    }

    /**
     * sleepy time
     * @param length duration for thread to pause
     */
    private static void sleep(int length) {
        try {
            Thread.sleep(length);
        } catch (InterruptedException e) {
            // ignore it
        }
    }

    public static void main (String[] args) {
        // initialize debug stream
        Deb.initialize("game");

        // quit if bad arguments
        Deb.ug.println("args provided: " + Arrays.toString(args));
        
        if (args.length !=  4 && args.length != 8) {
            usage(1);
        }

        // Set the number of players
        numPlayers = args.length/2;
        Deb.ug.println("number of players: " + numPlayers);

        // Connect to players
        Messenger hermes = new Messenger(args);

        // Instantiate Players
        Deb.ug.println("instantiating Players...");
        for ( int i = 0; i < numPlayers; i++ )
            players.add(new Player(i, WALL_POOL / numPlayers));

        // Instantiate GameBoard
        Deb.ug.println("instantiating GameBoard...");
        GameBoard board = new GameBoard(players);
        Deb.ug.println("players array: " + Arrays.toString(players.toArray()));

        // tell all move servers who the players are
        hermes.broadcastPlayers(players.toArray(new Player[players.size()]));

        // Start up the display
        Deb.ug.println("starting GameBoardFrame...");
        GameBoardFrame frame = new GameBoardFrame(board);

        // loop will need to check for a victory condition
        Deb.ug.println("beginning main loop");
        while (true) {
            // Get current player
            Player currentPlayer = players.peek();

            // Get move from player
            Deb.ug.println("requesting move from player: " + 
                           currentPlayer.getName());
            String response = hermes.requestMove(currentPlayer);
            Deb.ug.println("received: " + response);

            // Validate if the move is legal and make the move on the board
            // else boot the player for trying to make an illegal move
            if ( GameEngine.validateMove ( board,currentPlayer,response ) ) {
                Deb.ug.println("legal move");
                Square destination = GameEngine.getSquare(board,response);
                board.move(currentPlayer, destination);
                hermes.broadcastWent(currentPlayer, response);
            } else {
                Deb.ug.println("illegal move attempted");
                board.removePlayer(currentPlayer);
                players.remove();
                hermes.broadcastBoot(currentPlayer);
            }
        
            // Update the graphical board
            frame.update(board);

            // Retrieve a possibly winning player and broadcast if winner found
            Player winner = GameEngine.getWinner(board, players);
            if (winner != null) {
                hermes.broadcastVictor(winner);
                break;
            }
             
            // Shuffle queue
            players.add(players.remove());

            sleep(200); // sleepy time

        }//-----END OF LOOP-----
   
        hermes.closeAllStreams(players.toArray(new Player[players.size()]));
        
        // pause board for two seconds before ending
        sleep(2000);
        System.exit(0);
    }
}
