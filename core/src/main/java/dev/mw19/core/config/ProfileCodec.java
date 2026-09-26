package dev.mw19.core.config;

import dev.mw19.api.util.Json;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/** Shareable one-line profile strings: MW19-P1:<base64url(deflate(json))>:<crc32 hex>. */
public final class ProfileCodec {
    public static final String PREFIX = "MW19-P1:";
    /** Codes shared before the rename (2026-09-26) use the old client name; the payload format is identical. */
    private static final String LEGACY_PREFIX = "KESTREL-P1:";
    private static final int MAX_JSON = 1 << 20;

    private ProfileCodec() {}

    public static String encode(Map<String, Object> profile) {
        byte[] json = Json.write(profile, false).getBytes(StandardCharsets.UTF_8);
        Deflater d = new Deflater(Deflater.BEST_COMPRESSION);
        d.setInput(json);
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        CRC32 crc = new CRC32();
        crc.update(json);
        return PREFIX + Base64Url.encode(out.toByteArray()) + ":" + Long.toHexString(crc.getValue());
    }

    /** @throws IllegalArgumentException with a user-readable reason */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> decode(String text) {
        String s = text == null ? "" : text.trim();
        if (s.startsWith(LEGACY_PREFIX)) s = PREFIX + s.substring(LEGACY_PREFIX.length());
        if (!s.startsWith(PREFIX)) throw new IllegalArgumentException("Not a MW19 profile string");
        int colon = s.lastIndexOf(':');
        if (colon <= PREFIX.length()) throw new IllegalArgumentException("Profile string is truncated");
        byte[] packed;
        try {
            packed = Base64Url.decode(s.substring(PREFIX.length(), colon));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Profile string is damaged");
        }
        Inflater inf = new Inflater();
        inf.setInput(packed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        try {
            while (!inf.finished()) {
                int n = inf.inflate(buf);
                if (n == 0 && (inf.needsInput() || inf.needsDictionary())) throw new IllegalArgumentException("Profile string is truncated");
                out.write(buf, 0, n);
                if (out.size() > MAX_JSON) throw new IllegalArgumentException("Profile string is too large");
            }
        } catch (DataFormatException e) {
            throw new IllegalArgumentException("Profile string is damaged");
        } finally {
            inf.end();
        }
        byte[] json = out.toByteArray();
        CRC32 crc = new CRC32();
        crc.update(json);
        if (!Long.toHexString(crc.getValue()).equalsIgnoreCase(s.substring(colon + 1))) {
            throw new IllegalArgumentException("Profile string checksum mismatch");
        }
        Object parsed;
        try {
            parsed = Json.parse(new String(json, StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Profile content is not valid JSON");
        }
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("Profile content is not an object");
        try {
            return Migrations.profile((Map<String, Object>) parsed);
        } catch (IllegalStateException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /** java.util.Base64 is Java 8 but we keep URL-safe, unpadded output explicit. */
    static final class Base64Url {
        static String encode(byte[] b) {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        }

        static byte[] decode(String s) {
            return java.util.Base64.getUrlDecoder().decode(s);
        }
    }
}
