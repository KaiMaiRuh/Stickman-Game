// Obstacle.java - คลาสสิ่งกีดขวาง
import java.awt.image.BufferedImage;
class Obstacle extends Sprite {
    private double speedX;   // ความเร็วในแนวแกน X (พิกเซลต่อเฟรม)
    
    public Obstacle(double x, double y, BufferedImage image, double speedX) {
        super(x, y, image);
        this.speedX = speedX;
    }
    
    @Override
    public void update() {
        // เลื่อนตำแหน่งตามความเร็วแนว x
        x += speedX;
    }
}
