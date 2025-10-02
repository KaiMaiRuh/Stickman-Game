// Sprite.java - คลาสฐานของวัตถุเกมทั้งหมด (ผู้เล่นและสิ่งกีดขวาง)
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

abstract class Sprite {
    protected double x, y;           // พิกัดปัจจุบันของวัตถุ
    protected int width, height;     // ขนาดของภาพวัตถุ
    protected BufferedImage image;
    
    public Sprite(double x, double y, BufferedImage image) {
        this.x = x;
        this.y = y;
        this.image = image;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }
    
    public abstract void update();   // ให้คลาสย่อยกำหนดพฤติกรรมการเปลี่ยนแปลงแต่ละเฟรม
    
    public void draw(Graphics2D g) {
        g.drawImage(image, (int)x, (int)y, null);
    }
    
    // Getter/Setter เฉพาะที่จำเป็น
    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
