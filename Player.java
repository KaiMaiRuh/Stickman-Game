import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

class Player extends Sprite {
    private double velocityY = 0;
    private boolean onGround = true;
    private boolean sliding = false;

    private final int originalHeight;
    private final int originalWidth;

    private BufferedImage[] runFrames;
    private int frameIndex = 0;
    private int frameTicker = 0;
    
    private static final int RUN_FRAMES_PER_STEP = 16;
    private static final int JUMP_FRAMES_PER_STEP = 12;
    private static final int SLIDE_FRAMES_PER_STEP = 8;

    private int framesPerStep = RUN_FRAMES_PER_STEP;
    private double runAnimSpeedMultiplier = 1.0;

    private BufferedImage[] jumpFrames;
    private int jumpIndex = 0;
    private int jumpTicker = 0;
    private int jumpFramesPerStep = JUMP_FRAMES_PER_STEP;
    private boolean jumpAnimPlaying = false;

    private BufferedImage[] slideFrames;
    private int slideIndex = 0;
    private int slideTicker = 0;
    private int slideFramesPerStep = SLIDE_FRAMES_PER_STEP;
    private int slideDownCount = 3;
    private boolean slideAnimActive = false;
    private boolean slideReleasePlaying = false;

    private static final double SLIDE_VISUAL_SCALE = 0.4;
    
    private static final double DEFAULT_GRAVITY_ACC = 0.4;
    private static final double DEFAULT_JUMP_VELOCITY = -11.0;
    private double gravityAcc = DEFAULT_GRAVITY_ACC;
    private double jumpVelocity = DEFAULT_JUMP_VELOCITY;

    public Player(double x, double y, BufferedImage image) {
        super(x, y, image);
        this.originalWidth = this.width;
        this.originalHeight = this.height;
    }

    public Player(double x, double y, BufferedImage[] frames) {
        super(x, y, (frames != null && frames.length > 0) ? frames[0] : null);
        this.runFrames = (frames != null && frames.length > 0) ? frames.clone() : null;

        if (this.image != null) {
            this.width = this.image.getWidth();
            this.height = this.image.getHeight();
        } else {
            this.width = 40;
            this.height = 60;
        }
        this.originalWidth = this.width;
        this.originalHeight = this.height;
    }

    public Player(double x, double y, BufferedImage[] runFrames, BufferedImage[] jumpFrames) {
        this(x, y, runFrames);
        this.jumpFrames = (jumpFrames != null && jumpFrames.length > 0) ? jumpFrames.clone() : null;
    }

    public Player(double x, double y, BufferedImage[] runFrames, BufferedImage[] jumpFrames, BufferedImage[] slideFrames) {
        this(x, y, runFrames, jumpFrames);
        this.slideFrames = (slideFrames != null && slideFrames.length > 0) ? slideFrames.clone() : null;
        if (this.slideFrames != null) {
            slideDownCount = Math.min(slideDownCount, this.slideFrames.length);
        }
    }

    @Override
    public void update() {
        if (!onGround) {
            velocityY += gravityAcc;
        }

        y += velocityY;

    double groundY = GameCanvas.HEIGHT - GameCanvas.GROUND_HEIGHT - height;
        if (y >= groundY) {
            y = groundY;
            velocityY = 0;
            onGround = true;
        }

        
        if (jumpAnimPlaying && jumpFrames != null && jumpFrames.length > 0) {
            jumpTicker++;
            if (jumpTicker >= jumpFramesPerStep) {
                jumpTicker = 0;
                jumpIndex++;
                if (jumpIndex >= jumpFrames.length) {
                    jumpAnimPlaying = false;
                    jumpIndex = 0;
                    frameIndex = 0;
                    if (runFrames != null && runFrames.length > 0) {
                        image = runFrames[0];
                        width = image.getWidth();
                        if (!sliding) {
                            height = image.getHeight();
                        }
                    }
                } else {
                    image = jumpFrames[jumpIndex];
                    width = image.getWidth();
                    if (!sliding) {
                        height = image.getHeight();
                    }
                }
            }
        } else if (slideAnimActive && slideFrames != null && slideFrames.length > 0) {
            slideTicker++;
            if (slideTicker >= slideFramesPerStep) {
                slideTicker = 0;
                if (!slideReleasePlaying) {
                    if (slideIndex + 1 < slideDownCount) {
                        slideIndex++;
                    }
                } else {
                    if (slideIndex + 1 < slideFrames.length) {
                        slideIndex++;
                    } else {
                        slideAnimActive = false;
                        slideReleasePlaying = false;
                        sliding = false;
                        slideIndex = 0;
                        if (runFrames != null && runFrames.length > 0) {
                            frameIndex = 0;
                            image = runFrames[0];
                            width = image.getWidth();
                            height = image.getHeight();
                        }
                    }
                }
                if (slideAnimActive) {
                    image = slideFrames[slideIndex];
                    width = image.getWidth();
                    height = image.getHeight();
                }
            }
        } else if (runFrames != null && runFrames.length > 0) {
            int runStep = Math.max(1, (int) Math.round(framesPerStep / Math.max(0.1, runAnimSpeedMultiplier)));
            frameTicker++;
            if (frameTicker >= runStep) {
                frameTicker = 0;
                frameIndex = (frameIndex + 1) % runFrames.length;
                image = runFrames[frameIndex];
                width = image.getWidth();
                height = image.getHeight();
            }
        }
    }

