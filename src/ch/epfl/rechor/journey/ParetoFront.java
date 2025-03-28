package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;

import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static java.lang.System.arraycopy;


/**
 * Represents a Pareto frontier of optimization criteria.
 * The frontier is stored as an immutable array of packed tuples (long[]).
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class ParetoFront {

    /**
     * A constant representing an empty Pareto frontier.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    private final long[] tupleFront;

    /**
     * Private constructor to create a ParetoFront from a given array of packed tuples.
     * The array is cloned to ensure immutability.
     *
     * @param tupleFront the array of packed tuples
     */
    private ParetoFront(long[] tupleFront) {
        this.tupleFront = tupleFront.clone();
    }

    /**
     * Returns the size of the Pareto frontier, i.e., the number of tuples it contains.
     *
     * @return the number of tuples in the frontier
     */
    public int size() {
        return tupleFront.length;
    }

    /**
     * Retrieves the packed tuple with the given arrival time and number of changes.
     *
     * @param arrMins the arrival time in minutes
     * @param changes the number of changes
     * @return the packed tuple matching the criteria
     * @throws NoSuchElementException if no tuple matches the criteria
     */
    public long get(int arrMins, int changes) {
        for (long tuple : tupleFront) {
            if (PackedCriteria.arrMins(tuple) == arrMins && PackedCriteria.changes(tuple) == changes) {
                return tuple;
            }
        }
        throw new NoSuchElementException("No tuple with arrMins=" + arrMins + " and changes=" + changes);
    }

    /**
     * Applies the given action to each tuple in the frontier.
     *
     * @param action the action to apply to each tuple
     */
    public void forEach(LongConsumer action) {
        for (long value : tupleFront) {
            action.accept(value);
        }
    }

    /**
     * Returns a string representation of the Pareto frontier for debugging purposes.
     * The format includes departure time (if present), arrival time, and the number of changes.
     *
     * @return a string representation of the frontier
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParetoFront{");
        for (long tuple : tupleFront) {
            if (PackedCriteria.hasDepMins(tuple)) {
                sb.append("Departure time: ")
                        .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.depMins(tuple))))
                        .append(" ; ");
            }
            sb.append("Arrival time: ")
                    .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.arrMins(tuple))))
                    .append(" ; ");
            sb.append("Number of changes: ")
                    .append(PackedCriteria.changes(tuple))
                    .append(" ; \n");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * A builder for constructing ParetoFront instances.
     */
    public static class Builder {
        private static final int INITIAL_CAPACITY = 2;
        private long[] frontier;
        private int size;

        /**
         * Constructs an empty builder.
         */
        public Builder() {
            this.frontier = new long[INITIAL_CAPACITY];
            this.size = 0;
        }

        /**
         * Constructs a builder as a copy of another builder.
         *
         * @param that the builder to copy
         */
        public Builder(Builder that) {
            this.frontier = Arrays.copyOf(that.frontier, that.frontier.length);
            this.size = that.size;
        }

        /**
         * Checks if the frontier is empty.
         *
         * @return true if the frontier is empty, false otherwise
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Returns the current size of the frontier
         */
        public int getSize() {
            return size;
        }

        /**
         * Applies the given action to each tuple in the frontier
         *
         * @param action the action to apply to each tuple
         */
        public void forEach(LongConsumer action) {
            for (int i = 0; i < size; i++) {
                action.accept(frontier[i]);
            }
        }

        /**
         * Clears the frontier, removing all tuples
         */
        public Builder clear() {
            size = 0;
            return this;
        }

        /**
         * Adds a new tuple to the frontier if it is not dominated by any existing tuple.
         * Dominated tuples are removed from the frontier
         *
         * @param newTuple the packed tuple to add
         * @return this builder
         */
        public Builder add(long newTuple) {
            long newOrder = orderValue(newTuple);

            // Find the insertion position
            int pos = 0;
            while (pos < size && orderValue(frontier[pos]) < newOrder) {
                pos++;
            }

            // Check for dominance in the existing frontier
            for (int i = 0; i < pos; i++) {
                if (PackedCriteria.dominatesOrIsEqual(frontier[i], newTuple)) {
                    return this;
                }
            }

            // Remove dominated tuples
            int tempPos = pos;
            int tempSize = size;
            for (int j = pos; j < tempSize; j++) {
                if (!PackedCriteria.dominatesOrIsEqual(newTuple, frontier[j])) {
                    frontier[tempPos++] = frontier[j];
                } else {
                    size--;
                }
            }

            // Resize the array if necessary
            if (size == frontier.length) {
                frontier = Arrays.copyOf(frontier, (int) (frontier.length * 1.5));
            }

            // Insert the new tuple
            arraycopy(frontier, pos, frontier, pos + 1, tempPos - pos);
            frontier[pos] = newTuple;

            size = tempPos + 1;
            return this;
        }

        /**
         * Adds a new tuple with the given arrival time, number of changes, and payload.
         *
         * @param arrMins the arrival time in minutes
         * @param changes the number of changes
         * @param payload the payload
         * @return this builder for method chaining
         */
        public Builder add(int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }

        /**
         * Adds all tuples from another builder to this frontier.
         *
         * @param that the builder to copy tuples from
         * @return this builder for method chaining
         */
        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.frontier[i]);
            }
            return this;
        }

        /**
         * Computes the order value of a tuple (upper 32 bits).
         *
         * @param tuple the packed tuple
         * @return the order value
         */
        private long orderValue(long tuple) {
            return tuple & 0xFFFFFFFF00000000L; // keep only the bits from 32 to 50
        }

        /**
         * Checks if this frontier fully dominates another frontier when a departure time is added.
         *
         * @param that    the other frontier
         * @param depMins the departure time in minutes
         * @return true if this frontier dominates the other, false otherwise
         */
        public boolean fullyDominates(Builder that, int depMins) {
            int j = 0; // pointer for traversing 'this' frontier
            for (int i = 0; i < that.size; i++) {
                long tupleWithDep = PackedCriteria.withDepMins(that.frontier[i], depMins);
                while (j < this.size && !PackedCriteria.dominatesOrIsEqual(this.frontier[j], tupleWithDep)) {
                    j++;
                }
                if (j == this.size) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Builds an immutable ParetoFront from the current frontier.
         *
         * @return a new ParetoFront instance
         */
        public ParetoFront build() {
            return new ParetoFront(Arrays.copyOf(frontier, size));
        }

        /**
         * Returns a string representation of the frontier for debugging purposes.
         *
         * @return a string representation of the frontier
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ParetoFront{");
            for (int i = 0; i < size; i++) {
                if (PackedCriteria.hasDepMins(frontier[i])) {
                    sb.append("Departure time: ")
                            .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.depMins(frontier[i]))))
                            .append(" ; ");
                }
                sb.append("Arrival time: ")
                        .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.arrMins(frontier[i]))))
                        .append(" ; ");
                sb.append("Number of changes: ")
                        .append(PackedCriteria.changes(frontier[i]))
                        .append(" ; \n");
            }
            sb.append("}");
            return sb.toString();
        }
    }
}
