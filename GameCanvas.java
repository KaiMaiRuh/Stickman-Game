import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 * GameCanvas is the core of the Stickman Runner game.  It extends
 * {@link Canvas} so that it can take advantage of active rendering
 * through a {@link BufferStrategy}.  The canvas owns the game loop,
 * handles keyboard input, updates all game entities each frame,
 * performs collision detection and draws the current state of the
 * world to the screen.  All heavy lifting for timing, rendering and
 * updating occurs here.
 */
public class GameCanvas extends Canvas implements Runnable, KeyListener {
    // Logical size of the game.  The physical window will be sized to
    // this dimension.  Altering these values will resize the entire
    // game world accordingly.
    public static final int WIDTH = 800;
    public static final int HEIGHT = 400;

    // Target frames per second.  120 FPS provides a smooth experience
    // on monitors that support high refresh rates while still being
    // achievable on most modern hardware.  The sleep logic in the
    // run() loop attempts to maintain this frame rate by sleeping the
    // remainder of the frame after update and render operations.
    private static final int TARGET_FPS = 120;
    private static final long TARGET_FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;

    // Game thread and state flag.  The thread will continuously
    // execute the run() method until running is set to false.
    private Thread gameThread;
    private volatile boolean running = false;
    private BufferStrategy bufferStrategy;

    // Game entities.  The player is controlled by the user; obstacles
    // spawn periodically and move towards the left.  When an obstacle
    // moves off the left edge of the screen it will be recycled by
    // resetting its x position or removed from the list.
    private Player player;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final Random random = new Random();

    // Score counter.  This increments each time an obstacle passes off
    // the screen without colliding with the player.  Displayed in
    // the top-left corner of the screen.  It could be persisted or
    // reset as needed.
    private int score = 0;

    // Obstacle spawn timer.  Obstacles appear at semi-random
    // intervals between minSpawnDelay and maxSpawnDelay seconds.  A
    // timer accumulates delta time each frame; once it exceeds the
    // chosen delay a new obstacle is spawned and the timer resets.
    private double spawnTimer = 0;
    private double nextSpawnDelay = 1.5; // seconds until next obstacle
    private static final double MIN_SPAWN_DELAY = 1.0;
    private static final double MAX_SPAWN_DELAY = 2.0;

    // Images for the player and obstacle.  These are loaded once
    // during construction.  If loading fails the game will fall back
    // to simple coloured rectangles.  Images are scaled to sensible
    // sizes for the window to avoid oversized sprites.
    private BufferedImage playerImage;
    private BufferedImage obstacleImage;

    /**
     * Constructs the game canvas.  Configures its preferred size,
     * disables Swing repaint handling and loads resources.  The
     * caller should invoke {@link #start()} to begin the game loop.
     */
    public GameCanvas() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIgnoreRepaint(true);
        addKeyListener(this);
        setFocusable(true);

        // Load images.  If either load fails the corresponding
        // variable remains null; drawing methods will handle null by
        // drawing placeholders.  The names must match the files in
        // the working directory exactly (case sensitive on Linux).
        loadImages();

