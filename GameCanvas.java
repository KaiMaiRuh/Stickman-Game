import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
 

public class GameCanvas extends Canvas implements Runnable, KeyListener {
    private enum GameState { MENU, RUNNING, GAME_OVER_MENU }
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 512;
    // Tunable gameplay/view constants
    public static final int GROUND_HEIGHT = 50;                // ground thickness used in layout
    private static final int PLAYER_TARGET_HEIGHT = 120;       // player render height
    private static final int JUMP_OBS_HEIGHT = 100;            // jump obstacle height
    private static final int SLIDE_OBS_HEIGHT = 140;           // slide obstacle height
    private static final int GIFT_HEIGHT = 70;                 // gift sprite height

    private static final int TARGET_FPS = 120;
    private static final long TARGET_FRAME_TIME_NS = 1_000_000_000L / TARGET_FPS;
    // Spawning and movement tuning
    private static final int MIN_SPAWN_GAP_PX = 400;   // ensure obstacles don't appear together at the right edge
    private static final double MIN_SPAWN_DELAY = 1.3; // seconds
    private static final double MAX_SPAWN_DELAY = 2.2; // seconds
    private static final double SLIDE_MIN_INTERVAL = 2.5; // seconds between slide obstacles
    private static final double SLIDE_SPAWN_PROB = 0.45;   // chance when eligible
    private static final double BG_SCROLL_SPEED = 60.0;    // background scroll px/sec
    private static final double OBSTACLE_BASE_SPEED = -5.0;    // px/update frame baseline (will be scaled by dt in update())
    private static final double OBSTACLE_SPEED_VARIANCE = -2.0; // additional negative speed (0..-2)
    private static final double GIFT_BASE_SPEED = -4.5;
    private static final double GIFT_SPEED_VARIANCE = -1.5;
    private static final double RECYCLE_JITTER_MIN_RATIO = 0.25; // as fraction of MIN_SPAWN_GAP_PX
    private static final double RECYCLE_JITTER_MAX_RATIO = 0.75;
    // Gift spawning
    private static final double GIFT_CHECK_INTERVAL = 1.0; // seconds
    private static final int MAX_SIMULT_GIFTS = 2;
    private static final double GIFT1_PROB = 0.4; // per check
    private static final double GIFT2_PROB = 0.1; // per check

    private Thread gameThread;
    private volatile boolean running = false;
    private BufferStrategy bufferStrategy;
    
    private Player player;
    private final List<Obstacle> obstacles = new ArrayList<>();
    private final List<Gift> gifts = new ArrayList<>();
    private final Random random = new Random();
    private GameState state = GameState.MENU;
    
    private int score = 0;

    private double spawnTimer = 0;
    private double nextSpawnDelay = 1.5; 

    private BufferedImage playerImage;
    private BufferedImage obstacleImage;
    private BufferedImage[] jumpObstacleImages;
    private BufferedImage[] slideObstacleImages;
    private BufferedImage gift1Image;
    private BufferedImage gift2Image;
    private BufferedImage[] playerRunFrames;
    private BufferedImage[] playerJumpFrames;
    private BufferedImage[] playerSlideFrames;
    private BufferedImage[] playerIdleFrames;
    private int idleIndex = 0;
    private int idleTicker = 0;
    private int idleFramesPerStep = 14;
    private BufferedImage backgroundImage;
    private double bgOffset = 0.0; 
    private double timeSinceLastSlide = 999.0;
    private double giftSpawnTimer = 0.0;
    private boolean gift1Active = false;
    private double gift1Timer = 0.0;
    private static final double GIFT1_DURATION = 10.0;
    private boolean gift2Active = false;
    private double gift2Timer = 0.0;
    private static final double GIFT2_DURATION = 5.0;
    private static final double GIFT2_SPEED_MULTIPLIER = 2.0;
    private double obstacleSpeedMultiplier = 1.0;
    
    // Menu assets
    private BufferedImage startButtonImage;
    private BufferedImage exitButtonImage;
    private BufferedImage tryAgainButtonImage;
    
    // Sound
    private final SoundManager sound = new SoundManager();

