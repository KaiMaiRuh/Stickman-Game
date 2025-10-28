import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

class Gift extends Sprite {
    enum Type { GIFT1, GIFT2 }

    private final Type type;
    private double speedX;
    private boolean collected = false;

    public Gift(double x, double y, BufferedImage image, double speedX, Type type) {
        super(x, y, image);
        this.speedX = speedX;
        this.type = type;
        if (image != null) {
            this.width = image.getWidth();
            this.height = image.getHeight();
        } else {
            this.width = 24;
            this.height = 24;
        }
    }

    public Type getType() { return type; }
    public boolean isCollected() { return collected; }
    public void markCollected() { this.collected = true; }
    public double getSpeedX() { return speedX; }
    public void setSpeedX(double v) { this.speedX = v; }

    @Override
    public void update() {
        x += speedX;
    }

    @Override
    public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), width, height, null);
        } else {
            super.draw(g);
        }
    }
}