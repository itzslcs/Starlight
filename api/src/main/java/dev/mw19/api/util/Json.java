package dev.mw19.api.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal JSON codec shared by every target (DECISIONS D-007).
 * Objects → LinkedHashMap, arrays → ArrayList, numbers → Double, plus String/Boolean/null.
 */
public final class Json {
    private static final int MAX_DEPTH = 256;

    private Json() {}

    public static Object parse(String text) {
        Parser p = new Parser(text);
        p.ws();
        Object v = p.value(0);
        p.ws();
        if (p.i != text.length()) throw p.err("trailing data");
        return v;
    }

    public static String write(Object value, boolean pretty) {
        StringBuilder sb = new StringBuilder(256);
        write(sb, value, pretty ? 0 : -1);
        return sb.toString();
    }

    // ---- typed accessors that never throw ----

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : Collections.<String, Object>emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o) {
        return o instanceof List ? (List<Object>) o : Collections.emptyList();
    }

    public static String str(Map<String, Object> m, String key, String def) {
        Object v = m.get(key);
        return v instanceof String ? (String) v : def;
    }

    public static double num(Map<String, Object> m, String key, double def) {
        Object v = m.get(key);
        return v instanceof Number ? ((Number) v).doubleValue() : def;
    }

    public static boolean bool(Map<String, Object> m, String key, boolean def) {
        Object v = m.get(key);
        return v instanceof Boolean ? (Boolean) v : def;
    }

    // ---- writer ----

    private static void write(StringBuilder sb, Object v, int indent) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String) {
            quote(sb, (String) v);
        } else if (v instanceof Boolean) {
            sb.append(v.toString());
        } else if (v instanceof Number) {
            double d = ((Number) v).doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) sb.append("null");
            else if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append((long) d);
            else sb.append(d);
        } else if (v instanceof Map) {
            Map<?, ?> m = (Map<?, ?>) v;
            if (m.isEmpty()) {
                sb.append("{}");
                return;
            }
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) sb.append(',');
                first = false;
                newline(sb, indent + 1);
                quote(sb, String.valueOf(e.getKey()));
                sb.append(indent >= 0 ? ": " : ":");
                write(sb, e.getValue(), indent < 0 ? -1 : indent + 1);
            }
            newline(sb, indent);
            sb.append('}');
        } else if (v instanceof List) {
            List<?> l = (List<?>) v;
            if (l.isEmpty()) {
                sb.append("[]");
                return;
            }
            sb.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(',');
                newline(sb, indent + 1);
                write(sb, l.get(i), indent < 0 ? -1 : indent + 1);
            }
            newline(sb, indent);
            sb.append(']');
        } else {
            quote(sb, v.toString());
        }
    }

    private static void newline(StringBuilder sb, int indent) {
        if (indent < 0) return;
        sb.append('\n');
        for (int i = 0; i < indent; i++) sb.append("  ");
    }

    private static void quote(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
            }
        }
        sb.append('"');
    }

    // ---- parser ----

    private static final class Parser {
        final String s;
        int i;

        Parser(String s) {
            this.s = s;
        }

        IllegalArgumentException err(String what) {
            return new IllegalArgumentException("JSON " + what + " at " + i);
        }

        void ws() {
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == ' ' || c == '\n' || c == '\r' || c == '\t') i++;
                else break;
            }
        }

        Object value(int depth) {
            if (depth > MAX_DEPTH) throw err("too deep");
            if (i >= s.length()) throw err("unexpected end");
            char c = s.charAt(i);
            switch (c) {
                case '{': return object(depth);
                case '[': return array(depth);
                case '"': return string();
                case 't': return literal("true", Boolean.TRUE);
                case 'f': return literal("false", Boolean.FALSE);
                case 'n': return literal("null", null);
                default:
                    if (c == '-' || (c >= '0' && c <= '9')) return number();
                    throw err("unexpected '" + c + "'");
            }
        }

        Object literal(String word, Object v) {
            if (!s.startsWith(word, i)) throw err("bad literal");
            i += word.length();
            return v;
        }

        Map<String, Object> object(int depth) {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                ws();
                if (i >= s.length() || s.charAt(i) != '"') throw err("expected key");
                String k = string();
                ws();
                if (i >= s.length() || s.charAt(i) != ':') throw err("expected ':'");
                i++;
                ws();
                m.put(k, value(depth + 1));
                ws();
                if (i >= s.length()) throw err("unexpected end");
                char c = s.charAt(i++);
                if (c == '}') return m;
                if (c != ',') throw err("expected ',' or '}'");
            }
        }

        List<Object> array(int depth) {
            List<Object> l = new ArrayList<Object>();
            i++;
            ws();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                ws();
                l.add(value(depth + 1));
                ws();
                if (i >= s.length()) throw err("unexpected end");
                char c = s.charAt(i++);
                if (c == ']') return l;
                if (c != ',') throw err("expected ',' or ']'");
            }
        }

        String string() {
            i++; // opening quote
            StringBuilder sb = null;
            int start = i;
            while (i < s.length()) {
                char c = s.charAt(i);
                if (c == '"') {
                    String out = sb == null ? s.substring(start, i) : sb.append(s, start, i).toString();
                    i++;
                    return out;
                }
                if (c == '\\') {
                    if (sb == null) sb = new StringBuilder();
                    sb.append(s, start, i);
                    if (++i >= s.length()) throw err("bad escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'b': sb.append('\b'); break;
                        case 'f': sb.append('\f'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        case 'u':
                            if (i + 4 > s.length()) throw err("bad \\u escape");
                            try {
                                sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            } catch (NumberFormatException ex) {
                                throw err("bad \\u escape");
                            }
                            i += 4;
                            break;
                        default: throw err("bad escape");
                    }
                    start = i;
                } else {
                    if (c < 0x20) throw err("control char in string");
                    i++;
                }
            }
            throw err("unterminated string");
        }

        Double number() {
            int start = i;
            if (s.charAt(i) == '-') i++;
            while (i < s.length()) {
                char c = s.charAt(i);
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') i++;
                else break;
            }
            try {
                return Double.valueOf(s.substring(start, i));
            } catch (NumberFormatException ex) {
                throw err("bad number");
            }
        }
    }
}