        public GameCanvas() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setIgnoreRepaint(true);
        addKeyListener(this);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMousePressed(e);
            }
        });
        setFocusable(true);

    loadImages();

        double playerX = 100;
        int baseHeight = 60;
        int playerHeight = baseHeight;
        if (playerRunFrames != null && playerRunFrames.length > 0) {
            playerHeight = playerRunFrames[0].getHeight();
        } else if (playerImage != null) {
            playerHeight = playerImage.getHeight();
        }
    double playerY = HEIGHT - playerHeight - GROUND_HEIGHT; 
        if (playerRunFrames != null && playerRunFrames.length > 0) {
            player = new Player(playerX, playerY, playerRunFrames, playerJumpFrames, playerSlideFrames);
        } else {
            player = new Player(playerX, playerY, playerImage);
        }
        // Start menu BGM
    sound.playBgmLoop("Sound/Menu.wav");
    }

        public synchronized void start() {
        if (running) {
            return;
        }
        createBufferStrategy(2);
        bufferStrategy = getBufferStrategy();
        running = true;
        gameThread = new Thread(this, "GameThread");
        gameThread.setPriority(Thread.MAX_PRIORITY);
        gameThread.start();
    }

        @Override
    public void run() {
        long lastTime = System.nanoTime();
        while (running) {
            long frameStart = System.nanoTime();
            double dt = (frameStart - lastTime) / 1_000_000_000.0;
            lastTime = frameStart;

            updateGame(dt);
            renderGame();

            long frameEnd = System.nanoTime();
            long elapsed = frameEnd - frameStart;
            long sleepTime = TARGET_FRAME_TIME_NS - elapsed;
            if (sleepTime > 0) {
                try {
                    
                    long ms = sleepTime / 1_000_000L;
                    int ns = (int) (sleepTime % 1_000_000L);
                    Thread.sleep(ms, ns);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

        private void loadImages() {
        try {
            
            
            
            java.util.List<BufferedImage> frames = new java.util.ArrayList<>();
            for (int i = 1; i <= 8; i++) {
                File f = new File("Player/Run/Run" + i + ".png");
                if (f.exists()) {
                    BufferedImage raw = ImageIO.read(f);
                    frames.add(scaleToHeight(raw, PLAYER_TARGET_HEIGHT));
                }
            }
            if (!frames.isEmpty()) {
                playerRunFrames = frames.toArray(new BufferedImage[0]);
            } else {
                
                File playerFile = new File("Stickman.png");
                if (playerFile.exists()) {
                    BufferedImage raw = ImageIO.read(playerFile);
                    playerImage = scaleToHeight(raw, PLAYER_TARGET_HEIGHT);
                }
            }

            
            java.util.List<BufferedImage> jump = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                File f = new File("Player/Jump/Jump" + i + ".png");
                if (f.exists()) {
                    BufferedImage raw = ImageIO.read(f);
                    jump.add(scaleToHeight(raw, PLAYER_TARGET_HEIGHT));
                }
            }
            if (!jump.isEmpty()) {
                playerJumpFrames = jump.toArray(new BufferedImage[0]);
            }

            
            java.util.List<BufferedImage> slide = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                File f = new File("Player/Slide/Slide" + i + ".png");
                if (f.exists()) {
                    BufferedImage raw = ImageIO.read(f);
                    slide.add(scaleToHeight(raw, PLAYER_TARGET_HEIGHT));
                }
            }
            if (!slide.isEmpty()) {
                playerSlideFrames = slide.toArray(new BufferedImage[0]);
            }
            
            // Idle frames
            java.util.List<BufferedImage> idle = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                File f = new File("Player/Idle/Idle" + i + ".png");
                if (f.exists()) {
                    BufferedImage raw = ImageIO.read(f);
                    idle.add(scaleToHeight(raw, PLAYER_TARGET_HEIGHT));
                }
            }
            if (idle.isEmpty()) {
                // try common single-file names (case-insensitive variants)
                String[] candidates = new String[] {"Player/Idle/IDLE.png", "Player/Idle/Idle.png", "Player/Idle/idle.png", "Player/Idle/IDLE.PNG"};
                for (String p : candidates) {
                    File f = new File(p);
                    if (f.exists()) {
                        BufferedImage raw = ImageIO.read(f);
                        idle.add(scaleToHeight(raw, PLAYER_TARGET_HEIGHT));
                        break;
                    }
                }
            }
            if (!idle.isEmpty()) {
                playerIdleFrames = idle.toArray(new BufferedImage[0]);
            }

            
            
            
            File obstacleFile = new File("obstacle.png");
            if (obstacleFile.exists()) {
                BufferedImage raw = ImageIO.read(obstacleFile);
                obstacleImage = scaleToHeight(raw, 100);
            }

            java.util.List<BufferedImage> jumpObs = new java.util.ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                File jf = new File("Obstacle/Jump Obstacle/Jump Obstacle" + i + ".png");
                if (!jf.exists() && i == 1) {
                    jf = new File("Obstacle/Jump Obstacle/Jump Obstable1.png");
                }
                if (jf.exists()) {
                    BufferedImage raw = ImageIO.read(jf);
                    jumpObs.add(scaleToHeightAllowUpscale(raw, JUMP_OBS_HEIGHT));
                }
            }
            if (!jumpObs.isEmpty()) {
                jumpObstacleImages = jumpObs.toArray(new BufferedImage[0]);
            }

            java.util.List<BufferedImage> slideObs = new java.util.ArrayList<>();
            for (int i = 1; i <= 3; i++) {
                File sf = new File("Obstacle/Slide Obstacle/Slide Obstacle" + i + ".png");
                if (sf.exists()) {
                    BufferedImage raw = ImageIO.read(sf);
                    slideObs.add(scaleToHeightAllowUpscale(raw, SLIDE_OBS_HEIGHT));
                }
            }
            if (!slideObs.isEmpty()) {
                slideObstacleImages = slideObs.toArray(new BufferedImage[0]);
            }
            
            // Gifts
            File g1 = new File("Gift/Gift1.png");
            if (g1.exists()) {
                BufferedImage raw = ImageIO.read(g1);
                gift1Image = scaleToHeightAllowUpscale(raw, GIFT_HEIGHT);
            }
            File g2 = new File("Gift/Gift2.png");
            if (g2.exists()) {
                BufferedImage raw = ImageIO.read(g2);
                gift2Image = scaleToHeightAllowUpscale(raw, GIFT_HEIGHT);
            }
            
            File bgFile = new File("Background.png");
            if (!bgFile.exists()) {
                bgFile = new File("Background.jpg");
            }
            if (bgFile.exists()) {
                BufferedImage rawBg = ImageIO.read(bgFile);
                backgroundImage = scaleToHeight(rawBg, HEIGHT);
            }
            
            // Menu buttons (optional)
            File sb = new File("START.png");
            if (sb.exists()) {
                startButtonImage = ImageIO.read(sb);
            }
            File eb = new File("EXIT.png");
            if (eb.exists()) {
                exitButtonImage = ImageIO.read(eb);
            }
            // Try Again (support a few common names)
            File tab = new File("TRY AGAIN.png");
            if (!tab.exists()) tab = new File("Try Again.png");
            if (!tab.exists()) tab = new File("TRY_AGAIN.png");
            if (tab.exists()) {
                tryAgainButtonImage = ImageIO.read(tab);
            }
        } catch (IOException e) {
            
            System.err.println("Failed to load images: " + e.getMessage());
            playerImage = null;
            obstacleImage = null;
        }
    }

        private static BufferedImage scaleToHeight(BufferedImage src, int targetHeight) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        if (srcHeight <= targetHeight) {
            return src;
        }
        double scale = (double) targetHeight / srcHeight;
        int targetWidth = (int) Math.round(srcWidth * scale);
        Image scaled = src.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
        BufferedImage dest = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = dest.createGraphics();
        g2.drawImage(scaled, 0, 0, null);
        g2.dispose();
        return dest;
    }
        private static BufferedImage scaleToHeightAllowUpscale(BufferedImage src, int targetHeight) {
            int srcWidth = src.getWidth();
            int srcHeight = src.getHeight();
            double scale = (double) targetHeight / srcHeight;
            int targetWidth = (int) Math.round(srcWidth * scale);
            Image scaled = src.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
            BufferedImage dest = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = dest.createGraphics();
            g2.drawImage(scaled, 0, 0, null);
            g2.dispose();
            return dest;
        }
        private void updateGame(double dt) {
        
    if (state != GameState.RUNNING) {
        updateIdleAnim();
        return;
    }

    player.update();
    timeSinceLastSlide += dt;
    // Background scroll only while running
    if (backgroundImage != null) {
        bgOffset += BG_SCROLL_SPEED * dt;
        double bw = backgroundImage.getWidth();
        if (bw > 0) {
            bgOffset = bgOffset % bw;
        }
    }
    
    // Handle Gift1 timer expiration
    if (gift1Active) {
        gift1Timer -= dt;
        if (gift1Timer <= 0) {
            gift1Active = false;
            gift1Timer = 0.0;
            player.resetJumpPhysics();
        }
    }

    // Handle Gift2 timer expiration
    if (gift2Active) {
        gift2Timer -= dt;
        if (gift2Timer <= 0) {
            // deactivate and wipe all current obstacles
            gift2Active = false;
            gift2Timer = 0.0;
            obstacleSpeedMultiplier = 1.0;
            player.setRunAnimSpeedMultiplier(1.0);
            obstacles.clear();
            // reset spawn timing so new obstacles start after a brief delay
            spawnTimer = 0.0;
            nextSpawnDelay = MIN_SPAWN_DELAY + random.nextDouble() * (MAX_SPAWN_DELAY - MIN_SPAWN_DELAY);
        }
    }

        // Use a rolling rightmost tracker so multiple recycled obstacles don't get stacked at the same X
        double rollingRightmost = getRightmostObstacleRight();
        Iterator<Obstacle> iter = obstacles.iterator();
        while (iter.hasNext()) {
            Obstacle obs = iter.next();
            obs.update();

            if (player.getBounds().intersects(obs.getBounds())) {
                if (!gift2Active) {
                    gameOver();
                    return;
                }
            }

            if (obs.getX() + obs.getWidth() < 0) {
                score++;
                // Recycle this obstacle with enforced horizontal spacing from the rolling rightmost
                double baseSpawn = Math.max(WIDTH, rollingRightmost + MIN_SPAWN_GAP_PX);
                double jitter = MIN_SPAWN_GAP_PX * (RECYCLE_JITTER_MIN_RATIO + random.nextDouble() * (RECYCLE_JITTER_MAX_RATIO - RECYCLE_JITTER_MIN_RATIO));
                double newX = baseSpawn + jitter;
                obs.setX(newX);
                // advance rollingRightmost so subsequent recycled obstacles are placed further to the right
                rollingRightmost = Math.max(rollingRightmost, newX + obs.getWidth());
            }
        }
        
        spawnTimer += dt;
            if (spawnTimer >= nextSpawnDelay) {
                if (hasSpawnClearance()) {
                    spawnTimer = 0;
                    nextSpawnDelay = MIN_SPAWN_DELAY + random.nextDouble() * (MAX_SPAWN_DELAY - MIN_SPAWN_DELAY);
                    spawnObstacle();
                } else {
                    // hold the timer here and re-check each frame until the clearance is available
                    spawnTimer = nextSpawnDelay;
                }
            }

        // Gifts update and collisions
        Iterator<Gift> git = gifts.iterator();
        while (git.hasNext()) {
            Gift gift = git.next();
            gift.update();
            // collect
            if (!gift.isCollected() && player.getBounds().intersects(gift.getBounds())) {
                gift.markCollected();
                // apply effects
                if (gift.getType() == Gift.Type.GIFT1) {
                    // Better jump for limited time: lighter gravity and stronger upward velocity
                    gift1Active = true;
                    gift1Timer = GIFT1_DURATION; // refresh if already active
                    player.applyGift1JumpBoost(0.3, -12.0);
                } else if (gift.getType() == Gift.Type.GIFT2) {
                    // Speed boost and phase through obstacles for 5s
                    if (!gift2Active) {
                        gift2Active = true;
                        gift2Timer = GIFT2_DURATION;
                        obstacleSpeedMultiplier = GIFT2_SPEED_MULTIPLIER;
                        player.setRunAnimSpeedMultiplier(3.0);
                        for (Obstacle o : obstacles) {
                            o.setSpeedX(o.getSpeedX() * GIFT2_SPEED_MULTIPLIER);
                        }
                    } else {
                        // refresh timer if collected while active
                        gift2Timer = GIFT2_DURATION;
                    }
                }
            }
            // remove conditions
            if (gift.isCollected() || gift.getX() + gift.getWidth() < 0) {
                git.remove();
            }
        }

        // Gifts spawning check
        giftSpawnTimer += dt;
        if (giftSpawnTimer >= GIFT_CHECK_INTERVAL) {
            giftSpawnTimer -= GIFT_CHECK_INTERVAL;
            if (gifts.size() < MAX_SIMULT_GIFTS && hasSpawnClearance()) {
                double r = random.nextDouble();
                if (r < GIFT2_PROB && gift2Image != null) {
                    spawnGift(Gift.Type.GIFT2);
                } else if (r < (GIFT2_PROB + GIFT1_PROB) && gift1Image != null) {
                    spawnGift(Gift.Type.GIFT1);
                }
            }
        }

        // background already updated above
    }

    private boolean hasSpawnClearance() {
        // Ensure the rightmost obstacle's right edge is at least MIN_SPAWN_GAP_PX away from the spawn edge (WIDTH)
        double rightmost = getRightmostObstacleRight();
        return rightmost < (WIDTH - MIN_SPAWN_GAP_PX);
    }

    private double getRightmostObstacleRight() {
        double rightmost = Double.NEGATIVE_INFINITY;
        for (Obstacle o : obstacles) {
            rightmost = Math.max(rightmost, o.getX() + o.getWidth());
        }
        if (rightmost == Double.NEGATIVE_INFINITY) {
            return 0.0;
        }
        return rightmost;
    }

        private void spawnObstacle() {
    double x = WIDTH;
    double y;
    int desiredObstacleHeight;

    boolean canSpawnSlide = timeSinceLastSlide >= SLIDE_MIN_INTERVAL && slideObstacleImages != null && slideObstacleImages.length > 0;
    boolean spawnSlide = canSpawnSlide && random.nextDouble() < SLIDE_SPAWN_PROB;

        BufferedImage chosenImage;
        if (spawnSlide) {
            chosenImage = slideObstacleImages[random.nextInt(slideObstacleImages.length)];
            desiredObstacleHeight = SLIDE_OBS_HEIGHT;
            // Place slide obstacles so they block the upper body when standing but clear the lower body when sliding.
            // Compute player's top Y when standing, then position obstacle so its bottom sits around the mid-chest area.
            double standingTopY = HEIGHT - GROUND_HEIGHT - PLAYER_TARGET_HEIGHT;
            double slideBottomTarget = standingTopY + (PLAYER_TARGET_HEIGHT * 0.45); // target bottom around 45% down from head
            y = slideBottomTarget - desiredObstacleHeight;
            if (y < 0) y = 0;
        } else {
            if (jumpObstacleImages != null && jumpObstacleImages.length > 0) {
                chosenImage = jumpObstacleImages[random.nextInt(jumpObstacleImages.length)];
            } else {
                chosenImage = obstacleImage;
            }
            desiredObstacleHeight = JUMP_OBS_HEIGHT;
            int imgH = (chosenImage != null) ? desiredObstacleHeight : 60;
            y = HEIGHT - imgH - GROUND_HEIGHT;
        }

    double speedX = (OBSTACLE_BASE_SPEED + random.nextDouble() * OBSTACLE_SPEED_VARIANCE) * obstacleSpeedMultiplier; 
    Obstacle obs = new Obstacle(x, y, chosenImage, speedX, desiredObstacleHeight);
        obstacles.add(obs);
        if (spawnSlide) {
            timeSinceLastSlide = 0.0;
        }
    }

    private void spawnGift(Gift.Type type) {
        double x = WIDTH;
        BufferedImage img = (type == Gift.Type.GIFT1) ? gift1Image : gift2Image;
        int gH = (img != null) ? img.getHeight() : 24;
        // place above ground to encourage jumping
        double clearance = 100; // pixels above ground
    double y = HEIGHT - GROUND_HEIGHT - clearance - gH;
        y = Math.max(0, y);
    double speedX = (GIFT_BASE_SPEED + random.nextDouble() * GIFT_SPEED_VARIANCE) * obstacleSpeedMultiplier;
        Gift gift = new Gift(x, y, img, speedX, type);
        gifts.add(gift);
    }

        private void renderGame() {
        do {
            do {
                Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
                try {
                    
                    
                    if (backgroundImage != null) {
                        int bw = backgroundImage.getWidth();
                        if (bw <= 0) {
                            g.setColor(Color.WHITE);
                            g.fillRect(0, 0, WIDTH, HEIGHT);
                        } else {
                            
                            double startX = - (bgOffset % bw);
                            for (int i = 0; i <= WIDTH / bw + 1; i++) {
                                int drawX = (int) Math.round(startX + i * bw);
                                g.drawImage(backgroundImage, drawX, 0, null);
                            }
                        }
                    } else {
                        g.setColor(Color.WHITE);
                        g.fillRect(0, 0, WIDTH, HEIGHT);
                    }

                    
                    g.setColor(new Color(220, 220, 220));
                    g.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);

                    if (state == GameState.RUNNING) {
                        player.draw(g);
                        for (Obstacle obs : obstacles) obs.draw(g);
                        for (Gift gift : gifts) gift.draw(g);
                        g.setColor(Color.BLACK);
                        g.setFont(g.getFont().deriveFont(18f));
                        g.drawString("Score: " + score, 10, 20);
                        // Small HUD for active gift timers
                        int hudY = 40;
                        g.setFont(g.getFont().deriveFont(16f));
                        if (gift1Active) {
                            g.drawString("G1: " + formatSeconds(gift1Timer), 10, hudY);
                            hudY += 20;
                        }
                        if (gift2Active) {
                            g.drawString("G2: " + formatSeconds(gift2Timer), 10, hudY);
                        }
                    } else {
                        drawIdlePlayer(g);
                        drawMenu(g, state == GameState.GAME_OVER_MENU);
                    }
                } finally {
                    g.dispose();
                }
                
                
            } while (bufferStrategy.contentsRestored());
            
            bufferStrategy.show();
            Toolkit.getDefaultToolkit().sync();
            
            
        } while (bufferStrategy.contentsLost());
    }

        private void gameOver() {
        // Switch to game-over menu
        state = GameState.GAME_OVER_MENU;
        obstacles.clear();
        gifts.clear();
            gift1Active = false;
            gift1Timer = 0.0;
        gift2Active = false;
        gift2Timer = 0.0;
        obstacleSpeedMultiplier = 1.0;
        player.resetJumpPhysics();
            player.setRunAnimSpeedMultiplier(1.0);
            // Sounds: lose sfx then menu bgm
        sound.playSfx("Sound/Lose.wav");
        sound.playBgmLoop("Sound/Menu.wav");
    }

    
    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        if (state != GameState.RUNNING) return;

        if ((code == KeyEvent.VK_SPACE || code == KeyEvent.VK_UP) && player.isOnGround()) {
            player.jump();
        }
        
        
        
        if (code == KeyEvent.VK_DOWN) {
            player.startSlide();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        if (state != GameState.RUNNING) return;
        if (code == KeyEvent.VK_DOWN) {
            player.endSlide();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    // ---- Menu helpers ----
    private void updateIdleAnim() {
        if (playerIdleFrames == null || playerIdleFrames.length == 0) return;
        idleTicker++;
        if (idleTicker >= idleFramesPerStep) {
            idleTicker = 0;
            idleIndex = (idleIndex + 1) % playerIdleFrames.length;
        }
    }

    private void drawIdlePlayer(Graphics2D g) {
        BufferedImage frame = null;
        if (playerIdleFrames != null && playerIdleFrames.length > 0) {
            frame = playerIdleFrames[idleIndex];
        } else if (playerRunFrames != null && playerRunFrames.length > 0) {
            frame = playerRunFrames[0];
        }
        if (frame != null) {
            int ph = frame.getHeight();
            double py = HEIGHT - GROUND_HEIGHT - ph;
            double px = WIDTH * 0.15;
            g.drawImage(frame, (int)Math.round(px), (int)Math.round(py), frame.getWidth(), frame.getHeight(), null);
        }
    }

    private void drawMenu(Graphics2D g, boolean isGameOver) {
        int btnW = 260;
        int btnH = 90;
        Point startPos = getButtonPosition(true);
        Point exitPos = getButtonPosition(false);

        // START or TRY AGAIN
        if (!isGameOver) {
            if (startButtonImage != null) {
                g.drawImage(startButtonImage, startPos.x, startPos.y, btnW, btnH, null);
            } else {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(startPos.x, startPos.y, btnW, btnH, 20, 20);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(28f));
                drawCenteredText(g, "START", startPos.x, startPos.y, btnW, btnH);
            }
        } else {
            if (tryAgainButtonImage != null) {
                g.drawImage(tryAgainButtonImage, startPos.x, startPos.y, btnW, btnH, null);
            } else {
                g.setColor(new Color(0, 0, 0, 120));
                g.fillRoundRect(startPos.x, startPos.y, btnW, btnH, 20, 20);
                g.setColor(Color.WHITE);
                g.setFont(g.getFont().deriveFont(28f));
                drawCenteredText(g, "TRY AGAIN", startPos.x, startPos.y, btnW, btnH);
            }
        }

        // EXIT
        if (exitButtonImage != null) {
            g.drawImage(exitButtonImage, exitPos.x, exitPos.y, btnW, btnH, null);
        } else {
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(exitPos.x, exitPos.y, btnW, btnH, 20, 20);
            g.setColor(Color.WHITE);
            g.setFont(g.getFont().deriveFont(28f));
            drawCenteredText(g, "EXIT", exitPos.x, exitPos.y, btnW, btnH);
        }

        if (isGameOver) {
            g.setColor(Color.BLACK);
            g.setFont(g.getFont().deriveFont(24f));
            g.drawString("Score: " + score, 10, 30);
        }
    }

    private void drawCenteredText(Graphics2D g, String text, int x, int y, int w, int h) {
        java.awt.FontMetrics fm = g.getFontMetrics();
        int tx = x + (w - fm.stringWidth(text)) / 2;
        int ty = y + (h - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(text, tx, ty);
    }

    private Point getButtonPosition(boolean isStart) {
        int btnW = 260;
        int btnH = 90;
        int centerX = WIDTH / 2 - btnW / 2;
        int startY = (int)(HEIGHT * 0.45);
        int exitY = startY + btnH + 40;
        return new Point(centerX, isStart ? startY : exitY);
    }

    private java.awt.Rectangle getStartButtonBounds() {
        Point p = getButtonPosition(true);
        return new java.awt.Rectangle(p.x, p.y, 260, 90);
    }
    private java.awt.Rectangle getExitButtonBounds() {
        Point p = getButtonPosition(false);
        return new java.awt.Rectangle(p.x, p.y, 260, 90);
    }

    private void handleMousePressed(MouseEvent e) {
        if (state == GameState.RUNNING) return;
        int mx = e.getX();
        int my = e.getY();
        if (getStartButtonBounds().contains(mx, my)) {
            sound.playSfx("Sound/Start.wav");
            startNewRun();
        } else if (getExitButtonBounds().contains(mx, my)) {
            System.exit(0);
        }
    }

    private void startNewRun() {
        obstacles.clear();
        gifts.clear();
        score = 0;
        spawnTimer = 0;
        nextSpawnDelay = MIN_SPAWN_DELAY + random.nextDouble() * (MAX_SPAWN_DELAY - MIN_SPAWN_DELAY);
        timeSinceLastSlide = 999.0;
        giftSpawnTimer = 0.0;
        gift1Active = false;
        gift1Timer = 0.0;
        gift2Active = false;
        gift2Timer = 0.0;
        obstacleSpeedMultiplier = 1.0;
        player.resetJumpPhysics();
        player.setRunAnimSpeedMultiplier(1.0);
        // reposition player on ground
        int pH = (playerRunFrames != null && playerRunFrames.length > 0) ? playerRunFrames[0].getHeight() : player.getHeight();
    double pY = HEIGHT - pH - GROUND_HEIGHT;
        player.setY(pY);
        state = GameState.RUNNING;
    sound.playBgmLoop("Sound/Running.wav");
    }

    private static String formatSeconds(double seconds) {
        if (seconds < 0) seconds = 0;
        // Show one decimal place, e.g., 4.3s
        return String.format("%.1fs", seconds);
    }
}