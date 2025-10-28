import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

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

        public abstract void update();

        public void draw(Graphics2D g) {
        if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), null);
        }
    }

        public Rectangle getBounds() {
        return new Rectangle((int) Math.round(x), (int) Math.round(y), width, height);
    }

    
    public double getX() { return x; }
    public double getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
}