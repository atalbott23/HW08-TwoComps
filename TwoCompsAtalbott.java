package hw08;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

public class TwoCompsAtalbott extends Application
{
	Text aText;
	Text[][] mark;
	String myName; public String getMyName(){ return myName; }
	String yourName;
	boolean myTurn;
	boolean startTurn;
	VBox root; // the whole window
	Scene scene;
	Ear oe; // listens for what the other end says
	ServerSocket serverSock; // allows client to connect
	Socket clientSock; // this is the connection that BOTH server
	String ip; // the IP number of the server
	int socketNumber = 12457; // random number for this app
	BufferedReader myIn=null; // how we read from other end
	PrintWriter myOut=null; // how we write to the other end
	Pane gamePane; // upper part of window, shows conversation
	//TextArea conversation; // displays everything that has been said
	//String theText; // the raw text of all that has been said
	TextField talker; // where user types a new thing to say

	HBox controlPane; // has buttons to get started


	boolean gameOver;
	boolean[][] hostShip;//Tracks the current users ship location
	boolean[][] clientShip;//Tracks the opponents ship
	boolean[][] shotsFired;//Tracks all shots fired in the past
	int shipCount;//Makes sure the ship is a size of four
	int hitCount;//Counts the hit count to end the game
	public static void main( String[] args )
	{ launch(args); }

	public void start( Stage stage )
	{
		root = new VBox();
		scene = new Scene(root, 800, 800);
		stage.setTitle("Battle Ship");
		stage.setScene(scene);
		stage.show();

		gamePane = new Pane();
		gamePane.setPrefSize( 625 ,625 );
		root.getChildren().add(gamePane);

		controlPane = new HBox();
		root.getChildren().add( controlPane );

		mark = new Text[6][6];//initilizes the mark placements
		hostShip = new boolean[6][6];//initilizes the host ship
		clientShip = new boolean[6][6];//initilizes the opponents ships
		shotsFired = new boolean[6][6];//initilizes the shots fired
		startTurn = false; //Keeps track so the users can initialze the ships before shooting the first shot
		shipCount =0; //Tracks the ship size
		hitCount = 0;//Tracks the hits
		gameOver=false;
		setGameBoard();//Set up a game board functon to clean up code a bit

		// host button
		Button hostButton = new Button("Host");
		controlPane.getChildren().add(hostButton);
		hostButton.setOnAction
		(e->{ startTHISend(); new SetupHost().start(); });

		// client Button
		// When you press it, it puts up a field to enter the IP
		// number.  And when you enter that, then it calls
		// setupClient();
		Button clientButton = new Button("Client");
		controlPane.getChildren().add(clientButton);
		clientButton.setOnAction
		( e->
		{
			controlPane.getChildren().clear();
			Label ipLabel = new Label("IP#?");
			controlPane.getChildren().add(ipLabel);

			TextField iptf = new TextField("localhost");
			controlPane.getChildren().add(iptf);
			iptf.setOnAction
			( f-> { ip = iptf.getText(); setupClient(); } );
		}
				);

		// set it so that when you close the application window,
		// it exits BOTH this end and the other end
		stage.setOnCloseRequest
		( (WindowEvent w) -> 
		{ try{ send("byebyebye"); System.exit(0); } 
		catch (Exception e) { System.out.println("can't stop"); } 
		} 
				);

	}
	//Sets up the initial game board
	public void setGameBoard() 
	{
		Font font = Font.font("Verdana", FontWeight.EXTRA_BOLD, 25);
		Rectangle bg = new Rectangle(0,20,625,625);//Sets up the background
		bg.setFill( Color.BLACK);
		gamePane.getChildren().add(bg);
		//Tells each array about where each block is on the board to prevent overuse of on click buttons and make more effiecnt
		for (int i=0; i<6; i++ )
		{
			for ( int j=0; j<6; j++ )
			{
				double x = i*100+15;
				double y = j*100+35;
				Rectangle r = new Rectangle(x, y, 90, 90 );
				r.setFill(Color.PINK);
				gamePane.getChildren().add(r);
				Text t = new Text(); t.setFont(font);
				mark[i][j] = t; t.setX(x+30); t.setY(y+50);
				hostShip[i][j] =false;
				clientShip[i][j] =false;
				shotsFired[i][j] = false;
				gamePane.getChildren().add(t);
			}
		}
		gamePane.addEventHandler
		(  MouseEvent.MOUSE_CLICKED, 
				(MouseEvent m)->
		{               
			int i = (int)((m.getX()-15)/100);
			int j = (int)((m.getY()-35)/100);
			System.out.println("click at x="+m.getX()+" y="+m.getY()
			+" i="+i+" j="+j
					);
			if(!gameOver)
			{
				//Allows for the initial set up to not disturb the game play
				if(!startTurn)
				{
					//To ensure a player doesnt mark the sam location twice
					if(hostShip[i][j] != true)
					{
						setShip(i,j,"S");
						send("ship "+i+" "+j );//Sends the cordinates to the opponents without reveling where they are.
						shipCount++;
					}
					if(shipCount == 4)
					{
						startTurn =true;//Starts the game when the ship is placed
					}

				}
				else
				{
					if( myTurn )
					{
						if(shotsFired[i][j])//Ensures a spot thats been fired on is not fired on again
						{
							myTurn = true;
							System.out.println("Space is already taken try again");
						}
						else if(clientShip[i][j] == true)
						{
							marker( i, j, "X" );//Mark it with hit if a ship is hit
							send("play "+i+" "+j );//Tells the other player the ship was hit
							hitCount++;//Incriments the hit count
							if(hitCount == 4)
							{
								endGame();//Ends game when ship is sunk
							}
							shotsFired[i][j] = true;//Changes spot to fired upon
							myTurn = false;//Ends turn 
						}
						else
						{
							marker( i, j, "O" );//Marks the spot as a miss
							send("play "+i+" "+j );//Tells the other player it was a miss
							shotsFired[i][j] = true;//Changes spot to fired upon
							myTurn = false; //Ends the turn
						}
					}
				}
			}

		}
				);

		aText = new Text("Welcome to Battle Ship, connect your game and set up your ships");//Initializes the message board
		aText.setX(650); aText.setY(100);
		gamePane.getChildren().add(aText);
	}

