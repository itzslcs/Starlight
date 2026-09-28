package dev.starlight.core;

import dev.starlight.core.account.Accounts;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountsTest {
    private static final String PRISM = "{\"accounts\":["
            + "{\"type\":\"MSA\",\"profile\":{\"id\":\"069a79f444e94726a5befca90e38aaf5\",\"name\":\"Notch\"},"
            + "\"ygg\":{\"token\":\"eyJabc.def.ghi\",\"exp\":2000000000}},"
            + "{\"type\":\"Offline\",\"profile\":{\"id\":\"11111111-2222-3333-4444-555555555555\",\"name\":\"Dev\"},"
            + "\"ygg\":{\"token\":\"0\",\"exp\":0}},"
            + "{\"type\":\"MSA\",\"profile\":{\"id\":\"bad\",\"name\":\"Broken\"}},"
            + "{\"type\":\"MSA\",\"profile\":{\"id\":\"069a79f444e94726a5befca90e38aaf6\",\"name\":\"Stale\"},"
            + "\"ygg\":{\"token\":\"eyJold\",\"exp\":1000000}}"
            + "],\"formatVersion\":3}";

    @Test
    void readsPrismAccounts() {
        List<Accounts.Entry> all = Accounts.parse(PRISM);
        assertEquals(3, all.size(), "the entry with an unreadable id is skipped");

        Accounts.Entry msa = all.get(0);
        assertEquals("Notch", msa.name);
        assertEquals("069a79f4-44e9-4726-a5be-fca90e38aaf5", msa.id.toString());
        assertEquals("eyJabc.def.ghi", msa.token);
        assertFalse(msa.offline);
        assertFalse(msa.expired());
        assertEquals(2000000000000L, msa.expires, "seconds in the file, milliseconds in the entry");

        Accounts.Entry offline = all.get(1);
        assertTrue(offline.offline);
        assertEquals("", offline.token, "Prism's \"0\" token is no token");
        assertFalse(offline.expired(), "an offline account never expires");

        assertTrue(all.get(2).expired());
    }

    @Test
    void refusesWhatIsNotAnAccountsFile() {
        assertThrows(RuntimeException.class, () -> Accounts.parse("[]"));
        assertThrows(RuntimeException.class, () -> Accounts.parse("{\"formatVersion\":3}"));
    }
}
