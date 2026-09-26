package dev.mw19.core.modules;

import dev.mw19.api.Subscription;
import dev.mw19.api.event.KeyPressEvent;
import dev.mw19.api.game.Game;
import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.KeySetting;
import dev.mw19.core.Mw19;

import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;

/** After you take a screenshot: a toast plus keys to open the folder or copy the file path. Local only, no uploads. */
public final class ScreenshotModule extends Module {
    private final BoolSetting toast = add(new BoolSetting("toast", "Notify", "Show a toast with the file name", true));
    private final KeySetting openKey = add(new KeySetting("open_key", "Open folder key", "Opens the screenshots folder", KeySetting.NONE));
    private final KeySetting copyKey = add(new KeySetting("copy_key", "Copy path key", "Copies the last screenshot's path", KeySetting.NONE));

    private boolean wasDown;
    private int pending;
    private long pressedAt;
    private File last;
    private Subscription sub;

    public ScreenshotModule() {
        super("screenshot", "Screenshot Tools", "Toast and shortcuts after taking a screenshot", Category.UTILITY, Rule.ALLOWED, false);
    }

    @Override
    public void onEnable() {
        sub = Mw19.get().events.on(KeyPressEvent.class, new Consumer<KeyPressEvent>() {
            @Override
            public void accept(KeyPressEvent e) {
                Mw19 k = Mw19.get();
                if (openKey.bound() && e.key == openKey.key()) k.platform.openFolder(k.platform.screenshotsDir());
                if (copyKey.bound() && e.key == copyKey.key() && last != null) {
                    k.platform.setClipboard(last.getAbsolutePath());
                    k.toast("Screenshot", "Path copied", k.theme.good);
                }
            }
        });
    }

    @Override
    public void onDisable() {
        if (sub != null) sub.cancel();
    }

    @Override
    public void onTick() {
        Mw19 k = Mw19.get();
        boolean down = k.platform.bindingDown(Game.Binding.SCREENSHOT);
        if (down && !wasDown) {
            pending = 20; // the game writes the PNG on a worker thread; look shortly after
            pressedAt = System.currentTimeMillis();
        }
        wasDown = down;
        if (pending > 0 && --pending == 0) {
            File f = newest(k.platform.screenshotsDir());
            if (f != null && f.lastModified() >= pressedAt - 2000) {
                last = f;
                if (toast.on()) k.toast("Screenshot saved", f.getName(), k.theme.accent);
            }
        }
    }

    private static File newest(Path dir) {
        File[] files = dir.toFile().listFiles();
        File best = null;
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().endsWith(".png") && (best == null || f.lastModified() > best.lastModified())) best = f;
        }
        return best;
    }
}
