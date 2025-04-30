package ch.epfl.rechor.journey;

import static ch.epfl.rechor.Preconditions.checkArgument;

/**
 * Utility class for packing and unpacking journey criteria into a 64-bit long.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class PackedCriteria {

    private static final int DEP_SHIFT = 51;
    private static final int ARR_SHIFT = 39;
    private static final int CHG_SHIFT = 32;

    private static final long PAYLOAD_MASK = 0xFFFFFFFFL;
    private static final long DEP_MASK = 0xFFFL;
    private static final long ARR_MASK = 0xFFFL;
    private static final long CHG_MASK = 0x7FL;

    private static final int TIME_TRANSLATION = 240;
    private static final int MAX_STORED_TIME = 4095;

    private PackedCriteria() {
    }

    // Helpers for translating and complementing minutes values
    private static int encodeArrMins(int arrMins) {
        return arrMins + TIME_TRANSLATION;
    }

    private static int decodeArrMins(int storedArr) {
        return storedArr - TIME_TRANSLATION;
    }

    private static int encodeDepMins(int depMins) {
        return MAX_STORED_TIME - encodeArrMins(depMins);
    }

    private static int decodeDepMins(int storedDep) {
        return (MAX_STORED_TIME - storedDep) - TIME_TRANSLATION;
    }

    /**
     * Packs the arrival time, number of changes, and payload into a 64-bit long.
     *
     * @param arrMins the arrival time; must be in [-240, 2880)
     * @param changes the number of changes; must be between 0 and 127
     * @param payload the payload
     * @throws IllegalArgumentException if the arrival time or changes are out of range
     */
    public static long pack(int arrMins, int changes, int payload) {
        checkArgument(arrMins >= -TIME_TRANSLATION && arrMins < 2880);
        checkArgument(changes >= 0 && changes < 128);

        int arrTranslated = encodeArrMins(arrMins);
        long arrPart = (long) arrTranslated << ARR_SHIFT;
        long chgPart = (long) changes << CHG_SHIFT;
        long payloadPart = Integer.toUnsignedLong(payload);

        return arrPart | chgPart | payloadPart;
    }

    /**
     * Returns true if the packed criteria include a departure time.
     *
     * @param criteria the packed criteria
     * @return true if departure time bits (bits 62–51) are nonzero
     */
    public static boolean hasDepMins(long criteria) {
        return ((criteria >>> DEP_SHIFT) & DEP_MASK) != 0;
    }

    /**
     * Retrieves the departure time (in minutes since midnight) from the packed criteria.
     *
     * @param criteria the packed criteria
     * @throws IllegalArgumentException if no departure time is stored
     */
    public static int depMins(long criteria) {
        checkArgument(hasDepMins(criteria));
        int storedDep = (int) ((criteria >>> DEP_SHIFT) & DEP_MASK);
        return decodeDepMins(storedDep);
    }

    /**
     * Retrieves the arrival time (in minutes since midnight) from the packed criteria.
     *
     * @param criteria the packed criteria
     * @return the arrival time in minutes since midnight
     */
    public static int arrMins(long criteria) {
        int storedArr = (int) ((criteria >>> ARR_SHIFT) & ARR_MASK);
        return decodeArrMins(storedArr);
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
     *
     * @param criteria1 the first packed criteria
     * @param criteria2 the second packed criteria
     * @throws IllegalArgumentException if departure presence differs
     */
    public static boolean dominatesOrIsEqual(long criteria1, long criteria2) {
        checkArgument(hasDepMins(criteria1) == hasDepMins(criteria2));

        if (hasDepMins(criteria1)) {
            return (depMins(criteria1) >= depMins(criteria2))
                    && arrMins(criteria1) <= arrMins(criteria2)
                    && changes(criteria1) <= changes(criteria2);
        } else {
            return arrMins(criteria1) <= arrMins(criteria2)
                    && changes(criteria1) <= changes(criteria2);
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
     *
     * @param criteria the original packed criteria
     * @param depMins1 the new departure time in minutes since midnight; must be in [-240, 2880)
     * @return the criteria with the updated departure time
     */
    public static long withDepMins(long criteria, int depMins1) {
        int storedDep = encodeDepMins(depMins1);
        return (criteria & ~(DEP_MASK << DEP_SHIFT))
                | (((long) storedDep & DEP_MASK) << DEP_SHIFT);
    }

    /**
     * Returns packed criteria identical to the given ones but with one additional change.
     *
     * @param criteria the original packed criteria
     * @throws IllegalArgumentException if the number of changes would exceed 127
     */
    public static long withAdditionalChange(long criteria) {
        int newChanges = changes(criteria) + 1;
        return (criteria & ~(CHG_MASK << CHG_SHIFT))
                | (((long) newChanges & CHG_MASK) << CHG_SHIFT);
    }

    /**
     * Returns packed criteria identical to the given ones but with the specified payload.
     *
     * @param criteria the original packed criteria
     * @param payload1 the new payload
     * @return the criteria with the updated payload
     */
    public static long withPayload(long criteria, int payload1) {
        return (criteria & ~PAYLOAD_MASK) | (Integer.toUnsignedLong(payload1) & PAYLOAD_MASK);
    }
}