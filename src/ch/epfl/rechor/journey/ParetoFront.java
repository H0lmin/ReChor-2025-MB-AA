package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;


public final class ParetoFront {
    /**
     * A constant representing an empty Pareto frontier.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    private final long[] tupleFront;

    private ParetoFront(long[] tupleFront) {
        this.tupleFront = tupleFront.clone();
    }

    public int size() {
        return tupleFront.length;
    }

    public long get(int arrMins, int changes) {
        long searchOrder = (PackedCriteria.pack(arrMins, changes, 0)) & 0xFFFFFFFF00000000L;
        for (long tuple : tupleFront) {
            if ((tuple & 0xFFFFFFFF00000000L) == searchOrder) {
                return tuple;
            }
        }
        throw new NoSuchElementException("No tuple with arrMins=" + arrMins + " and changes=" + changes);
    }


    public void forEach(LongConsumer action) {
        for (long value : tupleFront) {
            action.accept(value);
        }
    }

    /**
     * Returns a string representation of this ParetoFront.
     *
     * @return a string representation of the ParetoFront containing the packed tuples.
     */
    @Override
    public String toString() {
        return "ParetoFront{" +
                "tupleFront=" + Arrays.toString(tupleFront) +
                '}';
    }

    public static class Builder {
        private static final int INITIAL_CAPACITY = 2;
        private long[] frontier;
        private int size;

        public Builder() {
            this.frontier = new long[INITIAL_CAPACITY];
            this.size = 0;
        }

        public Builder(Builder that) {
            this.frontier = Arrays.copyOf(that.frontier, that.frontier.length);
            this.size = that.size;
        }

        /**
         * Returns true if the current frontier is empty.
         */
        public boolean isEmpty() {
            return size == 0;
        }

        /**
         * Applies the given action to each tuple in the frontier.
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

        public Builder add(long newTuple) {
            // Compare only the criteria part (i.e., ignore the payload).
            long newOrder = orderValue(newTuple);

            // First, if any tuple already present (ignoring payload) dominates or equals newTuple,
            // then do not add newTuple.
            for (int i = 0; i < size; i++) {
                if (orderValue(frontier[i]) == newOrder ||
                        PackedCriteria.dominatesOrIsEqual(frontier[i], newTuple)) {
                    return this;
                }
            }

            // Find the insertion position: first index where the order value is not less than newOrder.
            int pos = 0;
            while (pos < size && orderValue(frontier[pos]) < newOrder) {
                pos++;
            }

            // Now, count how many consecutive tuples starting at pos are dominated by newTuple.
            int dominatedCount = 0;
            int i = pos;
            while (i < size && PackedCriteria.dominatesOrIsEqual(newTuple, frontier[i])) {
                dominatedCount++;
                i++;
            }

            // Resize the array if necessary.
            if (size + 1 - dominatedCount > frontier.length) {
                resize(size + 1 - dominatedCount);
            }

            // Shift the remaining (non-dominated) tuples to the right to fill the gap.
            System.arraycopy(frontier, pos + dominatedCount, frontier, pos + 1, size - pos - dominatedCount);
            frontier[pos] = newTuple;
            size = size + 1 - dominatedCount;
            return this;
        }

        public Builder add(int arrMins, int changes, int payload) {
            long newTuple = PackedCriteria.pack(arrMins, changes, payload);
            return add(newTuple);
        }


        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.frontier[i]);
            }
            return this;
        }

        private void resize(int requiredCapacity) {
            int newCapacity = Math.max((int) (frontier.length * 1.5), requiredCapacity);
            frontier = Arrays.copyOf(frontier, newCapacity);
        }

        private long orderValue(long tuple) {
            return tuple & 0xFFFFFFFF00000000L; // keep only the upper 32 bits
        }


        public boolean fullyDominates(Builder that, int depMins) {
            int j = 0; // pointer for traversing 'this' frontier
            for (int i = 0; i < that.size; i++) {
                // Repack the tuple from 'that' with the provided departure time.
                long tupleWithDep = PackedCriteria.withDepMins(that.frontier[i], depMins);
                // Advance in 'this' frontier until we find one that dominates or equals the repacked tuple.
                while (j < this.size && !PackedCriteria.dominatesOrIsEqual(this.frontier[j], tupleWithDep)) {
                    j++;
                }
                // If we reached the end of 'this' frontier, then no tuple dominated this one.
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

        @Override
        public String toString() {
            return Arrays.toString(Arrays.copyOf(frontier, size));
        }
    }

}
