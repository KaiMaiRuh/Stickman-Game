// GameCanvas.java - คลาสสำหรับพื้นที่วาดเกมและจัดการ game loop + input
import java.awt.Canvas;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.Toolkit;
import java.awt.Dimension;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;

class GameCanvas extends Canvas implements Runnable, KeyListener {
    public static final int WIDTH = 800;    // ขนาดหน้าต่างเกม
    public static final int HEIGHT = 600;
    private static final int TARGET_FPS = 120;
    private static final long TARGET_FRAME_TIME_NS = 1000000000 / TARGET_FPS;
    
    private Thread gameThread;
    private boolean running = false;
    private BufferStrategy bufferStrategy;
    
    // ภาพเกมที่โหลดไว้ล่วงหน้า
    private BufferedImage stickmanImg;
    private BufferedImage obstacleImg;
    // วัตถุเกม
    private Player player;
    private List<Obstacle> obstacles;
    
    public GameCanvas() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIgnoreRepaint(true);           // ปิดการ repaint อัตโนมัติของ AWT/Swing
        addKeyListener(this);
        setFocusable(true);
        
        // โหลดภาพล่วงหน้า
        try {
            stickmanImg = ImageIO.read(new File("stickman.png"));
            obstacleImg = ImageIO.read(new File("obstacle.png"));
        } catch (IOException e) {
            System.err.println("Error loading images: " + e);
            // กรณีโหลดภาพไม่ได้ สามารถสร้างภาพ placeholder ขึ้นมาเพื่อกันเกมล่ม
            stickmanImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            Graphics g = stickmanImg.getGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, 50, 50);
            g.dispose();
            obstacleImg = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
            g = obstacleImg.getGraphics();
            g.setColor(Color.RED);
            g.fillRect(0, 0, 50, 50);
            g.dispose();
        }
        
        // สร้างวัตถุ Player และ Obstacle เริ่มต้น
        player = new Player(100, HEIGHT - stickmanImg.getHeight(), stickmanImg);
        obstacles = new ArrayList<>();
        // เพิ่ม obstacle หนึ่งอันที่ขอบขวาของหน้าจอ
        Obstacle obs = new Obstacle(WIDTH, HEIGHT - obstacleImg.getHeight(), obstacleImg, -5);
        obstacles.add(obs);
    }
    
    // เมธอดเริ่มเกม สร้าง BufferStrategy และเธรดเกม
    public void start() {
        if (running) return;
        createBufferStrategy(2);                  // สร้าง double buffer
        bufferStrategy = getBufferStrategy();
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }
    
    // Game Loop หลัก รันในเธรดแยก
    @Override
    public void run() {
        long startTime, elapsedTime;
        while (running) {
            startTime = System.nanoTime();
            
            // 1. อัพเดตตรรกะเกม (ตำแหน่ง ความเร็ว การชน เป็นต้น)
            updateGame();
            // 2. วาดกราฟิกเฟรมถัดไปลงบัฟเฟอร์
            renderGame();
            // 3. คำนวณเวลาและหน่วงให้ครบตามเฟรมเรตที่กำหนด (120 FPS)
            elapsedTime = System.nanoTime() - startTime;
            long remainingTime = TARGET_FRAME_TIME_NS - elapsedTime;
            if (remainingTime > 0) {
                try {
                    // แปลงนาโนเป็นมิลลิวินาทีและนาโนสำหรับ sleep
                    long ms = remainingTime / 1000000;
                    int ns = (int)(remainingTime % 1000000);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e) {
                    // ถูกรบกวนก็ข้ามไปเฟรมถัดไป
                }
            }
        }
    }
    
    // อัพเดตสถานะเกมต่อเฟรม
    private void updateGame() {
        player.update();
        for (Obstacle obs : obstacles) {
            obs.update();
        }
        // ถ้า obstacle อันใดหลุดพ้นจอด้านซ้าย ให้ย้ายกลับไปขอบขวา
        for (Obstacle obs : obstacles) {
            if (obs.getX() + obs.getWidth() < 0) {
                obs.setX(WIDTH);
            }
        }
    }
    
    // วาดเกมลงบน back buffer ของ BufferStrategy
    private void renderGame() {
        do {
            do {
                // ได้ Graphics สำหรับวาดลงบัฟเฟอร์
                Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
                try {
                    // เติมพื้นหลัง (สีขาว)
                    g.setColor(Color.WHITE);
                    g.fillRect(0, 0, WIDTH, HEIGHT);
                    // วาดผู้เล่นและสิ่งกีดขวางทั้งหมด
                    player.draw(g);
                    for (Obstacle obs : obstacles) {
                        obs.draw(g);
                    }
                } finally {
                    g.dispose();  // คืนทรัพยากร Graphics
                }
                // วนจนกว่าจะวาดได้สำเร็จ (กรณี buffer ถูก restore ใหม่)
            } while (bufferStrategy.contentsRestored());
            // โชว์ภาพที่วาดเสร็จแล้วบนหน้าจอ
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();   // flush ภาพ (จำเป็นบน Linux)
            // วน loop ถ้า buffer สูญหาย (เช่น หน้าต่างโดนย่อ/ขยาย)
        } while (bufferStrategy.contentsLost());
    }
    
    // การควบคุมแป้นพิมพ์
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        // กด Space หรือ ลูกศรขึ้น เพื่อกระโดด
        if ((code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) && player.isOnGround()) {
            player.jump();
        }
    }
    @Override public void keyReleased(KeyEvent e) { /* ไม่ใช้งาน */ }
    @Override public void keyTyped(KeyEvent e) { /* ไม่ใช้งาน */ }
}
