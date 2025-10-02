import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Represents a single obstacle in the game.  Obstacles move
 * horizontally across the screen at a constant speed.  When they
 * exit the left side of the screen they can be recycled by
 * resetting their x position.  The image provided defines the
 * obstacle's appearance; if null a red rectangle is drawn instead.
 */
class Obstacle extends Sprite {
    private double speedX;

    public Obstacle(double x, double y, BufferedImage image, double speedX) {
        super(x, y, image);
        this.speedX = speedX;
        // If no image was provided set default dimensions
        if (image == null) {
            this.width = 30;
            this.height = 60;
        }
    }

    @Override
    public void update() {
        x += speedX;
    }

    /**
     * Draws the obstacle.  If an image is supplied it will be
     * rendered; otherwise a red rectangle is drawn.  The rectangle
     * size corresponds to the width/height fields of this instance.
     */
    @Override
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), width, height, null);
        } else {
            g.setColor(Color.RED);
            g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }
}