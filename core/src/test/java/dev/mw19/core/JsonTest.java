package dev.mw19.core;

import dev.mw19.api.util.Json;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonTest {
    @Test
    void roundTripsNestedValues() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("s", "a\"b\\c\n\u0001é");
        m.put("n", 4294937149.0);
        m.put("f", 1.5);
        m.put("b", true);
        m.put("z", null);
        m.put("l", Arrays.<Object>asList(1.0, "x", Arrays.asList()));
        for (boolean pretty : new boolean[]{false, true}) {
            assertEquals(m, Json.parse(Json.write(m, pretty)));
        }
        assertEquals("4294937149", Json.write(4294937149.0, false));
    }

    @Test
    void parsesEscapesAndNumbers() {
        assertEquals("A/\u00e9\t", Json.parse("\"\\u0041\\/\\u00e9\\t\""));
        assertEquals(-12.5e3, (Double) Json.parse("-12.5e3"), 1e-9);
        assertEquals(Arrays.asList(1.0, 2.0), Json.parse(" [ 1 , 2 ] "));
    }

    @Test
    void rejectsGarbageWithoutHanging() {
        for (String bad : new String[]{"", "{", "[1,", "{\"a\" 1}", "tru", "\"\\x\"", "1 2", "{\"a\":}", "\"\u0001\""}) {
            assertThrows(IllegalArgumentException.class, () -> Json.parse(bad), bad);
        }
        StringBuilder deep = new StringBuilder();
        for (int i = 0; i < 10000; i++) deep.append('[');
        assertThrows(IllegalArgumentException.class, () -> Json.parse(deep.toString()));
    }

    @Test
    void typedAccessorsNeverThrow() {
        Map<String, Object> m = Json.obj(Json.parse("{\"a\":\"x\",\"n\":3,\"b\":false}"));
        assertEquals("x", Json.str(m, "a", "d"));
        assertEquals("d", Json.str(m, "n", "d"));
        assertEquals(3, Json.num(m, "n", 0), 0);
        assertFalse(Json.bool(m, "b", true));
        assertTrue(Json.obj("not a map").isEmpty());
        List<Object> l = Json.arr(null);
        assertTrue(l.isEmpty());
    }
}
