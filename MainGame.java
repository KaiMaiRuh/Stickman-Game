import javax.swing.JFrame;

public class MainGame {
    public static void main(String[] args) {
        
        
        JFrame frame = new JFrame("Stickman Runner Game");
        GameCanvas canvas = new GameCanvas();
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        
        
        canvas.requestFocusInWindow();
        canvas.start();
    }
}