package ch.epfl.rechor.journey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static ch.epfl.rechor.journey.PackedCriteria.pack;
import static ch.epfl.rechor.journey.PackedCriteria.withDepMins;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;


public class MyParetoFrontTest {

    // ----------------------------------------------------------------------
    // ParetoFront basic tests
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("ParetoFront.EMPTY is empty")
    void testEmptyParetoFront() {
        ParetoFront empty = ParetoFront.EMPTY;
        assertEquals(0, empty.size());
    }

    // ----------------------------------------------------------------------
    // Builder basic behavior
    // ----------------------------------------------------------------------

    @Test
    @DisplayName("Builder is initially empty")
    void testBuilderInitiallyEmpty() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        assertTrue(builder.isEmpty());
    }

    @Test
    @DisplayName("Add single tuple to builder using add(long)")
    void testAddSingleTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple = pack(480, 3, 100);
        builder.add(tuple);
        assertFalse(builder.isEmpty());
        AtomicInteger count = new AtomicInteger();
        builder.forEach(val -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("Adding duplicate tuple does not change builder")
    void testAddDuplicateTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long tuple = pack(480, 3, 0);
        builder.add(tuple);
        builder.add(tuple);
        AtomicInteger count = new AtomicInteger();
        builder.forEach(val -> count.incrementAndGet());
        assertEquals(1, count.get());
    }

    @Test
    @DisplayName("Adding dominated tuple is rejected")
    void testAddDominatedTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Add tuple A: (480, 3, 0)
        long A = pack(480, 3, 0);
        builder.add(A);
        // Add tuple B: (480, 4, 0) is worse (more changes) than A.
        long B = pack(480, 4, 0);
        builder.add(B);
        AtomicInteger count = new AtomicInteger();
        List<Long> elements = new ArrayList<>();
        builder.forEach(val -> { count.incrementAndGet(); elements.add(val); });
        assertEquals(1, count.get());
        assertEquals(A, elements.getFirst());
    }

    @Test
    @DisplayName("Adding tuple that dominates existing tuples replaces them")
    void testAddDominatingTuple() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        // Start with two tuples:
        // A: (480, 3, 0) and C: (481, 2, 0) – these are incomparable.
        long A = pack(480, 3, 0);
        long C = pack(481, 2, 0);
        builder.add(A);
        builder.add(C);
        // Now add D: (480, 2, 0) which dominates A and also dominates C:
        //   (480,2,0) is better than (480,3,0) (same arrival, fewer changes)
        //   and (480,2,0) is better than (481,2,0) (earlier arrival, same changes).
        long D = pack(480, 2, 0);
        builder.add(D);
        AtomicInteger count = new AtomicInteger();
        List<Long> elements = new ArrayList<>();
        builder.forEach(val -> { count.incrementAndGet(); elements.add(val); });
        assertEquals(1, count.get());
        assertEquals(D, elements.getFirst());
    }

    @Test
    @DisplayName("Using add(arrMins, changes, payload) works correctly")
    void testAddUsingComponents() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(480, 3, 0);
        AtomicInteger count = new AtomicInteger();
        List<Long> elems = new ArrayList<>();
        builder.forEach(val -> { count.incrementAndGet(); elems.add(val); });
        assertEquals(1, count.get());
        long expected = pack(480, 3, 0);
        assertEquals(expected, elems.getFirst());
    }

    @Test
    @DisplayName("addAll merges two builders correctly")
    void testAddAll() {
        ParetoFront.Builder builder1 = new ParetoFront.Builder();
        ParetoFront.Builder builder2 = new ParetoFront.Builder();
        // builder1 has A: (480, 3, 0)
        long A = pack(480, 3, 0);
        builder1.add(A);
        // builder2 has B: (481, 2, 0) and C: (490, 4, 0)
        long B = pack(481, 2, 0);
        long C = pack(490, 4, 0);
        builder2.add(B);
        builder2.add(C);
        builder1.addAll(builder2);
        // Expected frontier: {A, B} because B dominates C:
        //   Compare B: (481,2,0) vs C: (490,4,0): 481 < 490 and 2 < 4.
        List<Long> result = new ArrayList<>();
        builder1.forEach(result::add);
        assertEquals(2, result.size());
        // Lexicographic order: A then B.
        assertEquals(A, result.get(0));
        assertEquals(B, result.get(1));
    }

    @Test
    @DisplayName("Copy constructor creates an independent Builder")
    void testBuilderCopyConstructor() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long A = pack(480, 3, 0);
        builder.add(A);
        ParetoFront.Builder copy = new ParetoFront.Builder(builder);
        long B = pack(481, 2, 0);
        copy.add(B);
        AtomicInteger origCount = new AtomicInteger();
        builder.forEach(val -> origCount.incrementAndGet());
        assertEquals(1, origCount.get());
        AtomicInteger copyCount = new AtomicInteger();
        List<Long> copyElems = new ArrayList<>();
        copy.forEach(val -> { copyCount.incrementAndGet(); copyElems.add(val); });
        assertEquals(2, copyCount.get());
        assertEquals(A, copyElems.get(0));
        assertEquals(B, copyElems.get(1));
    }

    @Test
    @DisplayName("clear() empties the Builder")
    void testClear() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(pack(480, 3, 0));
        builder.clear();
        assertTrue(builder.isEmpty());
        AtomicInteger count = new AtomicInteger();
        builder.forEach(val -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    @DisplayName("forEach iterates tuples in lexicographic order")
    void testForEachOrder() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long t1 = pack(490, 4, 0);
        long t2 = pack(480, 3, 0);
        long t3 = pack(485, 2, 0);
        // Add in random order.
        builder.add(t1)
                .add(t2)
                .add(t3);
        List<Long> ordered = new ArrayList<>();
        builder.forEach(ordered::add);
        // Expected order: t2, t3, t1.
        assertEquals(2, ordered.size());
        assertEquals(t2, ordered.get(0));
        assertEquals(t3, ordered.get(1));
    }

    @Test
    @DisplayName("build() returns an immutable ParetoFront with correct content")
    void testBuildParetoFront() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long t1 = pack(480, 3, 0);
        long t2 = pack(485, 2, 0);
        builder.add(t1);
        builder.add(t2);
        ParetoFront front = builder.build();
        assertEquals(2, front.size());
        List<Long> frontElems = new ArrayList<>();
        front.forEach(frontElems::add);
        assertEquals(2, frontElems.size());
        // Expected lex order: t1 then t2.
        assertEquals(t1, frontElems.get(0));
        assertEquals(t2, frontElems.get(1));
        // Ensure immutability: modifying builder afterwards does not affect 'front'
        builder.clear();
        ParetoFront newFront = builder.build();
        assertEquals(0, newFront.size());
        List<Long> unchanged = new ArrayList<>();
        front.forEach(unchanged::add);
        assertEquals(2, unchanged.size());
        assertEquals(t1, unchanged.get(0));
        assertEquals(t2, unchanged.get(1));
    }

    @Test
    @DisplayName("get(arrMins, changes) retrieves correct tuple or throws exception")
    void testGetMethod() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        long t1 = pack(480, 3, 10);
        long t2 = pack(485, 2, 20);
        builder.add(t1);
        builder.add(t2);
        ParetoFront front = builder.build();
        // Expect that get(480, 3) returns t1.
        long result = front.get(480, 3);
        assertEquals(t1, result);
        // For non-existent criteria, expect a NoSuchElementException.
        assertThrows(NoSuchElementException.class, () -> front.get(480, 2));
    }

    @Test
    @DisplayName("toString() returns a non-null, non-empty string")
    void testToString() {
        ParetoFront.Builder builder = new ParetoFront.Builder();
        builder.add(pack(480, 3, 0));
        ParetoFront front = builder.build();
        String str = front.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }

    // ----------------------------------------------------------------------
    // Tests for fullyDominates method
    // ----------------------------------------------------------------------
    @Test
    @DisplayName("fullyDominates returns true when receiver fully dominates the other builder")
    void testFullyDominatesTrue() {
        // In our interpretation, we assume that fullyDominates(Builder that, depMins)
        // subtracts depMins from the arrival times of tuples in 'that' and compares them
        // (i.e. effective travel time) with tuples in the receiver.
        // Let depMins = 480.
        // Builder A has tuple (500, 1, 0) → effective (20,1)
        // Builder B has tuple (510, 2, 0) → effective (30,2)
        ParetoFront.Builder A = new ParetoFront.Builder();
        ParetoFront.Builder B = new ParetoFront.Builder();

        long arr1= pack(500,1,0);
        A.add(withDepMins(arr1, 490));
        B.add(pack(510, 2, 0));
        assertTrue(A.fullyDominates(B, 480));
    }

    @Test
    @DisplayName("fullyDominates returns false when receiver does not fully dominate the other builder")
    void testFullyDominatesFalse() {
        // Builder A: (500,1,0) → effective (20,1)
        // Builder B: (495,2,0) → effective (15,2)
        // Here, (20,1) does not dominate (15,2) because 20 > 15.
        ParetoFront.Builder A = new ParetoFront.Builder();
        ParetoFront.Builder B = new ParetoFront.Builder();
        long arr1= pack(800,2,2);
        A.add(withDepMins(arr1, 490));
        B.add(pack(495, 2, 0));
        assertFalse(A.fullyDominates(B, 480));
    }

    private static final int DEP_MINS = 500;

    @Test
    public void testFullyDominates() {
        ParetoFront.Builder builderA = new ParetoFront.Builder();
        ParetoFront.Builder builderB = new ParetoFront.Builder();

        long arr1= pack(800,2,2);
        long arr2= pack(800,3,3);
        builderA.add(withDepMins(arr1,810));
        builderA.add(withDepMins(arr2,810));

        builderB.add(800, 2, 2);
        builderB.add(810, 3, 3);

        boolean result = builderA.fullyDominates(builderB, 800);
        assertTrue(result, "BuilderA should fully dominate BuilderB with departure time 800");
    }
    @Test
    public void testFullyDominatesEmptyThat() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();
        // Add a tuple with departure time set.
        long tuple = withDepMins(PackedCriteria.pack(600, 1, 0), DEP_MINS);
        dominating.add(tuple);

        ParetoFront.Builder dominated = new ParetoFront.Builder(); // empty builder
        assertTrue(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test that a dominating builder fully dominates a dominated builder with one tuple.
     * Here the dominating tuple has exactly the same criteria (after repacking) as the dominated one.
     */
    @Test
    public void testFullyDominatesTrueSingleTuple() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();
        // Create a tuple with departure time already set.
        long domTuple = withDepMins(PackedCriteria.pack(600, 1, 0), DEP_MINS);
        dominating.add(domTuple);

        ParetoFront.Builder dominated = new ParetoFront.Builder();
        // Add a tuple without departure (it will be repacked in fullyDominates).
        dominated.add(PackedCriteria.pack(600, 1, 10));

        // The repacked dominated tuple will have arrival 600 and 1 change,
        // which is exactly equal to the dominating tuple.
        assertTrue(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test failure when the dominated tuple has an earlier arrival time.
     * Since a lower arrival is better, the dominating tuple (with arrival 600)
     * does not dominate a tuple with arrival 590.
     */
    @Test
    public void testFullyDominatesFalseDueToArrival() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();
        long domTuple = withDepMins(PackedCriteria.pack(600, 1, 0), DEP_MINS);
        dominating.add(domTuple);

        ParetoFront.Builder dominated = new ParetoFront.Builder();
        dominated.add(PackedCriteria.pack(590, 1, 10));

        // Here, 600 > 590 (i.e. later arrival) so domination fails.
        assertFalse(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test failure when the dominated tuple is better in the number of changes.
     * The dominating tuple has 1 change while the dominated tuple has 0 changes.
     */
    @Test
    public void testFullyDominatesFalseDueToChanges() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();
        long domTuple = withDepMins(PackedCriteria.pack(600, 1, 0), DEP_MINS);
        dominating.add(domTuple);

        ParetoFront.Builder dominated = new ParetoFront.Builder();
        // Dominated tuple has fewer changes (0) making it better.
        dominated.add(PackedCriteria.pack(600, 0, 10));

        assertFalse(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test that domination holds when both builders have multiple tuples,
     * each dominated tuple is matched by a dominating tuple.
     */
    @Test
    public void testFullyDominatesMultipleTuples() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();
        // Two dominating tuples with departure times set.
        long tuple1 = withDepMins(PackedCriteria.pack(600, 1, 0), DEP_MINS);
        long tuple2 = withDepMins(PackedCriteria.pack(620, 2, 0), DEP_MINS);
        dominating.add(tuple1);
        dominating.add(tuple2);

        ParetoFront.Builder dominated = new ParetoFront.Builder();
        // Two dominated tuples that, when repacked with DEP_MINS, are exactly matched.
        dominated.add(PackedCriteria.pack(600, 1, 10));
        dominated.add(PackedCriteria.pack(620, 2, 20));

        assertTrue(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test failure when not every dominated tuple is dominated.
     * In this case, the dominating builder only has one tuple that dominates the first dominated tuple.
     * The second dominated tuple has no matching dominating tuple.
     */
    @Test
    public void testFullyDominatesFailureWhenNotAllDominated() {
        ParetoFront.Builder dominating = new ParetoFront.Builder();

        ParetoFront.Builder dominated = new ParetoFront.Builder();
        dominated.add(PackedCriteria.pack(600, 1, 10)); // dominated by the available tuple
        dominated.add(PackedCriteria.pack(620, 2, 20)); // not dominated (no tuple in 'dominating' with criteria as good)
        long arr1= PackedCriteria.pack(800,2,2);
        long arr2= PackedCriteria.pack(800,3,3);
        dominating.add(withDepMins(arr1,810));
        dominating.add(withDepMins(arr2,810));
        assertFalse(dominating.fullyDominates(dominated, DEP_MINS));
    }

    /**
     * Test that an empty dominating builder does not fully dominate a non-empty dominated builder.
     */
    @Test
    public void testEmptyDominatingFails() {
        ParetoFront.Builder dominating = new ParetoFront.Builder(); // empty
        ParetoFront.Builder dominated = new ParetoFront.Builder();
        dominated.add(PackedCriteria.pack(600, 1, 10));
        assertFalse(dominating.fullyDominates(dominated, DEP_MINS));
    }
}
