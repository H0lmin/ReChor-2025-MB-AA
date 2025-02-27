package ch.epfl.rechor.journey;

import static ch.epfl.rechor.Preconditions.checkArgument;

public final class PackedCriteria {

    // Constants for bit positions and masks.
    private static final int DEP_SHIFT = 51;         // departure time bits: 12 bits, bits 62-51
    private static final int ARR_SHIFT = 39;         // arrival time bits: 12 bits, bits 50-39
    private static final int CHG_SHIFT = 32;         // changes bits: 7 bits, bits 38-32
    private static final long PAYLOAD_MASK = 0xFFFFFFFFL;  // payload: lower 32 bits

    private static final long DEP_MASK = 0xFFFL;       // 12-bit mask for departure time
    private static final long ARR_MASK = 0xFFFL;       // 12-bit mask for arrival time
    private static final long CHG_MASK = 0x7FL;        // 7-bit mask for number of changes

    // The origin for time translation: times are given in minutes since midnight,
    // but valid times are between -240 and 2880. We add 240 so that the stored value is always nonnegative.
    private static final int TIME_TRANSLATION = 240;
    // For departure times we store the complement relative to 4095.
    private static final int MAX_STORED_TIME = 4095;

    private PackedCriteria() {
    }

    /**
     * Packs the arrival time (in minutes since midnight), number of changes,
     * and payload into a 64-bit long. These criteria do not include a departure time,
     * so the departure bits are set to 0.
     *
     * @param arrMins the arrival time in minutes since midnight; must be in [-240, 2880)
     * @param changes the number of changes; must be between 0 and 127
     * @param payload the payload (an arbitrary 32-bit value)
     * @return the packed criteria as a long
     * @throws IllegalArgumentException if the arrival time or changes are out of range
     */
    public static long pack(int arrMins, int changes, int payload) {
        checkArgument(arrMins >= -TIME_TRANSLATION && arrMins < 2880);
        checkArgument(changes >= 0 && changes < 128);

        // Translate arrival minutes so that -240 becomes 0.
        int arrTranslated = arrMins + TIME_TRANSLATION;  // valid range: 0 .. 3119 (fits in 12 bits)

        // Since no departure is provided, its bits are 0.
        long depPart = 0L;
        long arrPart = ((long) arrTranslated & ARR_MASK) << ARR_SHIFT;
        long chgPart = ((long) changes & CHG_MASK) << CHG_SHIFT;
        long payloadPart = Integer.toUnsignedLong(payload) & PAYLOAD_MASK;

        return depPart | arrPart | chgPart | payloadPart;
    }

    /**
     * Returns true if the packed criteria include a departure time.
     *
     * @param criteria the packed criteria
     * @return true if departure time bits (bits 62–51) are nonzero, false otherwise
     */
    public static boolean hasDepMins(long criteria) {
        return ((criteria >>> DEP_SHIFT) & DEP_MASK) != 0;
    }

    /**
     * Retrieves the departure time (in minutes since midnight) from the packed criteria.
     * The departure time is stored as the complement relative to MAX_STORED_TIME.
     *
     * @param criteria the packed criteria
     * @return the departure time in minutes since midnight
     * @throws IllegalArgumentException if the criteria do not include a departure time
     */
    public static int depMins(long criteria) {
        checkArgument(hasDepMins(criteria));

        int storedDep = (int) ((criteria >>> DEP_SHIFT) & DEP_MASK);
        // Convert back: actual departure minutes = (MAX_STORED_TIME - storedDep) - TIME_TRANSLATION.
        return (MAX_STORED_TIME - storedDep) - TIME_TRANSLATION;
    }

    /**
     * Retrieves the arrival time (in minutes since midnight) from the packed criteria.
     *
     * @param criteria the packed criteria
     * @return the arrival time in minutes since midnight
     */
    public static int arrMins(long criteria) {
        int storedArr = (int) ((criteria >>> ARR_SHIFT) & ARR_MASK);
        return storedArr - TIME_TRANSLATION;
    }

