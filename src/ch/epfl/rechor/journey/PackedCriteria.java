package ch.epfl.rechor.journey;

import static ch.epfl.rechor.Preconditions.checkArgument;

public final class PackedCriteria {
    private static final int DEP_SHIFT = 51;
    private static final int ARR_SHIFT = 39;
    private static final int CHG_SHIFT = 32;
    private static final long PAYLOAD_MASK = 0xFFFFFFFFL;

    private static final long SHIFT_MASK = 0xFFFL;
    private static final long CHG_MASK = 0x7FL;

    private static final int TIME_TRANSLATION = 240;
    private static final int MAX_STORED_TIME = 4095;


    private PackedCriteria(){
    }

    public static long pack (int arrMins, int changes, int payload){
        checkArgument(arrMins >= -TIME_TRANSLATION && arrMins < 2880);
        checkArgument(changes >= 0 && changes < 128);

        long arrPart = ((long) (arrMins + TIME_TRANSLATION) & SHIFT_MASK) << ARR_SHIFT;
        long chgPart = ((long) changes & CHG_MASK) << CHG_SHIFT;
        long payloadPart = Integer.toUnsignedLong(payload) & PAYLOAD_MASK;

        return arrPart | chgPart | payloadPart;
    }

    public static boolean hasDepMins (long criteria){
        return ((criteria >> DEP_SHIFT) & SHIFT_MASK) != 0;
    }

    public static int depMins (long criteria){
        checkArgument(hasDepMins(criteria));

        int storedDepTime = (int) ((criteria >> DEP_SHIFT) & SHIFT_MASK);
        return MAX_STORED_TIME - storedDepTime - TIME_TRANSLATION;
    }

    public static int arrMins(long criteria){
        int storedArrTime = (int) ((criteria >> ARR_SHIFT) & SHIFT_MASK);
        return storedArrTime - TIME_TRANSLATION;
    }

    public static int changes(long criteria){
        return (int) ((criteria >> CHG_SHIFT) & CHG_MASK);
    }

    public static int payload (long criteria){
        return (int) (criteria & PAYLOAD_MASK);
    }

    public static boolean dominatesOrIsEqual (long criteria1, long criteria2){
        checkArgument(hasDepMins(criteria1) == hasDepMins(criteria2));

        if (hasDepMins(criteria1)){
            return (depMins(criteria1) >= depMins(criteria2)) && (arrMins(criteria1) <= arrMins(criteria2))
                    && (changes(criteria1) <= changes(criteria2));
        }
        return arrMins(criteria1) <= arrMins(criteria2) && changes(criteria1) <= changes(criteria2);
    }

    public static long withoutDepMins(long criteria){
        return criteria & ~(SHIFT_MASK << DEP_SHIFT);
    }

    public static long withDepMins(long criteria, int depMins1){
        long newDepTime = (((MAX_STORED_TIME - depMins1 - TIME_TRANSLATION)) & SHIFT_MASK) << DEP_SHIFT;
        return (criteria & ~(SHIFT_MASK << DEP_SHIFT)) | newDepTime;
    }

    public static long withAdditionalChange(long criteria){
        return (criteria & ~(CHG_MASK << CHG_SHIFT)) | (((long) (changes(criteria) + 1) & CHG_MASK) << CHG_SHIFT);
    }

    public static long withPayload(long criteria, int payload1){
        return (criteria & ~PAYLOAD_MASK) | (Integer.toUnsignedLong(payload1) & PAYLOAD_MASK);
    }
}