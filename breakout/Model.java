// The model represents all the actual content and functionality of the game
// For Breakout, it manages all the game objects that the View needs
// (the bat, ball, bricks, and the score), provides methods to allow the Controller
// to move the bat (and a couple of other functions - change the speed or stop 
// the game), and runs a background process (a 'thread') that moves the ball 
// every 20 milliseconds and checks for collisions 

import java.util.Random;
import javafx.scene.paint.*;
import javafx.application.Platform;
import java.io.*;
import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class Model 
{
    // First,a collection of useful values for calculating sizes and layouts etc.

    public int B              = 6;      // Border round the edge of the panel
    public int M              = 40;     // Height of menu bar space at the top

    public int BALL_SIZE      = 15;     // Ball size
    public int BRICK_WIDTH    = 35;     // Brick size
    public int BRICK_HEIGHT   = 20;

    public int BAT_MOVE       = 10;      // Distance to move bat on each keypress
    public int BALL_MOVE      = 3;      // Units to move the ball on each step

    public int HIT_BRICK      = 50;     // Score for hitting a brick
    public int HIT_BOTTOM     = -200;   // Score (penalty) for hitting the bottom of the screen

    // The other parts of the model-view-controller setup
    View view;
    Controller controller;

    // The game 'model' - these represent the state of the game
    // and are used by the View to display it
    public GameObj[] balls;                // The balls
    public GameObj[] bricks;            // The bricks
    public GameObj bat;                 // The bat
    public int score = 0;               // The score
    public int lives = 3;               // users lives
    public int level = 1;               //Starting at level 1.
    public int hitBricks = 0;           //Number of bricks hit
    public int numBricks;               //number of bricks
    public int numBalls = 1;            // number of balls

    // variables that control the game 
    public String gameState = "running";// Set to "finished" to end the game
    public boolean fast = false;        // Set true to make the ball go faster

    // initialisation parameters for the model
    public int width;                   // Width of game
    public int height;                  // Height of game
    
    public Clip bg;                     //background music created here so that it can be accessed 
                                        //throughout the code so i can turn it off when game stops

    // CONSTRUCTOR - needs to know how big the window will be
    public Model( int w, int h )
    {
        Debug.trace("Model::<constructor>");  
        width = w; 
        height = h;


    }

    
    // Animating the game
    // The game is animated by using a 'thread'. Threads allow the program to do 
    // two (or more) things at the same time. In this case the main program is
    // doing the usual thing (View waits for input, sends it to Controller,
    // Controller sends to Model, Model updates), but a second thread runs in 
    // a loop, updating the position of the ball, checking if it hits anything
    // (and changing direction if it does) and then telling the View the Model 
    // changed.
    
    // When we use more than one thread, we have to take care that they don't
    // interfere with each other (for example, one thread changing the value of 
    // a variable at the same time the other is reading it). We do this by 
    // SYNCHRONIZING methods. For any object, only one synchronized method can
    // be running at a time - if another thread tries to run the same or another
    // synchronized method on the same object, it will stop and wait for the
    // first one to finish.
    
    // Start the animation thread
    
    
    public void startGame()
    {
        initialiseGame();                           // set the initial game state
        Thread t = new Thread( this::runGame );     // create a thread running the runGame method
        t.setDaemon(true);                          // Tell system this thread can die when it finishes
        t.start();                                  // Start the thread running
        try {                                                           //syntax for accessing and playing a sound
            File bgMusic = new File("bgMusic.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(bgMusic);    //allows the audio to be read
            bg = AudioSystem.getClip();                                             //Allows audio to be controlled
            bg.open(audioIn);                                                          //preparing playback
            bg.start();                                             
            } catch (Exception e) {
                e.printStackTrace();
            }
        if (gameState == "finished")
        {
            bg.stop(); //Stopping background music when game stops.
        }        
    }   
    
// Initialise the game - reset the score and create the game objects 
public void initialiseGame()
{       
    score = 0;  //resetting score
    lives = 3; //also resetting lives
    numBalls = 1; //Removing extra balls
    //Creating the first ball, the bricks and the bat.
    ballCreator();
    bat    = new GameObj(width/2, height - BRICK_HEIGHT*3/2, BRICK_WIDTH*3, BRICK_HEIGHT/4, Color.WHITE);
    brickCreator();
}
 

public void ballCreator()
    {
        balls = new GameObj[numBalls];
        for (int b = 0; b < numBalls; b++) {
            GameObj ball = new GameObj(width/2, 500, BALL_SIZE, BALL_SIZE, Color.WHITE);
            balls[b] = ball;
        }
    }
         
    public void brickCreator()
    {
    Color[] colours = {
            Color.web("#74c365"),
            Color.web("#4666ff"),
            Color.web("#f4c325"),
            Color.web("#f85376"),
            Color.web("#ff7f50")
        };    
        
    int wallTop = 100;
    int bricksPerRow = width/(BRICK_WIDTH);
    int rows = 10;
    numBricks = bricksPerRow * rows;
    int gaps = 0;
    bricks = new GameObj[numBricks];
    Random rand = new Random();
    
    for (int row = 0; row < rows; row++) {         //initialise;condition;iteration
        for (int i = 0 ;i < bricksPerRow; i++) {
            Color randomColour = colours[rand.nextInt(colours.length)];
            GameObj brick = new GameObj(BRICK_WIDTH*i + 3*i, wallTop + row * (BRICK_HEIGHT + 5), BRICK_WIDTH, BRICK_HEIGHT, randomColour);
                                        // x,y, width, height, colour
            bricks[i + row * bricksPerRow] = brick;
            if (rand.nextInt(100) <= 40) { //probability of the brick not being drawn to leave gaps to enhance gameplay.
            brick.visible = false;   // this actually creates the gap as it means view will not draw it.
            gaps++;
            }
            }
    }
    numBricks = numBricks - gaps; // So that the gaps do not affect the ability to level up.
}

 
    // The main animation loop
    public void runGame()
    {
        try
        {
            Debug.trace("Model::runGame: Game starting"); 
            // set game true - game will stop if it is set to "finished"
            setGameState("running");
            while (!getGameState().equals("finished"))
            {
                updateGame();                        // update the game state
                modelChanged();                      // Model changed - refresh screen
                Thread.sleep( getFast() ? 10 : 20 ); // wait a few milliseconds
            }
            Debug.trace("Model::runGame: Game finished"); 

        } catch (Exception e) 
        { 
            Debug.error("Model::runAsSeparateThread error: " + e.getMessage() );
        }
    }
  
    // updating the game - this happens about 50 times a second to give the impression of movement
    public synchronized void updateGame() throws IOException
    {
        // move the ball one step (the ball knows which direction it is moving in)
        
        for (int b = 0; b < numBalls; b++) {                //allowing each ball object to act as the original ball does.
            GameObj ball = balls[b];
        
        ball.moveX(BALL_MOVE);                      
        ball.moveY(BALL_MOVE);
        // get the current ball possition (top left corner)
        int x = ball.topX;  
        int y = ball.topY;
        // Deal with possible edge of board hit
        if (x >= width - B - BALL_SIZE)  ball.changeDirectionX();
        if (x <= 0 + B)  ball.changeDirectionX();
        if (y >= height - B - BALL_SIZE)  // Bottom
        { 
            ball.changeDirectionY(); 
            addToScore( HIT_BOTTOM );
            lives = lives-1;
            try {
                File crashSound = new File("crash.wav");
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(crashSound);
                Clip crash = AudioSystem.getClip();
                crash.open(audioIn);
                crash.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (y <= 0 + M)  ball.changeDirectionY();
        
       // check whether ball has hit a (visible) brick
        boolean hit = false;

        // *[3]******************************************************[3]*
        // * Fill in code to check if a visible brick has been hit      *
        // * The ball has no effect on an invisible brick               *
        // * If a brick has been hit, change its 'visible' setting to   *
        // * false so that it will 'disappear'                          * 
        // **************************************************************
        for (GameObj brick: bricks) {
            if (brick.visible && brick.hitBy(ball)) {
                hit = true;
                brick.visible = false;      // set the brick invisible
                addToScore( HIT_BRICK );// add to score for hitting a brick
                hitBricks++;            //Counting the number of bricks hit to aid levelling up.
                try {
                    File brickHit = new File("brickHit.wav");
                    AudioInputStream audioIn = AudioSystem.getAudioInputStream(brickHit);
                    Clip bh = AudioSystem.getClip();
                    bh.open(audioIn);
                    bh.start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        } 

        if (hit) {
            ball.changeDirectionY();
        }
        
        // check whether ball has hit the bat
        if ( ball.hitBy(bat) ) {
            ball.changeDirectionY();
            try {
            File batHit = new File("bat.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(batHit);
            Clip bat = AudioSystem.getClip();
            bat.open(audioIn);
            bat.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
    }

    public synchronized Boolean nextLevel()
    {
            if (hitBricks >= numBricks) {
            hitBricks = 0;          
            level++;                //increasing level if the number of bricks hit equals total number of drawn bricks.
            numBalls++;
            brickCreator();         //redraw bricks
            ballCreator();          // add another ball
            return true;
        } else {
            return false;
        }
    }
    
    public void test(){
        hitBricks = bricks.length;
    }
        
    // This is how the Model talks to the View
    // Whenever the Model changes, this method calls the update method in
    // the View. It needs to run in the JavaFX event thread, and Platform.runLater 
    // is a utility that makes sure this happens even if called from the
    // runGame thread
    
    public synchronized String gameOver()
    {
        if (lives < 0)
            {
            return (gameState = "finished");
            }
        else
        {
            return ("not");
        }
        
    }
    
    
    public synchronized void modelChanged()
    {
        Platform.runLater(view::update);
    }
    
    
    // Methods for accessing and updating values
    // these are all synchronized so that the can be called by the main thread 
    // or the animation thread safely
    
    // Change game state - set to "running" or "finished"
    public synchronized void setGameState(String value)
    {  
        gameState = value;
    }
    
    // Return game running state
    public synchronized String getGameState()
    {  
        return gameState;
    }

    // Change game speed - false is normal speed, true is fast
    public synchronized void setFast(Boolean value)
    {  
        fast = value;
    }
    
    // Return game speed - false is normal speed, true is fast
    public synchronized Boolean getFast()
    {  
        return(fast);
    }

    // Return bat object
    public synchronized GameObj getBat()
    {
        return(bat);
    }
    
    // return ball object
    public synchronized GameObj[] getBalls()
    {
        return(balls);
    }
    
    public synchronized int getNumBalls()
    {
        return(numBalls);
    }
   
    // return bricks
    public synchronized GameObj[] getBricks()
    {
        return(bricks);
    }
    
    // return score
    public synchronized int getScore()
    {
        return(score);
    }
    
    //return Lives
    public synchronized int getLives()
    {
        return(lives);
    }
    
    public synchronized int getLevel()
    {
        return(level);
    }
      
     // update the score
    public synchronized void addToScore(int n)    
    {
        score += n;        
    }
    
    // move the bat one step - -1 is left, +1 is right
    public synchronized void moveBat( int direction )
    {        
        int dist = direction * BAT_MOVE;    // Actual distance to move
        Debug.trace( "Model::moveBat: Move bat = " + dist );
        bat.moveX(dist);
    }
    
    //public synchronized void pause()
    //{
        //if (boolean pause == true)
        //{
            //gameState = "finished";
        //} else if (gameState =="finished")
        //{
            //updateGame();
           // modelChanged();                     
            //Thread.sleep( getFast() ? 10 : 20 );
        //}
    //}
}   
    