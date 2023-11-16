// The View class creates and manages the GUI for the application.
// It doesn't know anything about the game itself, it just displays
// the current state of the Model, and handles user input

// We import lots of JavaFX libraries (we may not use them all, but it
// saves us having to thinkabout them if we add new code)
import javafx.event.EventHandler;
import javafx.scene.input.*;
import javafx.scene.canvas.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.concurrent.TimeUnit;


public class View implements EventHandler<KeyEvent>
{ 
    // variables for components of the user interface
    public int width;       // width of window
    public int height;      // height of window

    // user interface objects
    public Pane pane;       // basic layout pane
    public Canvas canvas;   // canvas to draw game on
    public Label infoText;  // info at top of screen
    public Label livesText;
    public Label gameOverText;
    public Label gameOverText2;
    public Label levelText;

    // The other parts of the model-view-controller setup
    public Controller controller;
    public Model model;

    public GameObj   bat;            // The bat
    public GameObj[]   balls;           // The ball
    public GameObj[] bricks;         // The bricks
    public int numBalls = 1;
    public int       score =  0;     // The score
    public int lives = 3;
    public int level = 1;
    public String state = "not";     //gameover state
    public Boolean levelCheck = false;
   
    // constructor method - we get told the width and height of the window
    public View(int w, int h)
    {
        Debug.trace("View::<constructor>");
        width = w;
        height = h;
    }

    // start is called from the Main class, to start the GUI up
    
    public void start(Stage window) 
    {
        // breakout is basically one big drawing canvas, and all the objects are
        // drawn on it as rectangles, except for the text at the top - this
        // is a label which sits 'in front of' the canvas.
        
        // Note that it is important to create control objects (Pane, Label,Canvas etc) 
        // here not in the constructor (or as initialisations to instance variables),
        // to make sure everything is initialised in the right order
        pane = new Pane();       // a simple layout pane
        pane.setId("Breakout");  // Id to use in CSS file to style the pane if needed
        
        // canvas object - we set the width and height here (from the constructor), 
        // and the pane and window set themselves up to be big enough
        canvas = new Canvas(width,height);  
        pane.getChildren().add(canvas);     // add the canvas to the pane
        
        textDrawer();                   //draws all of the text

        // Make a new JavaFX Scene, containing the complete GUI
        Scene scene = new Scene(pane);   
        scene.getStylesheets().add("breakout.css"); // tell the app to use our css file

        // Add an event handler for key presses. By using 'this' (which means 'this 
        // view object itself') we tell JavaFX to call the 'handle' method (below)
        // whenever a key is pressed
        scene.setOnKeyPressed(this);

        // put the scene in the window and display it
        window.setScene(scene);
        window.show();
    }

    public void textDrawer()                //draws all of the text for the main game screen and removes any irrelevant text.
    {
        pane.getChildren().remove(gameOverText);
        pane.getChildren().remove(gameOverText2);
        pane.getChildren().remove(infoText);
        pane.getChildren().remove(levelText);
        infoText = new Label("Score = " + score);
        livesText = new Label("Lives = " + lives);
        levelText = new Label("Level = " + level);
        infoText.setTextFill(Color.WHITE);
        livesText.setTextFill(Color.WHITE);
        levelText.setTextFill(Color.WHITE);
        infoText.setTranslateX(55); 
        infoText.setTranslateY(10); 
        pane.getChildren().add(infoText);
        livesText.setTranslateX(55);
        livesText.setTranslateY(40);
        pane.getChildren().add(livesText);
        levelText.setTranslateX(400);
        levelText.setTranslateY(10);
        pane.getChildren().add(levelText);
    }
    
    // Event handler for key presses - it just passes the event to the controller
    public void handle(KeyEvent event)
    {
        // send the event to the controller
        controller.userKeyInteraction( event );
    }
    
    // drawing the game image
    public void drawPicture()
    {
        // the game loop is running 'in the background' so we have
        // add the following line to make sure it doesn't change
        // the model in the middle of us updating the image
        synchronized ( model ) 
        {
            // get the 'paint brush' to pdraw on the canvas
            GraphicsContext gc = canvas.getGraphicsContext2D();
            
            //fetching background images and drawing it onto the background. 
            Image backgroundImage = new Image("backgroundimage.png");
            gc.drawImage(backgroundImage, 0, 0, width, height);
            
            // draw all of the balls
            for (GameObj ball: balls) {
                displayGameObj( gc, ball ); 
            }
            
            displayGameObj( gc, bat  );  // Display the Bat

                    
            // *[2]****************************************************[2]*
            // * Display the bricks that make up the game                 *
            // * Fill in code to display bricks from the brick array      *
            // * Remember only a visible brick is to be displayed         *
            // ************************************************************
            
            //displays bricks that have not been hit.
            for (GameObj brick: bricks) {
                if (brick.visible) {
                    displayGameObj(gc, brick);
                }
            }
            
                     
            // update the score + lives
            infoText.setText("Score = " + score);
            livesText.setText("Lives = "+ lives);
            levelText.setText("Level = "+ level);
        }
    }

    // Display a game object - it is just a rectangle on the canvas
    public void displayGameObj( GraphicsContext gc, GameObj go )
    {
        gc.setFill( go.colour );
        gc.fillRect( go.topX, go.topY, go.width, go.height );
    }

    // This is how the Model talks to the View
    // This method gets called BY THE MODEL, whenever the model changes
    // It has to do whatever is required to update the GUI to show the new game position
    public void update()
    {
        // Get from the model the ball, bat, bricks & score
        balls    = model.getBalls();              // Ball
        bricks  = model.getBricks();            // Bricks
        bat     = model.getBat();               // Bat
        score   = model.getScore();             // Score
        lives   = model.getLives();             //lives
        level   = model.getLevel();             //level
        numBalls = model.getNumBalls();         //number of balls
        //Debug.trace("Update");
        drawPicture();                     // Re draw game
        
        state = model.gameOver();
        if (state == "finished")   //game over screen
            {
            GraphicsContext gc = canvas.getGraphicsContext2D();            
            gc.setFill( Color.BLACK );
            gc.fillRect( 0, 0, width, height );
            gameOverText = new Label("Game Over!");
            gameOverText.getStyleClass().add("bigLabel");
            gameOverText.setTextFill(Color.WHITE);
            gameOverText.setTranslateX(150);
            gameOverText.setTranslateY(200);  
            pane.getChildren().add(gameOverText);
            pane.getChildren().remove(livesText);
            pane.getChildren().remove(infoText);
            pane.getChildren().remove(levelText);
            pane.getChildren().add(infoText);
            infoText.setTranslateX(200);
            infoText.setTranslateY(270);
            gameOverText2 = new Label ("Press space to try again!");
            gameOverText2.setTextFill(Color.WHITE);
            gameOverText2.setTranslateX(130);
            gameOverText2.setTranslateY(300);
            pane.getChildren().add(gameOverText2);
            }
        
            
        levelCheck = model.nextLevel();             //redrawing screen if the user advances to the next level.
        if (levelCheck == true)
        {
            drawPicture();
        }else {
            
        }
    }
    
    
}



