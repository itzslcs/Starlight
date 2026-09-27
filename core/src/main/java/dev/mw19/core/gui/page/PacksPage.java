package dev.mw19.core.gui.page;

import dev.mw19.api.net.Http;
import dev.mw19.api.util.Colors;
import dev.mw19.core.gui.GuiRoot;
import dev.mw19.core.gui.Ui;
import dev.mw19.core.gui.Widget;
import dev.mw19.core.gui.widget.Button;
import dev.mw19.core.gui.widget.ScrollList;
import dev.mw19.core.gui.widget.TextField;
import dev.mw19.core.net.HttpClient;
import dev.mw19.core.packs.Modrinth;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resource pack browser: searches Modrinth, downloads the chosen pack into the resource pack folder (size-capped and
 * checked against Modrinth's SHA-512) and enables it. Network only while this page is used (README "Network use").
 */
public final class PacksPage extends Page {
    private static final int PAGE = 20;
    private static final long MAX_PACK = 512L << 20;

    private final TextField query = new TextField("");
    private final Button search, sort, version, folder, more;
    private final ScrollList list = new ScrollList();
    private Modrinth.Sort order = Modrinth.Sort.RELEVANCE;
    private boolean thisVersion = true, loading;
    private String error;
    private int total, loaded, generation;
    private final Map<String, Integer> icons = new HashMap<String, Integer>();
    private final Map<String, State> states = new HashMap<String, State>();
    private boolean searched;

    /** Install progress of one project. */
    private static final class State {
        String file, message;
        boolean busy, failed;
        volatile long done, size;
    }

    public PacksPage(final GuiRoot root) {
        super(root);
        list.gap = 3;
        query.placeholder = "Search resource packs";
        query.onSubmit = new Runnable() {
            @Override
            public void run() {
                search(false);
            }
        };
        search = new Button("", Button.Style.PRIMARY, new Runnable() {
            @Override
            public void run() {
                search(false);
            }
        }).icon("search");
        sort = new Button("", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                order = Modrinth.Sort.values()[(order.ordinal() + 1) % Modrinth.Sort.values().length];
                search(false);
            }
        });
        version = new Button("", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                thisVersion = !thisVersion;
                search(false);
            }
        });
        version.tooltip = "Only packs made for your Minecraft version";
        folder = new Button("Open folder", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                root.k.platform.openFolder(root.k.platform.packs().folder());
            }
        });
        more = new Button("Load more", Button.Style.SECONDARY, new Runnable() {
            @Override
            public void run() {
                search(true);
            }
        });
        more.h = 16;
    }

    @Override
    public String title() {
        return "Packs";
    }

    @Override
    public String icon() {
        return "packs";
    }

    @Override
    public void onShow() {
        if (!searched) search(false);
    }

    @Override
    public void dispose() {
        generation++; // callbacks still in flight are dropped
        for (Integer h : icons.values()) if (h != null && h > 0) root.k.platform.skins().releaseImage(h);
        icons.clear();
    }

    private String mc() {
        return root.k.platform.minecraftVersion();
    }

    private void search(final boolean append) {
        if (loading && append) return;
        searched = true;
        final int gen = append ? generation : ++generation;
        if (!append) {
            for (Integer h : icons.values()) if (h != null && h > 0) root.k.platform.skins().releaseImage(h);
            icons.clear();
            list.clear();
            loaded = total = 0;
        }
        loading = true;
        error = null;
        String url = Modrinth.searchUrl(query.text, thisVersion ? mc() : null, order, loaded, PAGE);
        root.k.http.getJson(url, 5 * 60_000L, new Http.Callback() {
            @Override
            public void done(int status, Object json, String err) {
                if (gen != generation) return; // a newer search replaced this one
                loading = false;
                if (status != 200) {
                    error = err != null ? "Modrinth: " + err : "Modrinth answered HTTP " + status;
                    return;
                }
                list.children.remove(more);
                List<Modrinth.Hit> hits = Modrinth.hits(json);
                total = Modrinth.totalHits(json);
                loaded += hits.size();
                for (Modrinth.Hit h : hits) list.add(new Row(h));
                if (loaded < total && !hits.isEmpty()) list.add(more);
            }
        });
    }

    private int icon(String url) {
        if (url == null || url.isEmpty() || !url.toLowerCase(Locale.ROOT).endsWith(".png")) return 0;
        Integer h = icons.get(url);
        if (h != null) return Math.max(0, h);
        icons.put(url, 0);
        final String u = url;
        final int gen = generation;
        root.k.http.request("GET", url, null, null, 1 << 20, new HttpClient.BytesCallback() {
            @Override
            public void done(int status, byte[] body, String error) {
                if (gen != generation) return;
                int handle = status == 200 && body != null ? root.k.platform.skins().loadImage(body) : 0;
                icons.put(u, handle > 0 ? handle : -1);
            }
        });
        return 0;
    }

    private void install(final Modrinth.Hit hit) {
        final State st = state(hit);
        if (st.busy) return;
        st.busy = true;
        st.failed = false;
        st.message = "Finding file…";
        root.k.http.getJson(Modrinth.versionsUrl(hit.id, thisVersion ? mc() : null), 60_000L, new Http.Callback() {
            @Override
            public void done(int status, Object json, String err) {
                Modrinth.PackFile f = status == 200 ? Modrinth.newestFile(json) : null;
                if (f == null) {
                    fail(st, status == 200 ? "No download for " + (thisVersion ? mc() : "any version") : "Modrinth: " + (err != null ? err : "HTTP " + status));
                    return;
                }
                if (f.size > MAX_PACK) {
                    fail(st, "Too large (" + mb(f.size) + ")");
                    return;
                }
                final Path target = root.k.platform.packs().folder().resolve(f.fileName);
                st.file = f.fileName;
                if (Files.exists(target)) {
                    st.busy = false;
                    st.message = null;
                    return;
                }
                st.size = f.size;
                st.message = null;
                root.k.http.download(f.url, target, f.sha512, MAX_PACK, new HttpClient.Progress() {
                    @Override
                    public void update(long done, long totalBytes) {
                        st.done = done;
                        if (totalBytes > 0) st.size = totalBytes;
                    }
                }, new HttpClient.BytesCallback() {
                    @Override
                    public void done(int s, byte[] body, String e) {
                        if (e != null) {
                            st.file = null;
                            fail(st, e);
                            return;
                        }
                        st.busy = false;
                        root.k.toast("Downloaded", hit.title + " is in your resource packs. Press Enable to use it.", root.k.theme.good);
                    }
                });
            }
        });
    }

    private void fail(State st, String msg) {
        st.busy = false;
        st.failed = true;
        st.message = msg;
    }

    private State state(Modrinth.Hit h) {
        State s = states.get(h.id);
        if (s == null) states.put(h.id, s = new State());
        return s;
    }

    private void enable(Modrinth.Hit hit, State st) {
        if (st.file == null) return;
        if (root.k.platform.packs().enable(st.file)) {
            root.k.toast("Resource pack enabled", hit.title + " is on top of your packs. Minecraft is reloading its resources.", root.k.theme.good);
        } else {
            st.message = "Minecraft did not find the file";
            st.failed = true;
        }
    }

    @Override
    public void render(Ui ui) {
        heading(ui, "Resource Packs", "Search Modrinth and install packs in one click.");
        float top = y + 30, bw = 18;
        sort.label = order.label;
        version.label = thisVersion ? mc() + " only" : "All versions";
        float sw = ui.g.textWidth(sort.label) + 14, vw = ui.g.textWidth(version.label) + 14;
        float qw = w - bw - sw - vw - 12;
        query.bounds(x, top, qw, 16).render(ui);
        search.bounds(x + qw + 4, top, bw, 16).render(ui);
        sort.bounds(x + qw + bw + 8, top, sw, 16).render(ui);
        version.bounds(x + w - vw, top, vw, 16).render(ui);
        float ly = top + 22, lh = h - 22 - 30 - 18;
        list.bounds(x, ly, w, lh);
        list.render(ui);
        if (list.children.isEmpty()) {
            String msg = loading ? "Searching…" : error != null ? error : searched ? "No packs found" : "";
            ui.g.textCentered(msg, x + w / 2f, ly + lh / 2f - 4, error != null ? ui.t.bad : ui.t.textDim, false);
        }
        float fy = y + h - 16;
        float fw = ui.g.textWidth(folder.label) + 14;
        folder.bounds(x, fy, fw, 16).render(ui);
        String info = loading && !list.children.isEmpty() ? "Loading…" : total > 0 ? loaded + " of " + total + " · from modrinth.com" : "Results from modrinth.com";
        ui.g.textRight(info, x + w, fy + 4, ui.t.textDim, false);
    }

    @Override
    public boolean mouseClicked(Ui ui, int button) {
        return query.mouseClicked(ui, button) || search.mouseClicked(ui, button) || sort.mouseClicked(ui, button)
                || version.mouseClicked(ui, button) || folder.mouseClicked(ui, button) || list.mouseClicked(ui, button);
    }

    @Override
    public boolean mouseScrolled(Ui ui, double amount) {
        return list.mouseScrolled(ui, amount);
    }

    @Override
    public void reveal(Widget w) {
        list.reveal(w);
    }

    /** Smoke: "N results" once a search finished, the error, or "searching". */
    public String status() {
        return loading ? "searching" : error != null ? error : loaded + " of " + total + " results";
    }

    static String count(long n) {
        if (n >= 1_000_000) return String.format(Locale.ROOT, "%.1fM", n / 1e6);
        if (n >= 1_000) return String.format(Locale.ROOT, "%.1fK", n / 1e3);
        return Long.toString(n);
    }

    private static String mb(long bytes) {
        return String.format(Locale.ROOT, "%.1f MB", bytes / 1048576.0);
    }

    /** One search result with its install button. */
    private final class Row extends Widget {
        private final Modrinth.Hit hit;
        private final String meta;
        private final char initial;
        private float bx, by2, bw2, bh2;

        Row(Modrinth.Hit hit) {
            this.hit = hit;
            this.h = 38;
            this.meta = (hit.author.isEmpty() ? "" : "by " + hit.author + " · ") + count(hit.downloads) + " downloads";
            this.initial = hit.title.isEmpty() ? '?' : Character.toUpperCase(hit.title.charAt(0));
        }

        @Override
        public void render(Ui ui) {
            boolean hv = hovered(ui);
            ui.g.roundRect(x, y, w, h, 4, Colors.fade(ui.t.surface2, hv ? 0.75f : 0.45f));
            int ic = icon(hit.iconUrl);
            float is = 30, ix = x + 4, iy = y + 4;
            if (ic > 0) {
                ui.g.image(ic, ix, iy, is, is);
            } else {
                ui.g.roundRect(ix, iy, is, is, 3, ui.t.surface);
                ui.g.textCentered(String.valueOf(initial), ix + is / 2f, iy + 11, ui.t.textDim, false);
            }
            State st = states.get(hit.id);
            String label;
            boolean primary = false, enabledNow = false;
            if (st != null && st.busy) {
                label = st.size > 0 && st.done > 0 ? (int) (st.done * 100 / Math.max(1, st.size)) + "%" : "…";
            } else if (st != null && st.file != null) {
                enabledNow = root.k.platform.packs().enabled().contains(st.file);
                label = enabledNow ? "Enabled" : "Enable";
                primary = !enabledNow;
            } else {
                label = st != null && st.failed ? "Retry" : "Install";
                primary = true;
            }
            bw2 = Math.max(52, ui.g.textWidth(label) + 14);
            bh2 = 16;
            bx = x + w - bw2 - 5;
            by2 = y + (h - bh2) / 2f;
            float tw = bx - (x + 40) - 6;
            ui.g.text(ui.g.ellipsize(hit.title, tw), x + 40, y + 4, ui.t.text, false);
            String second = st != null && st.message != null ? st.message : meta;
            ui.g.text(ui.g.ellipsize(second, tw), x + 40, y + 14, st != null && st.failed ? ui.t.bad : ui.t.textDim, false);
            ui.g.text(ui.g.ellipsize(hit.description, tw / 0.8f), x + 40, y + 25, 0.8f, Colors.fade(ui.t.textDim, 0.85f), false);
            boolean bhv = ui.hover(bx, by2, bw2, bh2) && !(st != null && st.busy) && !enabledNow;
            int bg = primary ? (bhv ? Colors.lerp(ui.t.accent, 0xFFFFFFFF, 0.15f) : ui.t.accent) : (bhv ? ui.t.border : ui.t.surface);
            ui.g.roundRect(bx, by2, bw2, bh2, 3, bg);
            if (st != null && st.busy && st.size > 0) {
                float p = Math.min(1f, st.done / (float) st.size);
                ui.g.roundRect(bx, by2 + bh2 - 2, bw2 * p, 2, 1, ui.t.good);
            }
            ui.g.textCentered(label, bx + bw2 / 2f, by2 + 4, primary ? ui.t.onAccent : enabledNow ? ui.t.good : ui.t.text, false);
        }

        @Override
        public boolean mouseClicked(Ui ui, int button) {
            if (button != 0 || !ui.hover(bx, by2, bw2, bh2)) return false;
            State st = states.get(hit.id);
            if (st != null && st.busy) return true;
            if (st != null && st.file != null) enable(hit, st);
            else install(hit);
            return true;
        }
    }
}
