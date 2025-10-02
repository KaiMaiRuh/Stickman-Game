// Player.java - คลาสผู้เล่น (Stickman)
import java.awt.image.BufferedImage;
class Player extends Sprite {
    private double velocityY = 0;    // ความเร็วแนวดิ่ง (หน่วยพิกเซลต่อเฟรม)
    private boolean onGround = true; // สถานะอยู่บนพื้นหรือไม่
    
    public Player(double x, double y, BufferedImage image) {
        super(x, y, image);
    }
    
    @Override
    public void update() {
        if (!onGround) {
            // ถ้าอยู่กลางอากาศ ให้ความเร็วเพิ่มลง (แรงโน้มถ่วง)
            velocityY += 0.5;
        }
        // ปรับตำแหน่งตามความเร็ว
        y += velocityY;
        // ตรวจสอบการชนพื้น
        if (y + height >= GameCanvas.HEIGHT) {
            y = GameCanvas.HEIGHT - height;  // ตั้งให้ยืนบนพื้นพอดี
            velocityY = 0;
            onGround = true;
        }
    }
    
    // เริ่มการกระโดด (เรียกเมื่อกดปุ่มกระโดด)
    public void jump() {
        if (onGround) {
            velocityY = -10;    // ความเร็วขึ้นด้านบน (ตัวเลขยิ่งลบยิ่งกระโดดสูง)
            onGround = false;
        }
    }
    
    public boolean isOnGround() {
        return onGround;
    }
}
