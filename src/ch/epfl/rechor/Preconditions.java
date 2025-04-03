package ch.epfl.rechor;

/**
 * Utility class for validating method arguments.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class Preconditions {
    private Preconditions() {
    }

    /**
     * Checks if the provided condition is true.
     *
     * @param shouldBeTrue The condition to check. If false, an exception will be thrown.
     * @throws IllegalArgumentException if the condition is false.
     */
    public static void checkArgument(boolean shouldBeTrue) {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException("Argument condition failed");
        }
    }
}
