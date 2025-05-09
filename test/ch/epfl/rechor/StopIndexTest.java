package ch.epfl.rechor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class StopIndexTest {
    private StopIndex index;

    // -----------------------------  Fixtures  --------------------------------

    @BeforeEach
    void setUp() {
        List<String> mains = List.of(
                "Lausanne",
                "Genève",
                "St-Cergue",
                "Nyon",
                "Nyon Gare"
        );
        Map<String, String> alts = Map.ofEntries(
                Map.entry("Lsn", "Lausanne"),
                Map.entry("Lausanne Gare", "Lausanne"),
                Map.entry("Geneva", "Genève"),
                Map.entry("NYON", "Nyon"),          // differs only in case
                Map.entry("Zurich HB", "Zürich")    // «main» not declared in mains
        );
        index = new StopIndex(mains, alts);
    }

    // -----------------------  Constructor validation  ------------------------

    @Nested
    @DisplayName("Constructor argument validation")
    final class ConstructorValidation {

        @Test
        @DisplayName("null main list -> NPE")
        void nullMainList() {
            assertThrows(NullPointerException.class,
                    () -> new StopIndex(null, Map.of()));
        }

        @Test
        @DisplayName("null alt map -> NPE")
        void nullAltMap() {
            assertThrows(NullPointerException.class,
                    () -> new StopIndex(List.of(), null));
        }

//        @Test
//        @DisplayName("null element in main list -> NPE")
//        void nullElementInMainList() {
//            List<String> mains = List.of("A", null);
//            assertThrows(NullPointerException.class,
//                    () -> new StopIndex(mains, Map.of()));
//        }
//
//        @Test
//        @DisplayName("null key in alt map -> NPE")
//        void nullKeyInAltMap() {
//            Map<String, String> alts = Map.ofEntries(
//                    Map.entry(null, "A")
//            );
//            assertThrows(NullPointerException.class,
//                    () -> new StopIndex(List.of("A"), alts));
//        }
//
//        @Test
//        @DisplayName("null value in alt map -> NPE")
//        void nullValueInAltMap() {
//            Map<String, String> alts = Map.ofEntries(
//                    Map.entry("B", null)
//            );
//            assertThrows(NullPointerException.class,
//                    () -> new StopIndex(List.of("A"), alts));
//        }

        @Test
        @DisplayName("alt referring to unknown main is accepted")
        void altWithUnknownMainAccepted() {
            // Must not throw
            Map<String,String> alts = Map.of("Alias", "NewMain");
            StopIndex s = new StopIndex(List.of("A"), alts);
            assertNotNull(s);
            // And the unknown main becomes searchable
            assertEquals(List.of("NewMain"), s.stopsMatching("Alias", 5));
        }
    }

    // ---------------------------  Search basics  -----------------------------

    @Nested
    @DisplayName("Basic search behaviour")
    final class BasicSearch {

        @Test
        @DisplayName("null query -> NPE")
        void nullQueryThrows() {
            assertThrows(NullPointerException.class,
                    () -> index.stopsMatching(null, 3));
        }

        @Test
        @DisplayName("result list size ≤ maxCount")
        void respectsMaxCount() {
            int max = 2;
            List<String> r = index.stopsMatching("n", max);
            assertTrue(r.size() <= max);
        }
    }

    // -------------------------  Matching semantics  --------------------------

    @Nested
    @DisplayName("Matching semantics")
    final class MatchingSemantics {

        @Test
        @DisplayName("synonym returns main name, not the synonym")
        void synonymMapsToMain() {
            assertEquals(List.of("Lausanne"),
                    index.stopsMatching("Lsn", 1));
        }

        @Test
        @DisplayName("accent‑agnostic query matches")
        void accentInsensitiveMatch() {
            assertEquals(List.of("Genève"),
                    index.stopsMatching("Geneve", 3));
        }

        @Test
        @DisplayName("lower‑case query is case‑insensitive")
        void lowerCaseQueryIsCaseInsensitive() {
            // lower‑case 'geneva' should match 'Geneva' alt -> 'Genève' main
            assertEquals(List.of("Genève"),
                    index.stopsMatching("geneva", 1));
        }

        @Test
        @DisplayName("all sub‑queries must appear")
        void allSubQueriesMandatory() {
            assertTrue(index.stopsMatching("nyon lugano", 5).isEmpty());
        }

        @Test
        @DisplayName("ranking prefers shorter, higher‑scoring matches")
        void rankingByScore() {
            List<String> res = index.stopsMatching("nyon", 2);
            assertEquals("Nyon", res.get(0), "Exact stop should outrank longer variant");
            assertEquals("Nyon Gare", res.get(1));
        }

        @Test
        @DisplayName("maxCount truncates result list—not merely hides elements")
        void maxCountTruncates() {
            List<String> more = index.stopsMatching("n", 10);
            List<String> truncated = index.stopsMatching("n", 1);
            assertEquals(1, truncated.size());
            assertEquals(more.get(0), truncated.get(0));
        }
    }

    // ----------------------  Data‑driven accent tests  -----------------------

    @ParameterizedTest(name = "\"{0}\" ➜ {1}")
    @MethodSource("accentExamples")
    @DisplayName("accent variations are interchangeable")
    void accentVariants(String query, String expected) {
        assertEquals(List.of(expected), index.stopsMatching(query, 3));
    }

    private static Stream<Arguments> accentExamples() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("cergue", "St-Cergue"),
                org.junit.jupiter.params.provider.Arguments.of("zurich", "Zürich"),
                org.junit.jupiter.params.provider.Arguments.of("zürich", "Zürich")
        );
    }

    public static List<String> stringTable(){
        List<String> list = new ArrayList<>();
        list.add("Anet");
        list.add("Ins");
        list.add("Lausanne");
        list.add("Losanna");
        list.add("Palézieux");
        list.add("Genève");
        list.add("Zurich");
        list.add("Bâle");
        list.add("Berne");
        list.add("Fribourg");
        list.add("Neuchâtel");
        list.add("Delémont");
        list.add("Sion");
        list.add("Sierre");
        list.add("Martigny");
        list.add("Monthey");
        list.add("Nyon");
        list.add("Yverdon-les-Bains");
        list.add("Vevey");
        list.add("Montreux");
        list.add("Aigle");
        list.add("Morges");
        list.add("Renens");
        list.add("Pully");
        list.add("Ecublens");
        list.add("Gland");
        list.add("Versoix");
        list.add("Carouge");
        list.add("Onex");
        list.add("Lancy");
        list.add("Meyrin");
        list.add("Thônex");
        list.add("Vernier");
        list.add("Plan-les-Ouates");
        list.add("Bienne");
        list.add("Soleure");
        list.add("Schaffhouse");
        list.add("Saint-Gall");
        list.add("Coire");
        list.add("Lugano");
        list.add("Bellinzone");
        list.add("Locarno");
        list.add("Aarau");
        list.add("Olten");
        list.add("Zoug");
        list.add("Winterthour");
        list.add("Uster");
        list.add("Rapperswil-Jona");
        list.add("Meziere");
        list.add("Méziere");
        list.add("Mêziere");
        list.add("mezzo");
        list.add("mezquité");
        list.add("mézanine");
        list.add("mézomorphose");
        list.add("mesquin");
        list.add("prémez");
        list.add("aménagement");
        list.add("commerçant");
        return list;
    }

    public static Map<String, String> mapTable(){
        Map<String, String> translations = new HashMap<>();
        translations.put("Losanna", "Lausanne");
        translations.put("Ginevra", "Genève");
        translations.put("Zurigo", "Zurich");
        translations.put("Basilea", "Bâle");
        translations.put("Berna", "Berne");
        translations.put("Friburgo", "Fribourg");
        translations.put("Coira", "Coire");
        translations.put("San Gallo", "Saint-Gall");
        translations.put("Sciaffusa", "Schaffhouse");
        translations.put("Soletta", "Soleure");
        translations.put("Lucerna", "Lucerne");
        translations.put("Zugo", "Zoug");
        translations.put("Neuenburg", "Neuchâtel");
        translations.put("Delsberg", "Delémont");
        translations.put("Sitten", "Sion");
        translations.put("Briga", "Brigue");
        translations.put("Thun", "Thoune");
        translations.put("Lenzburg", "Lentzbourg");
        translations.put("Biel", "Bienne");
        translations.put("Rapperswil", "Rapperschwyl");
        translations.put("Winterthur", "Winterthour");
        translations.put("Lugano", "Lugano"); // même nom en italien et français
        translations.put("Bellinzona", "Bellinzone");
        translations.put("Locarno", "Locarno");
        return translations;
    }



    @Test
    void testConstruction() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        System.out.println(s.stopsMatching("gru", 150));
    }


    public static List<String> stringTable2(){
        List<String> list = new ArrayList<>();
        list.add("aménagement");
        list.add("commerçant");
        return list;
    }

    public static Map<String, String> mapTable2(){
        Map<String, String> translations = new HashMap<>();
        translations.put("commerçant", "commerçant");
        return translations;
    }


    @Test
    void stopsMatchingTest1() throws IOException {
        List<String> stringTable = List.copyOf(stringTable2());

        StopIndex s = new StopIndex(stringTable, mapTable2());

        System.out.println(s.stopsMatching("mmerc", 150));
    }

    //YEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEEESSSSSSSSSSS POUR LES MAJUSCULES CA PASSE PAS
    @Test
    void stopsMatchingTest2() throws IOException {
        List<String> stringTable = List.copyOf(stringTable2());

        StopIndex s = new StopIndex(stringTable, mapTable2());

        System.out.println(s.stopsMatching("mmerC", 150));
    }

    public static List<String> stringTable3(){
        List<String> list = new ArrayList<>();
        list.add("aménagement");
        list.add("commerçant");
        return list;
    }

    public static Map<String, String> mapTable3(){
        Map<String, String> translations = new HashMap<>();
        translations.put("commerçant", "commerçant");
        return translations;
    }

    @Test
    void stopsMatchingTest3() throws IOException {
        List<String> stringTable = List.copyOf(stringTable3());

        StopIndex s = new StopIndex(stringTable, mapTable3());

        System.out.println(s.stopsMatching("mmerc", 150));
    }

    @Test
    void stopsMatchingTest4() throws IOException {
        List<String> stringTable = List.copyOf(stringTable2());

        StopIndex s = new StopIndex(stringTable, mapTable2());

        System.out.println(s.stopsMatching("mmerc", 150));
    }




    public static List<String> mystringTable4(){
        List<String> list = new ArrayList<>();
        list.add("GOBOU_________________________________________________________");
        list.add("PIKACHU");
        list.add("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        list.add("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
        list.add("Lausanne");
        list.add("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
        list.add("Palézieux");
        list.add("\r\r\r\r\r\r");
        list.add("Zurich");
        list.add("\\n\\r\\n");
        list.add("POKEMON");

        return list;
    }

    public static Map<String, String> mapTable4(){
        Map<String, String> translations = new HashMap<>();
        return translations;
    }

    @Test
    void stopsMatchingTest5() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable4());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD", 150));
    }
    @Test
    void stopsMatchingTest6() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable4());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("\r\r\r", 150));
    }

    @Test
    void stopsMatchingTest7() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable4());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("GOBOU_________________________________________________________PIKACHUAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAPalézieux", 150));
    }

    @Test
    void stopsMatchingTest8() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable4());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("GOBOU_________________________________________________________ PIKACHU AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA Palézieux", 150));
    }

    public static List<String> mystringTable5(){
        List<String> list = new ArrayList<>();
        list.add("GOBOU_________________________________________________________ PIKACHU");
        list.add("PIKACHU");
        list.add("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        list.add("BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB");
        list.add("Lausanne");
        list.add("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD");
        list.add("Palézieux ");
        list.add("\r\r\r\r\r\r");
        list.add("Zurich");
        list.add("\\n\\r\\n");
        list.add("POKEMON");

        return list;
    }

    @Test
    void stopsMatchingTest9() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable5());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("GOBOU_________________________________________________________ PIKACHU AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA Palézieux", 150));
    }

    //ON A UN BUG AVEC LES ESPACES
    @Test
    void stopsMatchingTest10() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable5());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("Palézieux ", 150));
    }

    @Test
    void stopsMatchingTest11() throws IOException {
        List<String> stringTable = List.copyOf(mystringTable5());

        StopIndex s = new StopIndex(stringTable, mapTable4());

        System.out.println(s.stopsMatching("Palézieux", 150));
    }



    @Test
    void testselonlesdonnesduprofquisontdonnesdansletape8() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        System.out.println(s.stopsMatching("mez vil", 150));
    }


    public static List<String> stringTable25(){
        List<String> list = new ArrayList<>();
        list.add("aménagement urbain");
        list.add("commercant");
        return list;
    }

    public static Map<String, String> mapTable25(){
        Map<String, String> translations = new HashMap<>();
        translations.put("commerçant", "commercant");
        return translations;
    }

    @Test
    void stopsMatchingTest115() throws IOException {

        StopIndex s = new StopIndex(stringTable25(), mapTable25());

        System.out.println(s.stopsMatching("commerçant", 4));
    }

    @Test
    void stopsMatchingTest116() throws IOException {

        StopIndex s = new StopIndex(stringTable25(), mapTable25());

        System.out.println(s.stopsMatching("aménagement urbain", 4));
    }



    @Test
    void testProfesorData() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        assertTrue((s.stopsMatching("kawabunga", 150)).isEmpty());
        assertTrue((s.stopsMatching("mez vil", 0)).isEmpty());
        assertTrue((s.stopsMatching("$$£", 150)).isEmpty());
        assertTrue((s.stopsMatching("MADRIGAL SONGOKU", 150)).isEmpty());
        assertTrue((s.stopsMatching("Pseudotorynorrhina", 150)).isEmpty());
        assertTrue((s.stopsMatching("navijak", 150)).isEmpty());

    }

    @Test
    void testProfesorData2() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        System.out.println((s.stopsMatching("ilétait unpetit navirequi n'avait jamais navigué", 8)));
        assertTrue((s.stopsMatching("ilétait unpetit navirequi n'avait jamais navigué", 8)).isEmpty());
    }

    @Test
    void testProfesorData3() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        System.out.println(s.stopsMatching("RENENS GARE", 8));
    }

    @Test
    void testProfesorData4() throws IOException {

        Path stringsPath = Path.of("timetable12").resolve("strings.txt");
        List<String> stringTable = List.copyOf(Files.readAllLines(stringsPath, StandardCharsets.ISO_8859_1));

        StopIndex s = new StopIndex(stringTable, mapTable());

        System.out.println(s.stopsMatching("mez vil", 8));
    }



    @Test
    void stopsMatchingTest117() throws IOException {

        StopIndex s = new StopIndex(stringTable25(), mapTable25());

        System.out.println(s.stopsMatching("COMmERCANT", 4));
    }





















    //ASSERT TESTS

    public static List<String> stringTableAssert(){
        List<String> list = new ArrayList<>();
        list.add("Anet");
        list.add("Ins");
        list.add("Lausanne");
        list.add("Losanna");
        list.add("Palézieux");
        list.add("Genève");
        list.add("Zurich");
        list.add("Bâle");
        list.add("Berne");
        list.add("Fribourg");
        list.add("Neuchâtel");
        list.add("Delémont");
        list.add("Sion");
        list.add("Sierre");
        list.add("Martigny");
        list.add("Monthey");
        list.add("Nyon");
        list.add("Yverdon-les-Bains");
        list.add("Vevey");
        list.add("Montreux");
        list.add("Aigle");
        list.add("Morges");
        list.add("Renens");
        list.add("Pully");
        return list;
    }

    public static Map<String, String> mapTableAssert(){
        Map<String, String> translations = new HashMap<>();
        return translations;
    }

    public static List<String> stringTableAssert2(){
        List<String> list = new ArrayList<>();
        list.add("Anetad");
        list.add("Anetae");
        list.add("Anetaf");
        list.add("Anettg");
        list.add("Anethu");
        list.add("Anetok");
        list.add("Anetlo");
        list.add("Anetmo");
        list.add("Anetro");
        list.add("Anetko");

        return list;
    }


    @Test
    void stopsMatchingTestAssert1() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert(), mapTableAssert());
        assertEquals(1 , s.stopsMatching("Anet", 154).size());
        assertEquals(1 , s.stopsMatching("anet", 154).size());
        assertEquals(0 , s.stopsMatching("anEt", 154).size());
        assertEquals(0 , s.stopsMatching("pokemon", 154).size());
    }

    @Test
    void stopsMatchingTestAssert2() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert2(), mapTableAssert());
        assertEquals("[Anetad, Anetae, Anetaf, Anethu, Anetko, Anetlo, Anetmo, Anetok, Anetro, Anettg]" , s.stopsMatching("anet", 154).toString());
    }

    @Test
    void stopsMatchingTestAssert3() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert2(), mapTableAssert());
        assertEquals("[]" , s.stopsMatching("aneT", 154).toString());
    }

    @Test
    void stopsMatchingTestAssert4() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert(), mapTableAssert());
        assertEquals(1 , s.stopsMatching("Anet", Integer.MAX_VALUE).size());
        assertThrows(IllegalArgumentException.class, () ->  s.stopsMatching("Anet", Integer.MIN_VALUE).size());
        assertThrows(IllegalArgumentException.class, () ->  s.stopsMatching("Anet", -154).size());
        assertEquals(0 , s.stopsMatching("Anet", 0).size());
    }

    @Test
    void stopsMatchingTestAssert5() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert(), mapTableAssert());
        assertEquals(3 , s.stopsMatching("an", Integer.MAX_VALUE).size());
        assertEquals("[Anet, Losanna, Lausanne]", s.stopsMatching("an", 4).toString());
    }

    public static Map<String, String> mapTableAssert2(){
        Map<String, String> translations = new HashMap<>();
        translations.put("an","Losanna");
        translations.put("annecy","Losanna");
        translations.put("ans","Losanna");
        translations.put("année","Losanna");
        translations.put("antre","Losanna");
        translations.put("ande","Losanna");


        return translations;
    }

    @Test
    void stopsMatchingTestAssert6() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert(), mapTableAssert2());
        assertEquals(3 , s.stopsMatching("an", Integer.MAX_VALUE).size());
        assertEquals("[Losanna, Anet, Lausanne]", s.stopsMatching("an", 3).toString());
    }

    @Test
    void stopsMatchingTestAssert7() throws IOException {
        StopIndex s = new StopIndex(stringTableAssert(), mapTableAssert2());
        assertThrows(NullPointerException.class, ()->s.stopsMatching(null, 3));
    }


}

