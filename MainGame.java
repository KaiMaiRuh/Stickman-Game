// MainGame.java - คลาสหลักสำหรับเริ่มโปรแกรม
import javax.swing.JFrame;
public class MainGame {
    public static void main(String[] args) {
        // สร้างหน้าต่างเกม
        JFrame frame = new JFrame("2D Stickman Game");
        GameCanvas canvas = new GameCanvas();
        frame.add(canvas);
        frame.pack();                           // ปรับขนาดเฟรมให้พอดีกับ Canvas
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);      // จัดตำแหน่งกลางจอ
        frame.setVisible(true);
        canvas.requestFocus();                  // ให้ canvas รับโฟกัสเพื่อรับการกดแป้นพิมพ์
        canvas.start();                         // เริ่มเกม loop
    }
}
