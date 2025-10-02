import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Base class for all drawable entities in the game world.  A Sprite
 * encapsulates a position in the world, dimensions and an optional
 * image.  Subclasses implement the {@link #update()} method to
 * advance their state each frame.  The {@link #draw(Graphics2D)}
 * method renders the sprite at its current position using its
 * associated image; if no image is supplied subclasses are free to
 * override draw to provide custom rendering.
 */
abstract class Sprite {
    protected double x;
    protected double y;
    protected int width;
    protected int height;
    protected BufferedImage image;

    public Sprite(double x, double y, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();
        }
    }

    /**
     * Update the state of the sprite.  Called once per frame from the
     * game loop.  Subclasses should override this method to move
     * themselves or implement behaviour such as physics, animation or
     * logic.
     */
    public abstract void update();

    /**
     * Draws the sprite to the provided graphics context.  If an
     * image is associated with the sprite it is drawn at the
     * sprite's current position.  Subclasses may override to draw
     * primitives or apply effects in addition to drawing the image.
     *
     * @param g the graphics context to draw to
     */
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), null);
        }
    }

    /**
     * Returns the bounding rectangle of this sprite for collision
     * detection.  The rectangle is calculated from the current
     * position and the sprite's width and height.  Because the
     * position is maintained as doubles the values are rounded to
     * integers.
     *
     * @return a new Rectangle representing the sprite's bounds
     */
    public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    // Accessors useful for subclasses and collision logic
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}