        // Create the player.  Position the player just above the
        // bottom of the screen taking its height into account.  Use
        // the player image if available; otherwise the constructor
        // will fall back to drawing a coloured rectangle.
        int playerWidth = (playerImage != null) ? playerImage.getWidth() : 40;
        int playerHeight = (playerImage != null) ? playerImage.getHeight() : 60;
        double playerX = 100;
        double playerY = HEIGHT - playerHeight - 50; // leave room for ground
        player = new Player(playerX, playerY, playerImage);
    }

    /**
     * Starts the game loop on a new thread if it isn't already
     * running.  It also creates the buffer strategy used for
     * double-buffered rendering.  This method should only be called
     * once; subsequent calls will be ignored if the game is already
     * running.
     */
    public synchronized void start() {
        if (running) {
            return;
        }
        createBufferStrategy(2);
        bufferStrategy = getBufferStrategy();
        running = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();
    }

    /**
     * The main game loop.  This method is executed on the game
     * thread.  It repeatedly updates game state, renders the frame
     * and sleeps for the remainder of the frame time.  If the
     * processing of a frame takes longer than the target frame time
     * the loop will skip sleeping and immediately process the next
     * frame.  When {@link #running} becomes false the loop exits and
     * the thread terminates.
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            double dt = (frameStart - lastTime) / 1_000_000_000.0;
            lastTime = frameStart;

            updateGame(dt);
            renderGame();

            long frameEnd = System.nanoTime();
            long elapsed = frameEnd - frameStart;
            long sleepTime = TARGET_FRAME_TIME_NS - elapsed;
            if (sleepTime > 0) {
                try {
                    // Convert nanoseconds into milliseconds and nanoseconds for sleep
                    long ms = sleepTime / 1_000_000L;
                    int ns = (int) (sleepTime % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Loads the player's and obstacle's images from disk.  If the
     * images are too large relative to the game window they will be
     * scaled down to a sensible size while preserving aspect ratio.
     */
    private void loadImages() {
        try {
            // Attempt to load the player image.  The file name is
            // case sensitive on Linux; ensure that Stickman.png exists
            // relative to the working directory.  If the file is too
            // large relative to the screen height scale it down.
            File playerFile = new File("Stickman.png");
            if (playerFile.exists()) {
                BufferedImage raw = ImageIO.read(playerFile);
                playerImage = scaleToHeight(raw, 100);
            }

            // Load the obstacle image.  This example uses a simple
            // vertical bar.  It will also be scaled down if taller than
            // a certain threshold.
            File obstacleFile = new File("obstacle.png");
            if (obstacleFile.exists()) {
                BufferedImage raw = ImageIO.read(obstacleFile);
                obstacleImage = scaleToHeight(raw, 100);
            }
        } catch (IOException e) {
            // If loading fails log the error and fall back to
            // simple coloured rectangles.  We don't throw here to
            // avoid crashing the game because of missing assets.
            System.err.println("Failed to load images: " + e.getMessage());
            playerImage = null;
            obstacleImage = null;
        }
    }

    /**
     * Scales a {@link BufferedImage} to a target height while
     * preserving aspect ratio.  If the source image is already
     * smaller than the target height it will be returned unchanged.
     *
     * @param src the source image to scale
     * @param targetHeight the desired height in pixels
     * @return a scaled copy of the image or the original if no scaling
     *         was needed
     */
    private static BufferedImage scaleToHeight(BufferedImage src, int targetHeight) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        if (srcHeight <= targetHeight) {
            return src;
        }
        double scale = (double) targetHeight / srcHeight;
        int targetWidth = (int) Math.round(srcWidth * scale);
        Image scaled = src.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage dest = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return dest;
    }

    /**
     * Updates the game state.  This includes moving the player and
     * obstacles, spawning new obstacles, handling collisions and
     * updating the score.  The dt parameter provides the elapsed
     * seconds since the last update; although the loop attempts to
     * maintain a fixed frame time this value is used to make the
     * game time consistent even if small hiccups occur.
     *
     * @param dt the elapsed seconds since the previous frame
     */
    private void updateGame(double dt) {
        // Update the player.  The player handles its own gravity and
        // ground collision.  It doesn't move horizontally in this
        // simple runner but could be extended to allow sliding or
        // forward/backward movement.
        player.update();

        // Update obstacles.  Move each obstacle leftwards by a fixed
        // amount per frame.  If an obstacle collides with the player
        // trigger a game over.  If an obstacle moves off the left
        // edge increment the score and recycle it to the right.
        Iterator<Obstacle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Obstacle obs = iter.next();
            obs.update();

            // Check for collision with the player.  We use simple
            // bounding box intersection which is sufficient for
            // rectangular sprites.
            if (player.getBounds().intersects(obs.getBounds())) {
                gameOver();
                return;
            }

            // If the obstacle has completely left the screen to the
            // left, increase the score and respawn it at the right
            // hand side of the screen with a random vertical alignment
            // matching the ground.  This avoids constantly creating
            // and destroying objects which can trigger GC pauses.
            if (obs.getX() + obs.getWidth() < 0) {
                score++;
                // Respawn the obstacle at the right edge with a
                // consistent y coordinate so the player can jump over
                // it.  Reset the x coordinate to the width of the
                // window.
                obs.setX(WIDTH + random.nextInt(200));
            }
        }

        // Handle obstacle spawning.  Decrease the spawn timer and
        // spawn a new obstacle when it runs out.  Then reset the
        // timer to a random delay within the min/max bounds.  This
        // creates a pseudo-random spacing between obstacles which
        // keeps the gameplay varied without overwhelming the player.
        spawnTimer += dt;
        if (spawnTimer >= nextSpawnDelay) {
            spawnTimer = 0;
            nextSpawnDelay = MIN_SPAWN_DELAY + random.nextDouble() * (MAX_SPAWN_DELAY - MIN_SPAWN_DELAY);
            spawnObstacle();
        }
    }

    /**
     * Spawns a new obstacle at the right hand side of the screen.
     * Obstacles use the configured image if available or default to a
     * red rectangle.  They move leftwards at a fixed speed.
     */
    private void spawnObstacle() {
        double x = WIDTH;
        double y;
        int imgHeight = (obstacleImage != null) ? obstacleImage.getHeight() : 60;
        y = HEIGHT - imgHeight - 50;
        double speedX = -5 - random.nextDouble() * 2; // vary speed slightly
        Obstacle obs = new Obstacle(x, y, obstacleImage, speedX);
        obstacles.add(obs);
    }

    /**
     * Renders the current game state to the back buffer.  This
     * includes clearing the background, drawing the player and
     * obstacles and rendering the score.  After drawing the buffer
     * strategy is shown and the graphics context disposed.  A call
     * to Toolkit.sync() ensures that the contents of the back buffer
     * are flushed to the display immediately; this is particularly
     * important on Linux platforms to avoid visual tearing or stale
     * frames.
     */
    private void renderGame() {
        do {
            do {
                Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
                try {
                    // Clear the background
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, WIDTH, HEIGHT);

                    // Draw a simple ground line for reference
                    g.setColor(new Color(220, 220, 220));
                    g.fillRect(0, HEIGHT - 50, WIDTH, 50);

                    // Draw the player
                    player.draw(g);

                    // Draw obstacles
                    for (Obstacle obs : obstacles) {
                        obs.draw(g);
                    }

                    // Draw the score
                    g.setColor(Color.BLACK);
                    g.setFont(g.getFont().deriveFont(18f));
                    g.drawString("Score: " + score, 10, 20);
                } finally {
                    g.dispose();
                }
                // If the contents were restored (rare) repeat the
                // render
            } while (bufferStrategy.contentsRestored());
            // Display the next frame
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();
            // If the buffer was lost (e.g. window resized) repeat the
            // render cycle
        } while (bufferStrategy.contentsLost());
    }

    /**
     * Invoked when the player collides with an obstacle.  The game
     * loop is stopped and a dialog is shown to the user.  After
     * acknowledgement the application exits.  This behaviour can be
     * customised to reset the game instead of exiting.
     */
    private void gameOver() {
        running = false;
        // Show a modal dialog on the Event Dispatch Thread.  Swing
        // components should only be touched on the EDT but since this
        // game loop runs on another thread we invoke later.
        javax.swing.SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "Game Over!\nFinal Score: " + score);
            System.exit(0);
        });
    }

    // --- KeyListener implementation ---
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        // Jump when pressing space or the up arrow, only if on ground
        if ((code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) && player.isOnGround()) {
            player.jump();
        }
        // Slide when pressing the down arrow; adjust the player's
        // height temporarily.  This simple example doesn't implement
        // sliding but the hook is here for future expansion.
        if (code == KeyEvent.VK_DOWN) {
            player.startSlide();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (code == KeyEvent.VK_DOWN) {
            player.endSlide();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }
}