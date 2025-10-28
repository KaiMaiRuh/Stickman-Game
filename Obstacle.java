import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

class Obstacle extends Sprite {
    private double speedX;
    public double getSpeedX() { return speedX; }
    public void setSpeedX(double v) { this.speedX = v; }

    public Obstacle(double x, double y, BufferedImage image, double speedX) {
        this(x, y, image, speedX, -1);
    }

        public Obstacle(double x, double y, BufferedImage image, double speedX, int desiredHeight) {
        super(x, y, image);
        this.speedX = speedX;
        if (image == null) {
            this.width = 30;
            this.height = 60;
        } else if (desiredHeight > 0) {
            
            int srcW = image.getWidth();
            int srcH = image.getHeight();
            double scale = (double) desiredHeight / (double) srcH;
            this.height = desiredHeight;
            this.width = (int) Math.round(srcW * scale);
        } else {
            
            this.width = image.getWidth();
            this.height = image.getHeight();
        }
    }

    @Override
    public void update() {
        x += speedX;
    }

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