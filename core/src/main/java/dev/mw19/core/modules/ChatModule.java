package dev.mw19.core.modules;

import dev.mw19.api.Subscription;
import dev.mw19.api.event.KeyPressEvent;
import dev.mw19.api.module.Category;
import dev.mw19.api.module.Module;
import dev.mw19.api.module.Rule;
import dev.mw19.api.setting.BoolSetting;
import dev.mw19.api.setting.ChoiceSetting;
import dev.mw19.api.setting.ColorSetting;
import dev.mw19.api.setting.KeySetting;
import dev.mw19.api.setting.ListSetting;
import dev.mw19.core.Mw19;
import dev.mw19.core.chat.ChatLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Display-only chat tools: timestamps, stacking duplicates, filters, mention highlights, and search. Never sends chat. */
public final class ChatModule extends Module {
    public static final int HISTORY = 500;

    private final BoolSetting timestamps = add(new BoolSetting("timestamps", "Timestamps", "Prefix lines with the time", true));
    private final ChoiceSetting tsFormat = add(new ChoiceSetting("ts_format", "Time format", "Timestamp style", "HH:mm", "HH:mm", "HH:mm:ss", "h:mm"));
    private final BoolSetting compact = add(new BoolSetting("compact", "Stack duplicates", "Collapse repeated lines into one with (xN)", true));
    private final ListSetting filters = add(new ListSetting("filters", "Hide lines containing", "Words to hide (case-insensitive); prefix with re: for a regex",
            Collections.<String>emptyList(), 64));
    private final BoolSetting mentionName = add(new BoolSetting("mention_name", "Highlight my name", "Mark lines that mention you", true));
    private final ListSetting mentionWords = add(new ListSetting("mention_words", "Also highlight", "Extra words that mark a line", Collections.<String>emptyList(), 32));
    private final ColorSetting mentionColor = add(new ColorSetting("mention_color", "Highlight colour", "Marker colour for mentions", 0xFFFFC53D));
    private final BoolSetting mentionSound = add(new BoolSetting("mention_sound", "Ding on mention", "Play a local sound when you are mentioned", false));
    private final KeySetting searchKey = add(new KeySetting("search_key", "Search key", "Opens chat search", KeySetting.NONE));

    private final String[] history = new String[HISTORY];
    private final long[] historyTime = new long[HISTORY];
    private int head, size;
    private String lastPlain;
    private int lastRepeat;
    private List<Pattern> compiled = new ArrayList<Pattern>();
    private Subscription keySub;

    public ChatModule() {
        super("chat", "Chat Tools", "Timestamps, stacking, filters, highlights and search (display only)", Category.CHAT, Rule.ALLOWED, false);
        filters.addListener(new Runnable() {
            @Override
            public void run() {
                compile();
            }
        });
        compile();
    }

    private void compile() {
        List<Pattern> out = new ArrayList<Pattern>();
        for (String f : filters.get()) {
            if (f.trim().isEmpty()) continue;
            try {
                out.add(f.startsWith("re:") ? Pattern.compile(f.substring(3), Pattern.CASE_INSENSITIVE)
                        : Pattern.compile(Pattern.quote(f.trim()), Pattern.CASE_INSENSITIVE));
            } catch (PatternSyntaxException e) {
                // an invalid regex just does not filter
            }
        }
        compiled = out;
    }

    @Override
    public void onEnable() {
        keySub = Mw19.get().events.on(KeyPressEvent.class, new Consumer<KeyPressEvent>() {
            @Override
            public void accept(KeyPressEvent e) {
                if (searchKey.bound() && e.key == searchKey.key()) Mw19.get().gui().openChatSearch();
            }
        });
    }

    @Override
    public void onDisable() {
        if (keySub != null) keySub.cancel();
        lastPlain = null;
    }

    /** Game thread, for every incoming line (after plugins). */
    public void process(ChatLine line) {
        for (Pattern p : compiled) {
            if (p.matcher(line.plain).find()) {
                line.cancel = true;
                return;
            }
        }
        record(line.plain);
        line.track = compact.on();
        if (compact.on() && lastPlain != null && lastPlain.equals(line.plain)) {
            line.repeat = ++lastRepeat;
        } else {
            lastPlain = line.plain;
            lastRepeat = 1;
        }
        if (timestamps.on()) line.prefix = "§7[" + time() + "]§r ";
        if (mentions(line.plain)) {
            line.highlight = mentionColor.get();
            if (mentionSound.on() && line.repeat <= 1) Mw19.get().platform.playPing();
        }
    }

    private boolean mentions(String plain) {
        String lower = plain.toLowerCase(Locale.ROOT);
        if (mentionName.on()) {
            String me = Mw19.get().platform.playerName();
            // skip our own chat lines ("<name> ...") so you do not ping yourself
            if (me != null && !me.isEmpty() && lower.contains(me.toLowerCase(Locale.ROOT)) && !plain.startsWith("<" + me + ">")) return true;
        }
        for (String w : mentionWords.get()) if (!w.isEmpty() && lower.contains(w.toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private String time() {
        long now = System.currentTimeMillis();
        long local = now + TimeZone.getDefault().getOffset(now);
        long sec = local / 1000;
        int s = (int) (sec % 60), m = (int) (sec / 60 % 60), h = (int) (sec / 3600 % 24);
        if (tsFormat.is("h:mm")) return (h % 12 == 0 ? 12 : h % 12) + ":" + two(m);
        String t = two(h) + ":" + two(m);
        return tsFormat.is("HH:mm:ss") ? t + ":" + two(s) : t;
    }

    private static String two(int v) {
        return v < 10 ? "0" + v : Integer.toString(v);
    }

    private void record(String plain) {
        history[head] = plain;
        historyTime[head] = System.currentTimeMillis();
        head = (head + 1) % HISTORY;
        if (size < HISTORY) size++;
    }

    /** Newest first, lines containing {@code query} (case-insensitive). */
    public List<String[]> search(String query, int limit) {
        List<String[]> out = new ArrayList<String[]>();
        String q = query.toLowerCase(Locale.ROOT);
        for (int i = 0; i < size && out.size() < limit; i++) {
            int idx = (head - 1 - i + HISTORY) % HISTORY;
            String line = history[idx];
            if (line != null && (q.isEmpty() || line.toLowerCase(Locale.ROOT).contains(q))) {
                out.add(new String[]{line, Long.toString(historyTime[idx])});
            }
        }
        return out;
    }
}