	// put s in square i,j 
	public void marker( int i, int j, String s )
	{
		System.out.println(myName+" i="+i+" j="+j+" s="+s);
		mark[i][j].setText(s);

	}
	//shows the user where their own ship is
	public void setShip( int i, int j, String s )
	{
		mark[i][j].setText(s);
		hostShip[i][j] = true;
	}

	//Ends the game
	public void endGame()
	{
		say("Game Over! The battle ship has been sunk!");
		gameOver = true;
		myTurn = false;
		send("GameOver");
	}

	public void setOpShip( int i, int j, String s )
	{
		//		System.out.println(myName+" i="+i+" j="+j+" s="+s);
		clientShip[i][j] = true;


	}


	public class SetupHost extends Thread
	{
		// sets up this to be the first player / host ...
		// First player opens a socket and announces the
		// IP and number, then waits (hangs) until 2nd connects.  
		@Override
		public void run()
		{
			myName = "Host";
			yourName = "Challenger";
			myTurn = true;
			try
			{
				serverSock = new ServerSocket(socketNumber);
				//InetAddress ad = serverSock.getInetAddress();
				//System.out.println(ad); just prints 0s
				say("socket is open, number="+socketNumber);

				// wait for client to make the connection ...
				// the next line hangs until client connects
				clientSock = serverSock.accept(); 
				say("server says client connected ...");

				// once connected set up i/o, do handshake.
				// handshake is: server reads one line from client, 
				// then sends one line to client.
				InputStream in = clientSock.getInputStream();
				myIn = new BufferedReader( new InputStreamReader(in));
				String msg = myIn.readLine();
				say("just read="+msg);
				//
				myOut = new PrintWriter( clientSock.getOutputStream(),true);
				String msg2 = "you rang?";
				say("about to write to client= "+msg2);
				myOut.println(msg2);
				myOut.flush();
				say("just tried to write to client ....");
				say("Here is what we wrote: "+msg2);

				// start the Ear thread, which listens for messages
				// from the other.
				oe = new Ear();
				oe.start();     
			}
			catch(Exception e) 
			{ System.out.println("socket open error e="+e); }
		}
	}

