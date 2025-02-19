package ch.epfl.rechor;

/**
 * Utility class for validating method arguments.
 * This class provides a method for checking if a condition is true and throws an exception if it is not.
 * This class cannot be instantiated.
 */
public final class Preconditions {
    private Preconditions() {}

    /**
     * Checks if the provided condition is true. If it is false, an {@link IllegalArgumentException} is thrown.
     *
     * @param shouldBeTrue The condition to check. If false, an exception will be thrown.
     * @throws IllegalArgumentException if the condition is false.
     */
    public static void checkArgument(boolean shouldBeTrue) throws IllegalArgumentException {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException("Argument condition failed");
        }
    }
}
