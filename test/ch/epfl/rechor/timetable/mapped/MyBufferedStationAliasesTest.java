package ch.epfl.rechor.timetable.mapped;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example tests for the BufferedStationAliases class.
 * Adapt method/field names to your actual code if needed.
 */
public class MyBufferedStationAliasesTest {

    // ----------------------------------------------------------------------
    // 1) Basic constructor tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Constructor succeeds with valid data (example from §2.6.2)")
    void testConstructorValid() {
        // From the specification §2.6.2, we have 2 alias entries => 2 * 4 = 8 bytes:
        //   Entry 0: ALIAS_ID=5 => "Losanna", STATION_NAME_ID=4 => "Lausanne"
        //   Entry 1: ALIAS_ID=2 => "Anet",    STATION_NAME_ID=3 => "Ins"
        //
        // Hex representation: 00 05 00 04 00 02 00 03
        // We'll create a ByteBuffer with these 8 bytes.

        byte[] aliasBytes = new byte[] {
                0x00, 0x05,  0x00, 0x04,
                0x00, 0x02,  0x00, 0x03
        };
        ByteBuffer buffer = ByteBuffer.wrap(aliasBytes);

        // Minimal string table for indices [0..5], at least covering 2..5
        //   index 2 => "Anet"
        //   index 3 => "Ins"
        //   index 4 => "Lausanne"
        //   index 5 => "Losanna"
        List<String> stringTable = Arrays.asList(
                "1", "70", "Anet", "Ins", "Lausanne", "Losanna"
        );

        // Should not throw:
        BufferedStationAliases aliases = new BufferedStationAliases(stringTable, buffer);
        assertNotNull(aliases);
        // We expect 2 aliases
        assertEquals(2, aliases.size());
    }

    @Test
    @DisplayName("Constructor throws IllegalArgumentException if buffer size is not multiple of 4 bytes")
    void testConstructorInvalidBufferSize() {
        // Each alias is 4 bytes => if buffer size is not multiple of 4 => fail
        byte[] invalidBytes = new byte[6]; // 6 is not multiple of 4
        ByteBuffer buffer = ByteBuffer.wrap(invalidBytes);
        List<String> stringTable = List.of("A", "B");

        assertThrows(IllegalArgumentException.class, () ->
                new BufferedStationAliases(stringTable, buffer)
        );
    }

    @Test
    @DisplayName("Constructor with empty buffer => zero aliases (if allowed)")
    void testConstructorEmptyBuffer() {
        // 0 is a multiple of 4 => means 0 aliases
        ByteBuffer buffer = ByteBuffer.allocate(0);
        List<String> stringTable = List.of("A", "B");

        BufferedStationAliases aliases = new BufferedStationAliases(stringTable, buffer);
        assertEquals(0, aliases.size());
    }

    @Test
    @DisplayName("Constructor throws if stringTable is null (optional)")
    void testConstructorNullStringTable() {
        ByteBuffer buffer = ByteBuffer.allocate(4); // enough for 1 alias
        assertThrows(NullPointerException.class, () ->
                new BufferedStationAliases(null, buffer)
        );
    }

    @Test
    @DisplayName("Constructor throws if buffer is null (optional)")
    void testConstructorNullBuffer() {
        List<String> stringTable = List.of("A", "B");
        assertThrows(NullPointerException.class, () ->
                new BufferedStationAliases(stringTable, null)
        );
    }

    // ----------------------------------------------------------------------
    // 2) Tests reading aliases data
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("aliasCount() returns correct number of aliases")
    void testAliasCount() {
        // Create a buffer for 3 aliases => 3 * 4 = 12 bytes
        ByteBuffer buffer = ByteBuffer.allocate(12);
        List<String> table = List.of("A", "B", "C");
        BufferedStationAliases aliases = new BufferedStationAliases(table, buffer);
        assertEquals(3, aliases.size());
    }

    @Test
    @DisplayName("Reading alias and station names from example data")
    void testReadAliasDataExample() {
        // Reuse the example data from §2.6.2
        byte[] aliasBytes = new byte[] {
                0x00, 0x05,  0x00, 0x04,
                0x00, 0x02,  0x00, 0x03
        };
        ByteBuffer buffer = ByteBuffer.wrap(aliasBytes);

        // Indices 5 => "Losanna", 4 => "Lausanne", 2 => "Anet", 3 => "Ins"
        List<String> stringTable = Arrays.asList(
                "1", "70", "Anet", "Ins", "Lausanne", "Losanna"
        );

        BufferedStationAliases aliases = new BufferedStationAliases(stringTable, buffer);
        assertEquals(2, aliases.size());

        // Alias 0 => "Losanna", station => "Lausanne"
        assertEquals("Losanna",  aliases.alias(0));
        assertEquals("Lausanne", aliases.stationName(0));

        // Alias 1 => "Anet", station => "Ins"
        assertEquals("Anet", aliases.alias(1));
        assertEquals("Ins",  aliases.stationName(1));
    }

    // ----------------------------------------------------------------------
    // 3) Tests for invalid alias indices
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Calling aliasName/stationName with invalid index => IndexOutOfBoundsException")
    void testInvalidAliasIndex() {
        // 2 aliases
        ByteBuffer buffer = ByteBuffer.allocate(8);
        List<String> table = List.of("A", "B", "C", "D", "E");
        BufferedStationAliases aliases = new BufferedStationAliases(table, buffer);

        // Valid indices: 0..1
        assertThrows(IndexOutOfBoundsException.class, () -> aliases.alias(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> aliases.alias(2));
        assertThrows(IndexOutOfBoundsException.class, () -> aliases.stationName(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> aliases.stationName(2));
    }

    // ----------------------------------------------------------------------
    // 4) Tests for name resolution from the string table
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Resolves correct alias and station name from string table for each entry")
    void testAliasNameResolution() {
        // Let's create a buffer for 2 aliases, each 4 bytes:
        // Alias 0 => ALIAS_ID=1, STATION_NAME_ID=2
        // Alias 1 => ALIAS_ID=3, STATION_NAME_ID=0
        byte[] data = new byte[] {
                0x00, 0x01,  0x00, 0x02,
                0x00, 0x03,  0x00, 0x00
        };
        ByteBuffer buffer = ByteBuffer.wrap(data);

        // stringTable => index 0 => "Zero", 1 => "One", 2 => "Two", 3 => "Three"
        List<String> stringTable = Arrays.asList("Zero", "One", "Two", "Three");

        BufferedStationAliases aliases = new BufferedStationAliases(stringTable, buffer);

        assertEquals(2, aliases.size());
        // alias 0 => aliasName= "One", stationName= "Two"
        assertEquals("One", aliases.alias(0));
        assertEquals("Two", aliases.stationName(0));
        // alias 1 => aliasName= "Three", stationName= "Zero"
        assertEquals("Three", aliases.alias(1));
        assertEquals("Zero",  aliases.stationName(1));
    }
}