	// sets up this to be the client, which logs into
	// the host. ...
	public void setupClient()
	{
		say("client setup: starting ...");
		myName = "Challenger";
		yourName = "Host";
		myTurn = false;
		try
		{
			say("about to try to call "+ip+"/"+socketNumber);

			// connect to server.  Use ip="localhost" for server
			// on the same machine (for testing)
			clientSock = new Socket(ip,socketNumber);

			say("if you see this, client is connected!");
			InputStream in = clientSock.getInputStream();
			myIn = new BufferedReader( new InputStreamReader(in) );
			myOut = new PrintWriter( clientSock.getOutputStream(),true);
			say("about to greet the server");
			myOut.println("greetings");    
			myOut.flush();
			say("now listening for server reply");
			String line;
			line = myIn.readLine();
			say("read from server = "+line);

			startTHISend();

			// start the Ear thread, which listens for messages
			// from the other end.
			oe = new Ear();
			oe.start();     

		}
		catch( Exception e )
		{ say("client setup error e="+e); }
	}

	// put a place for
	// the user to type in the controlPane and connect
	// it to the conversation window
	public void startTHISend()
	{
		controlPane.getChildren().clear();
		talker = new TextField();
		controlPane.getChildren().add(talker);
		talker.setOnAction
		( g-> { String s = talker.getText();
		say( "(me): " +s ); 
		send(s);
		talker.setText(""); 
		} 
				);
	}

	// Ear is the thread that listens for information coming
	// from the other user.  Go into a loop reading
	// whatever they send and add it to the conversation.
	// If the other end sends "byebyebye", exit this app.
	public class Ear extends Thread
	{
		@Override
		public void run()
		{
			while (true)
			{
				try
				{
					String s = myIn.readLine(); // hangs for input
					say( "(you): "+ s );
					if ( s.equals("byebyebye") ) { System.exit(0); }
					else
					{
						try
						{
							StringTokenizer st = new StringTokenizer(s);
							String cmd = st.nextToken();
							System.out.println("cmd="+cmd);
							if ( cmd.equals("play") ) //Shows hits and misses
							{
								int i = Integer.parseInt( st.nextToken());
								int j = Integer.parseInt( st.nextToken());
								if(hostShip[i][j] == true)
								{
									marker( i,j, "H" );
									mark[i][j].setFill(Color.GREEN);
								}
								else
								{
									marker( i,j, "M" );
									mark[i][j].setFill(Color.RED);
								}

								myTurn = true;
							}
							else if(cmd.equals("ship"))//Tells where the oppistes ship is
							{
								int i = Integer.parseInt( st.nextToken());
								int j = Integer.parseInt( st.nextToken());
								aText.setText("gotit");
								setOpShip( i,j, "Ship" );
								myTurn = true;
							}
							else if (cmd.equals("GameOver"))//Sends game over and ends game
							{
								gameOver = true;
							}
						}
						catch(Exception k)
						{ System.out.println(myName+" read failure"); }
					}
				}
				catch(Exception h)
				{ say("couldn't read from the other end"); }
			}
		}
	}

	// add this string to the conversation.
	public void say(String s) 
	{

		String prevMess = aText.getText();
		aText.setText(prevMess + "\r\n"  +yourName+" says "+s);
	}

	// if the output is established, send s to it.
	public void send( String s )
	{
		if ( myOut!=null )
		{
			myOut.println(s);
		}
	}
}
