package ch.epfl.rechor;

public final class Preconditions {
    private Preconditions() {}

    public static void checkArgument(boolean shouldBeTrue) throws IllegalArgumentException {
        if (!shouldBeTrue) {
            throw new IllegalArgumentException("Argument condition failed");
        }
    }
}
