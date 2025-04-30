package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static ch.epfl.rechor.journey.PackedCriteria.*;

/**
 * Represents a Pareto frontier for journey criteria.
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class ParetoFront {

    /**
     * An empty Pareto frontier.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    private final long[] tupleFront;

    private ParetoFront(long[] tupleFront) {
        this.tupleFront = tupleFront;
    }

    /**
     * @return the number of tuples in the Pareto frontier.
     */
    public int size() {
        return tupleFront.length;
    }

    /**
     * Retrieves the tuple matching the specified arrival minutes and number of changes.
     *
     * @param arrMins the arrival minutes.
     * @param changes the number of changes.
     * @return the matching tuple.
     * @throws NoSuchElementException if no matching tuple is found.
     */
    public long get(int arrMins, int changes) {
        for (long tuple : tupleFront) {
            if (arrMins(tuple) == arrMins && changes(tuple) == changes) {
                return tuple;
            }
        }
        throw new NoSuchElementException("No tuple with arrMins=" + arrMins + " and changes=" + changes);
    }

    /**
     * Applies the given action to each tuple in the frontier.
     *
     * @param action the action to apply.
     */
    public void forEach(LongConsumer action) {
        for (long value : tupleFront) {
            action.accept(value);
        }
    }

    /**
     * Builder class for constructing a ParetoFront.
     */
    public static class Builder {
        private static final int INITIAL_CAPACITY = 2;
        private long[] frontier;
        private int size;

        /**
         * Constructs an empty Builder.
         */
        public Builder() {
            this.frontier = new long[INITIAL_CAPACITY];
            this.size = 0;
        }

        /**
         * Constructs a Builder by copying another Builder.
         *
         * @param that the Builder to copy.
         */
        public Builder(Builder that) {
            this.frontier = Arrays.copyOf(that.frontier, that.frontier.length);
            this.size = that.size;
        }

        /**
         * Checks if the current frontier is empty.
         *
         * @return true if empty, false otherwise.
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Applies the given action to each tuple in the current frontier.
         *
         * @param action the action to apply.
         */
        public void forEach(LongConsumer action) {
            for (int i = 0; i < size; i++) {
                action.accept(frontier[i]);
            }
        }

        /**
         * Clears the current frontier.
         *
         * @return this Builder.
         */
        public Builder clear() {
            size = 0;
            return this;
        }

        /**
         * Adds a new tuple to the frontier if it is not dominated by an existing tuple. Also
         * removes any tuples that are dominated by the new tuple.
         *
         * @param newTuple the tuple to add.
         * @return this Builder.
         */
        public Builder add(long newTuple) {
            long newOrder = orderValue(newTuple);
            int insertionPos = 0;

            // 1) Find insertion point; reject if same (arrMins,changes) or dominated
            while (insertionPos < size) {
                long current = frontier[insertionPos];
                long currentOrder = orderValue(current);

                if (currentOrder >= newOrder) {
                    break;
                }

                if (arrMins(current) == arrMins(newTuple)
                        && changes(current) == changes(newTuple)) {
                    return this;
                }

                if (dominatesOrIsEqual(current, newTuple)) {
                    return this;
                }
                insertionPos++;
            }

            // 2) Remove any tuples dominated by newTuple; also reject duplicates in tail
            int newSize = insertionPos;
            for (int j = insertionPos; j < size; j++) {
                long current = frontier[j];

                if (arrMins(current) == arrMins(newTuple)
                        && changes(current) == changes(newTuple)) {
                    return this;
                }

                if (!dominatesOrIsEqual(newTuple, current)) {
                    frontier[newSize++] = current;
                }
            }

            // 3) Ensure capacity
            if (newSize + 1 > frontier.length) {
                frontier = Arrays.copyOf(frontier, (int) (frontier.length * 1.5));
            }

            // 4) Shift tail to make room
            int tailLength = newSize - insertionPos;
            if (tailLength > 0) {
                System.arraycopy(frontier, insertionPos,
                        frontier, insertionPos + 1,
                        tailLength);
            }

            // 5) Insert and update size
            frontier[insertionPos] = newTuple;
            size = newSize + 1;
            return this;
        }


        /**
         * Adds a new tuple to the frontier using individual criteria values.
         *
         * @param arrMins the arrival minutes.
         * @param changes the number of changes.
         * @param payload the payload.
         * @return this Builder.
         */
        public Builder add(int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }

        /**
         * Adds all tuples from another Builder's frontier.
         *
         * @param that the other Builder.
         * @return this Builder.
         */
        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.frontier[i]);
            }
            return this;
        }

        private long orderValue(long tuple) {
            return tuple & 0xFFFFFFFF00000000L;
        }

        /**
         * Determines whether this frontier fully dominates the frontier of another Builder, given a
         * departure time.
         *
         * @param that    the other Builder.
         * @param depMins the departure minutes to set.
         * @return true if this frontier fully dominates the other, false otherwise.
         */
        public boolean fullyDominates(Builder that, int depMins) {
            int j = 0;
            for (int i = 0; i < that.size; i++) {
                long tupleWithDep = PackedCriteria.withDepMins(that.frontier[i], depMins);
                while (j < this.size && !dominatesOrIsEqual(this.frontier[j],
                        tupleWithDep)) {
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
         * @return a new ParetoFront containing a copy of the frontier.
         */
        public ParetoFront build() {
            return new ParetoFront(Arrays.copyOf(frontier, size));
        }
    }
}
