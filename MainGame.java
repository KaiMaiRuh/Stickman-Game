import javax.swing.JFrame;

/**
 * Entry point for the Stickman Runner game.  This class is responsible for
 * creating the application window, instantiating the canvas that contains
 * the game logic and graphics, and starting the main game loop.  The
 * dimensions of the window are defined in GameCanvas.  When running the
 * application from the command line be sure to compile all Java files
 * together, for example:
 *
 * {@code javac MainGame.java GameCanvas.java Sprite.java Player.java Obstacle.java}
 *
 * and then launch with:
 * {@code java -Dsun.java2d.opengl=true -Dsun.java2d.pmoffscreen=true MainGame}
 */
public class MainGame {
    public static void main(String[] args) {
        // Create the application window and attach the game canvas.  The
        // preferred size of the canvas determines the window size.
        JFrame frame = new JFrame("Stickman Runner Game");
        GameCanvas canvas = new GameCanvas();
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Request focus so the canvas receives key events and start the
        // game loop on a separate thread.
        canvas.requestFocusInWindow();
        canvas.start();
    }
}