    /**
     * Retrieves the number of changes from the packed criteria.
     *
     * @param criteria the packed criteria
     * @return the number of changes
     */
    public static int changes(long criteria) {
        return (int) ((criteria >>> CHG_SHIFT) & CHG_MASK);
    }

    /**
     * Retrieves the payload from the packed criteria.
     *
     * @param criteria the packed criteria
     * @return the payload as an int
     */
    public static int payload(long criteria) {
        return (int) (criteria & PAYLOAD_MASK);
    }

    /**
     * Returns true if the first set of packed criteria dominates or is equal to the second.
     * Domination (or equality) is defined component-wise. Note that both criteria must either include
     * a departure time or not.
     * <p>
     * For criteria that include a departure time, the comparisons are:
     * - Departure: lower stored value (i.e. later actual departure) is better.
     * - Arrival: lower stored value (i.e. earlier actual arrival) is better.
     * - Changes: lower number is better.
     * <p>
     * For criteria without a departure time, only arrival and changes are compared.
     *
     * @param criteria1 the first packed criteria
     * @param criteria2 the second packed criteria
     * @return true if criteria1 dominates or is equal to criteria2
     * @throws IllegalArgumentException if one criterion includes a departure time and the other does not
     */
    public static boolean dominatesOrIsEqual(long criteria1, long criteria2) {
        checkArgument(hasDepMins(criteria1) == hasDepMins(criteria2));

        if (hasDepMins(criteria1)){
            return (depMins(criteria1) >= depMins(criteria2)) && arrMins(criteria1) <= arrMins(criteria2) &&
                    changes(criteria1) <= changes(criteria2);
        } else {
            return arrMins(criteria1) <= arrMins(criteria2) && changes(criteria1) <= changes(criteria2);
        }
    }


    /**
     * Returns packed criteria identical to the given ones but with no departure time.
     *
     * @param criteria the original packed criteria
     * @return the criteria with departure bits set to 0
     */
    public static long withoutDepMins(long criteria) {
        return criteria & ~(DEP_MASK << DEP_SHIFT);
    }

    /**
     * Returns packed criteria identical to the given ones but with the specified departure time.
     * The departure time is provided in minutes since midnight and will be stored as its complement.
     *
     * @param criteria the original packed criteria
     * @param depMins1 the new departure time in minutes since midnight; must be in [-240, 2880)
     * @return the criteria with the updated departure time
     */
    public static long withDepMins(long criteria, int depMins1) {

        // Compute stored departure as the complement relative to MAX_STORED_TIME.
        int storedDep = MAX_STORED_TIME - (depMins1 + TIME_TRANSLATION);
        return (criteria & ~(DEP_MASK << DEP_SHIFT)) | (((long) storedDep & DEP_MASK) << DEP_SHIFT);
    }

    /**
     * Returns packed criteria identical to the given ones but with one additional change.
     *
     * @param criteria the original packed criteria
     * @return the criteria with the number of changes incremented by 1
     * @throws IllegalArgumentException if the number of changes would exceed 127
     */
    public static long withAdditionalChange(long criteria) {
        int currentChanges = changes(criteria);

        int newChanges = currentChanges + 1;
        return (criteria & ~(CHG_MASK << CHG_SHIFT)) | (((long) newChanges & CHG_MASK) << CHG_SHIFT);
    }

    /**
     * Returns packed criteria identical to the given ones but with the specified payload.
     *
     * @param criteria the original packed criteria
     * @param payload1 the new payload (a 32-bit value)
     * @return the criteria with the updated payload
     */
    public static long withPayload(long criteria, int payload1) {
        return (criteria & ~PAYLOAD_MASK) | (Integer.toUnsignedLong(payload1) & PAYLOAD_MASK);
    }
}
