package ch.epfl.rechor.journey;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MyPackedCriteriaTest {

    /**
     * Test that packing valid criteria (with no departure)
     * produces a packed long from which the arrival minutes, changes, and payload
     * can be correctly extracted. Also, since no departure is provided, hasDepMins must be false.
     */
    @Test
    public void testPackValidNoDeparture() {
        int arrMins = 0;      // 0 minutes (valid: 0 is in [-240, 2880))
        int changes = 10;     // valid: 10 in [0,128)
        int payload = 123456789;

        long criteria = PackedCriteria.pack(arrMins, changes, payload);
        // With no departure provided, hasDepMins should be false.
        assertFalse(PackedCriteria.hasDepMins(criteria), "Criteria should not have a departure time");
        // After translation, extracting arrival should return the original arrival minutes.
        assertEquals(arrMins, PackedCriteria.arrMins(criteria), "Arrival minutes should be 0");
        assertEquals(changes, PackedCriteria.changes(criteria), "Changes should be 10");
        assertEquals(payload, PackedCriteria.payload(criteria), "Payload should match");
    }

    /**
     * Test that pack throws an exception when the arrival minutes are invalid.
     * Valid arrival minutes must lie in [-240, 2880).
     */
    @Test
    public void testPackInvalidArrMins() {
        int changes = 5;
        int payload = 0;
        // Test with arrMins too low (e.g. -241)
        assertThrows(IllegalArgumentException.class, () -> {
            PackedCriteria.pack(-241, changes, payload);
        }, "arrMins below -240 should throw an exception");
        // Test with arrMins too high (e.g. 2880)
        assertThrows(IllegalArgumentException.class, () -> {
            PackedCriteria.pack(2880, changes, payload);
        }, "arrMins >= 2880 should throw an exception");
    }

    /**
     * Test that pack throws an exception when the number of changes is invalid.
     * Valid changes must be in the range [0,128), i.e. 0 to 127.
     */
    @Test
    public void testPackInvalidChanges() {
        int arrMins = 0;
        int payload = 0;
        // Negative changes should be rejected.
        assertThrows(IllegalArgumentException.class, () -> {
            PackedCriteria.pack(arrMins, -1, payload);
        }, "Negative changes should throw an exception");
        // Changes equal to 128 (or more) are invalid.
        assertThrows(IllegalArgumentException.class, () -> {
            PackedCriteria.pack(arrMins, 128, payload);
        }, "Changes of 128 or more should throw an exception");
    }

    /**
     * Test that the helper methods correctly extract arrival minutes,
     * changes, and payload from a packed criteria.
     */
    @Test
    public void testUnpackFields() {
        int arrMins = 500;
        int changes = 3;
        int payload = 987654321;
        long criteria = PackedCriteria.pack(arrMins, changes, payload);

        assertEquals(arrMins, PackedCriteria.arrMins(criteria), "Arrival minutes mismatch");
        assertEquals(changes, PackedCriteria.changes(criteria), "Changes mismatch");
        assertEquals(payload, PackedCriteria.payload(criteria), "Payload mismatch");
    }

    /**
     * Test adding a departure time using withDepMins.
     * Starting from criteria with no departure, we add a departure time (e.g., 600)
     * and then verify that hasDepMins becomes true and depMins returns the correct value.
     */
    @Test
    public void testWithDepMinsAndDepMinsRetrieval() {
        int arrMins = 300;
        int changes = 2;
        int payload = 555;
        long criteria = PackedCriteria.pack(arrMins, changes, payload);
        assertFalse(PackedCriteria.hasDepMins(criteria), "Criteria should initially have no departure");

        int depTime = 600; // valid departure time in [-240, 2880)
        long criteriaWithDep = PackedCriteria.withDepMins(criteria, depTime);
        assertTrue(PackedCriteria.hasDepMins(criteriaWithDep), "Criteria should now have a departure time");
        assertEquals(depTime, PackedCriteria.depMins(criteriaWithDep), "Departure minutes should match the provided value");
    }

    /**
     * Test that calling depMins on criteria without a departure time throws an exception.
     */
    @Test
    public void testDepMinsWithoutDeparture() {
        int arrMins = 0;
        int changes = 0;
        int payload = 0;
        long criteria = PackedCriteria.pack(arrMins, changes, payload);
        assertThrows(IllegalArgumentException.class, () -> {
            PackedCriteria.depMins(criteria);
        }, "Calling depMins without a departure time should throw an exception");
    }

    /**
     * Test that withPayload updates only the payload field without affecting arrival or changes.
     */
    @Test
    public void testWithPayload() {
        int arrMins = 200;
        int changes = 4;
        int initialPayload = 1111;
        long criteria = PackedCriteria.pack(arrMins, changes, initialPayload);
        int newPayload = 2222;
        long updatedCriteria = PackedCriteria.withPayload(criteria, newPayload);

        assertEquals(newPayload, PackedCriteria.payload(updatedCriteria), "Payload should be updated");
        assertEquals(arrMins, PackedCriteria.arrMins(updatedCriteria), "Arrival minutes should remain unchanged");
        assertEquals(changes, PackedCriteria.changes(updatedCriteria), "Changes should remain unchanged");
    }

    /**
     * Test the dominatesOrIsEqual method for criteria without departure.
     * Lower arrival (earlier arrival) and fewer changes are considered better.
     */
    @Test
    public void testDominatesOrIsEqualWithoutDeparture() {
        // Create two criteria:
        // c1: arrival = 0, changes = 1
        // c2: arrival = 10, changes = 2
        long c1 = PackedCriteria.pack(0, 1, 0);
        long c2 = PackedCriteria.pack(10, 2, 0);

        assertTrue(PackedCriteria.dominatesOrIsEqual(c1, c2), "c1 should dominate c2 (earlier arrival and fewer changes)");
        assertFalse(PackedCriteria.dominatesOrIsEqual(c2, c1), "c2 should not dominate c1");

        // Equal criteria should dominate each other.
        long c3 = PackedCriteria.pack(5, 3, 100);
        assertTrue(PackedCriteria.dominatesOrIsEqual(c3, c3), "A criteria should dominate itself");
    }

    /**
     * Test the dominatesOrIsEqual method for criteria with departure.
     * When departure times are present, a later actual departure is preferred.
     */
    @Test
    public void testDominatesOrIsEqualWithDeparture() {
        // First, create base criteria (without departure) with same arrival and changes.
        int arrMins = 400;
        int changes = 2;
        int payload = 0;
        long base = PackedCriteria.pack(arrMins, changes, payload);

        // Add different departure times.
        // Let d1 = 700 and d2 = 600; since later departure (higher actual time) is better,
        // c1 should dominate c2.
        long c1 = PackedCriteria.withDepMins(base, 700);
        long c2 = PackedCriteria.withDepMins(base, 600);

        assertTrue(PackedCriteria.dominatesOrIsEqual(c1, c2), "c1 with later departure should dominate c2");
        assertFalse(PackedCriteria.dominatesOrIsEqual(c2, c1), "c2 with earlier departure should not dominate c1");
    }

    /**
     * Test that withoutDepMins removes the departure time from criteria.
     */
    @Test
    public void testWithoutDepMins() {
        int arrMins = 500;
        int changes = 3;
        int payload = 333;
        long criteria = PackedCriteria.pack(arrMins, changes, payload);
        // First, add a departure time.
        int depTime = 800;
        long criteriaWithDep = PackedCriteria.withDepMins(criteria, depTime);
        assertTrue(PackedCriteria.hasDepMins(criteriaWithDep), "Criteria should have a departure time");
        // Now remove it.
        long criteriaNoDep = PackedCriteria.withoutDepMins(criteriaWithDep);
        assertFalse(PackedCriteria.hasDepMins(criteriaNoDep), "Departure should have been removed");
    }

}

