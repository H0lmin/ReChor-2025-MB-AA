package ch.epfl.rechor.journey;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import java.util.ArrayList;
import java.util.List;

import ch.epfl.rechor.PackedRange;

public class MyParetoFrontTest {

    /**
     * Helper method to pack a tuple representing a Pareto criterion.
     * Here, we interpret the tuple as (arrivalMinutes, changes) using the PackedRange convention:
     * the tuple = Bits32_24_8.pack(arrMins, (arrMins + changes) - arrMins) = Bits32_24_8.pack(arrMins, changes).
     * In our case, to represent "arrival at t minutes and c changes", we use:
     *     PackedRange.pack(t, t + c)
     */
    private int packPareto(int arrivalMinutes, int changes) {
        return PackedRange.pack(arrivalMinutes, arrivalMinutes + changes);
    }

    // --- Test EMPTY constant ---

    @Test
    public void testEmptyParetoFront() {
        ParetoFront empty = ParetoFront.EMPTY;
        assertEquals(0, empty.size(), "EMPTY ParetoFront should have size 0");
        assertThrows(NoSuchElementException.class, () -> {
            empty.get(480, 480 + 3);
        }, "Getting an element from EMPTY should throw exception");
    }

    // --- Basic insertion and retrieval ---

    @Test
    public void testSingleInsertion() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        int arrival = 480; // 8:00 (in minutes)
        int changes = 3;
        int tuple = packPareto(arrival, changes);

        builder.add(tuple);
        ParetoFront front = builder.build();
        assertEquals(1, front.size(), "Frontier should contain 1 tuple");

        long retrieved = front.get(arrival, arrival + changes);
        assertEquals(tuple, retrieved, "Retrieved tuple should match inserted tuple");
    }

    @Test
    public void testForEachIteration() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        int arrival1 = 480, changes1 = 3;
        int arrival2 = 500, changes2 = 2;
        int tuple1 = packPareto(arrival1, changes1);
        int tuple2 = packPareto(arrival2, changes2);
        builder.add(tuple1).add(tuple2);

        List<Long> collected = new ArrayList<>();
        ParetoFront front = builder.build();
        front.forEach(collected::add);

        assertEquals(2, collected.size(), "forEach should iterate over 2 tuples");
        // Since ParetoFront is built from an array stored in lexicographic order,
        // the order should be such that the tuple with lower value (when interpreted as an unsigned int) comes first.
        // We can compare the order values directly.
        long order1 = Integer.toUnsignedLong(tuple1);
        long order2 = Integer.toUnsignedLong(tuple2);
        if (order1 < order2) {
            assertEquals(tuple1, collected.get(0));
            assertEquals(tuple2, collected.get(1));
        } else {
            assertEquals(tuple2, collected.get(0));
            assertEquals(tuple1, collected.get(1));
        }
    }

    // --- Duplicate insertion ---

    @Test
    public void testDuplicateInsertion() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        int arrival = 480;
        int changes = 3;
        int tuple = packPareto(arrival, changes);
        builder.add(tuple);
        builder.add(tuple);  // duplicate
        assertEquals(1, builder.build().size(), "Duplicate insertion should not increase size");
    }

    // --- Insertion with dominance ---

    @Test
    public void testDominatedInsertionRemovesTuples() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Insert three tuples:
        // Tuple A: arrival 480, 4 changes  (pack(480, 480+4))
        // Tuple B: arrival 485, 3 changes  (pack(485, 485+3))
        // Tuple C: arrival 490, 5 changes  (pack(490, 490+5))
        int tupleA = packPareto(480, 4);
        int tupleB = packPareto(485, 3);
        int tupleC = packPareto(490, 5);

        builder.add(tupleA).add(tupleB).add(tupleC);
        // Initially, suppose the frontier contains all three.
        ParetoFront front1 = builder.build();
        int initialSize = front1.size();
        assertTrue(initialSize >= 1, "At least one tuple should be in frontier");

        // Now insert a new tuple D that "dominates" tuple B and C.
        // For instance, a tuple with earlier arrival and fewer changes.
        // Let tuple D be: arrival 480, 3 changes => pack(480, 480+3)
        int tupleD = packPareto(480, 3);
        builder.add(tupleD);
        ParetoFront front2 = builder.build();
        // Expect that tupleB and tupleC are removed because they are dominated by tupleD.
        // The frontier should now contain tupleA and tupleD.
        assertEquals(2, front2.size(), "Frontier should now contain 2 tuples after dominated removal");

        // Check that tupleD is present.
        assertEquals(tupleD, front2.get(480, 480+3), "Tuple D should be in the frontier");
    }

    // --- get() method exception tests ---

    @Test
    public void testGetThrowsForMissingTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Insert a tuple representing (500, 2) i.e. arrival=500, changes=2.
        int tuple = packPareto(500, 2);
        builder.add(tuple);
        ParetoFront front = builder.build();
        // Attempt to get a tuple that was not added.
        assertThrows(NoSuchElementException.class, () -> {
            front.get(480, 480+3);
        }, "Requesting a non-existing tuple should throw an exception");
    }

    // --- Builder addAll() and clear() tests ---

    @Test
    public void testAddAll() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        ParetoFront.Builder builder2 = new ParetoFront.Builder();

        int tuple1 = packPareto(480, 3);
        int tuple2 = packPareto(500, 2);
        builder1.add(tuple1);
        builder2.add(tuple2);

        // Add all from builder2 to builder1.
        builder1.addAll(builder2);
        ParetoFront combined = builder1.build();
        // Depending on ordering and dominance, the combined frontier should contain both if they are non-dominating.
        assertEquals(2, combined.size(), "Combined frontier should have 2 tuples");
    }

    @Test
    public void testClear() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(packPareto(480, 3)).add(packPareto(500, 2));
        assertFalse(builder.isEmpty(), "Builder should not be empty before clear");
        builder.clear();
        assertTrue(builder.isEmpty(), "Builder should be empty after clear");
    }

    // --- Test the toString() method ---

    @Test
    public void testToStringNotNull() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(packPareto(480, 3)).add(packPareto(500, 2));
        ParetoFront front = builder.build();
        String str = front.toString();
        assertNotNull(str, "toString() should not return null");
        assertFalse(str.isEmpty(), "toString() should return a non-empty string");
    }
}