    public void jump() {
        
        if (onGround || sliding) {
            
            sliding = false;
            slideAnimActive = false;
            slideReleasePlaying = false;

            velocityY = jumpVelocity;
            onGround = false;
            if (jumpFrames != null && jumpFrames.length > 0) {
                jumpAnimPlaying = true;
                jumpIndex = 0;
                jumpTicker = 0;
                image = jumpFrames[0];
                width = image.getWidth();
                if (!sliding) {
                    height = image.getHeight();
                }
            }
        }
    }

    public void startSlide() {
        if (!sliding) {
            sliding = true;
            if (slideFrames != null && slideFrames.length > 0) {
                slideAnimActive = true;
                slideReleasePlaying = false;
                slideIndex = 0;
                slideTicker = 0;
                image = slideFrames[0];
                width = image.getWidth();
                height = image.getHeight();
                
            } else {
                if (onGround) {
                    height = originalHeight / 2;
                    y += originalHeight / 2;
                } else {
                    
                    height = originalHeight / 2;
                }
            }
        }
    }

    public void endSlide() {
        if (sliding) {
            if (slideFrames != null && slideFrames.length > 0) {
                slideReleasePlaying = true;
                slideIndex = Math.max(slideDownCount - 1, 0);
            } else {
                y -= originalHeight / 2;
                height = (image != null) ? image.getHeight() : originalHeight;
                sliding = false;
            }
        }
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void applyGift1JumpBoost(double newGravityAcc, double newJumpVelocity) {
        this.gravityAcc = newGravityAcc;
        this.jumpVelocity = newJumpVelocity;
    }

    public void resetJumpPhysics() {
        
        this.gravityAcc = DEFAULT_GRAVITY_ACC;
        this.jumpVelocity = DEFAULT_JUMP_VELOCITY;
    }

    public void setRunAnimSpeedMultiplier(double m) {
        this.runAnimSpeedMultiplier = m;
    }
    @Override
    public void draw(Graphics2D g) {
        if (slideAnimActive && slideFrames != null && slideFrames.length > 0) {
            int drawW = originalWidth;
            int drawH = Math.max(1, (int) Math.round(originalHeight * SLIDE_VISUAL_SCALE));
            int drawX = (int) Math.round(x);
            int drawY = (int) Math.round(y + originalHeight - drawH);
            BufferedImage toDraw = (image != null) ? image : slideFrames[Math.max(0, Math.min(slideIndex, slideFrames.length - 1))];
            g.drawImage(toDraw, drawX, drawY, drawW, drawH, null);
        } else if (image != null) {
            g.drawImage(image, (int) Math.round(x), (int) Math.round(y), width, height, null);
        } else {
            g.setColor(Color.BLUE);
            g.fillRect((int) Math.round(x), (int) Math.round(y), width, height);
        }
    }

    @Override
    public Rectangle getBounds() {
        if (slideAnimActive && slideFrames != null && slideFrames.length > 0) {
            int drawW = originalWidth;
            int drawH = Math.max(1, (int) Math.round(originalHeight * SLIDE_VISUAL_SCALE));
            int drawX = (int) Math.round(x);
            int drawY = (int) Math.round(y + originalHeight - drawH);
            return new Rectangle(drawX, drawY, drawW, drawH);
        }
        return super.getBounds();
    }
}
