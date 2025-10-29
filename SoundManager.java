import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

class SoundManager {
    
    private static final double BGM_VOLUME = 0.7;
    private static final double SFX_VOLUME = 1.0;
    private Clip bgmClip;
    private Clip sfxClip;

    public void playBgmLoop(String path) {
        stopBgm();
        Clip clip = openClip(path);
        if (clip == null) return;
    setVolume(clip, BGM_VOLUME);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
        clip.start();
        bgmClip = clip;
    }

    public void stopBgm() {
        if (bgmClip != null) {
            try { bgmClip.stop(); } catch (Exception ignored) {}
            try { bgmClip.close(); } catch (Exception ignored) {}
            bgmClip = null;
        }
    }

    public void playSfx(String path) {
        
        if (sfxClip != null) {
            try { sfxClip.stop(); } catch (Exception ignored) {}
            try { sfxClip.close(); } catch (Exception ignored) {}
            sfxClip = null;
        }
        Clip clip = openClip(path);
        if (clip == null) return;
    setVolume(clip, SFX_VOLUME);
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                try { clip.close(); } catch (Exception ignored) {}
            }
        });
        clip.start();
        sfxClip = clip;
    }

    private Clip openClip(String path) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                System.out.println("SoundManager: File not found: " + path);
                return null;
            }
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            try { ais.close(); } catch (IOException ignored) {}
            return clip;
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.out.println("SoundManager: Failed to open '" + path + "': " + e.getMessage());
            return null;
        }
    }

    private void setVolume(Clip clip, double volume01) {
        try {
            FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            double v = Math.max(0.0001, Math.min(1.0, volume01));
            float dB = (float) (20.0 * Math.log10(v));
            dB = Math.max(control.getMinimum(), Math.min(control.getMaximum(), dB));
            control.setValue(dB);
        } catch (Exception ignored) {
            
        }
    }
}
