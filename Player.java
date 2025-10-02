import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * Represents the player-controlled stickman.  Handles jumping,
 * gravity and sliding.  The player can only jump when on the
 * ground and sliding temporarily reduces the height of the player's
 * bounding box to allow ducking under obstacles.  The sliding
 * mechanism can be extended to include a timer or animation.
 */
class Player extends Sprite {
    private double velocityY = 0;    // vertical speed
    private boolean onGround = true; // whether the player is on the ground
    private boolean sliding = false; // whether the player is currently sliding

    // Original dimensions.  Used to restore height after sliding.
    private final int originalHeight;
    private final int originalWidth;

    public Player(double x, double y, BufferedImage image) {
        super(x, y, image);
        this.originalWidth = this.width;
        this.originalHeight = this.height;
    }

    @Override
    public void update() {
        // Apply gravity.  When jumping or falling, velocityY is
        // updated each frame.  When on ground velocityY remains 0.
        if (!onGround) {
            velocityY += 0.5; // gravity strength
        }

        // Update vertical position
        y += velocityY;

        // Check for collision with the ground.  If the bottom of the
        // player goes below the ground level clamp it and reset
        // velocity.  The ground is defined as HEIGHT - 50 in
        // GameCanvas.  Because Player has no reference to the canvas
        // dimensions we assume that the caller sets y such that the
        // ground is y value and that negative velocity resets the
        // onGround flag appropriately.
        double groundY = GameCanvas.HEIGHT - 50 - height;
        if (y >= groundY) {
            y = groundY;
            velocityY = 0;
            onGround = true;
        }
    }

    /**
     * Initiates a jump if the player is on the ground.  Sets the
     * vertical velocity to a negative value to move up on the next
     * update and marks the player as airborne.
     */
    public void jump() {
        if (onGround) {
            velocityY = -10;
            onGround = false;
        }
    }

    /**
     * Starts sliding.  When sliding the player's height is reduced
     * and the y position adjusted so the bottom of the player stays
     * aligned with the ground.  A more sophisticated implementation
     * might include a timer to end sliding automatically.
     */
    public void startSlide() {
        if (!sliding && onGround) {
            sliding = true;
            // Reduce height to half and raise y so the bottom stays on the ground
            height = originalHeight / 2;
            y += originalHeight / 2;
        }
    }

    /**
     * Ends sliding and restores the player's original height.  The
     * y position is adjusted downward accordingly.  If a timer
     * system is desired this could be invoked after a delay instead
     * of directly from keyReleased.
     */
    public void endSlide() {
        if (sliding) {
            // Restore y first then height to keep the bottom aligned
            y -= originalHeight / 2;
            height = originalHeight;
            sliding = false;
        }
    }

    /**
     * Indicates whether the player is currently on the ground.
     * @return true if the player is touching the ground, false if
     *         airborne
     */
    public boolean isOnGround() {
        return onGround;
    }

    @Override
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), width, height, null);
        } else {
            // Fallback: draw a simple blue rectangle when no image is available
            g.setColor(Color.BLUE);
            g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }
}