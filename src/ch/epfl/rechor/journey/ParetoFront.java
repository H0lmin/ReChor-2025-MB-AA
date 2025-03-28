package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static java.lang.System.arraycopy;


/**
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public final class ParetoFront {

    /**
     * A constant representing an empty Pareto frontier.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    private final long[] tupleFront;

    private ParetoFront (long[] tupleFront) {
        this.tupleFront = tupleFront.clone();
    }

    public int size () {
        return tupleFront.length;
    }

    public long get (int arrMins, int changes) {
        for (long tuple : tupleFront) {
            if (PackedCriteria.arrMins(tuple) == arrMins && PackedCriteria.changes(tuple) == changes) {
                return tuple;
            }
        }
        throw new NoSuchElementException("No tuple with arrMins=" + arrMins + " and changes=" + changes);
    }


    public void forEach (LongConsumer action) {
        for (long value : tupleFront) {
            action.accept(value);
        }
    }

    public static class Builder {
        private static final int INITIAL_CAPACITY = 2;
        private long[] frontier;
        private int size;

        public Builder () {
            this.frontier = new long[INITIAL_CAPACITY];
            this.size = 0;
        }

        public Builder (Builder that) {
            this.frontier = Arrays.copyOf(that.frontier, that.frontier.length);
            this.size = that.size;
        }

        /**
         * Returns true if the current frontier is empty.
         */
        public boolean isEmpty () {
            return size == 0;
        }

        /**
         * Applies the given action to each tuple in the frontier.
         */
        public void forEach (LongConsumer action) {
            for (int i = 0; i < size; i++) {
                action.accept(frontier[i]);
            }
        }

        /**
         * Clears the current frontier.
         *
         * @return this Builder.
         */
        public Builder clear () {
            size = 0;
            return this;
        }

        public Builder add (long newTuple) {
            long newOrder = orderValue(newTuple);

            int insertionPos = 0;
            while (insertionPos < size && orderValue(frontier[insertionPos]) < newOrder) {
                if (PackedCriteria.dominatesOrIsEqual(frontier[insertionPos], newTuple)) {
                    return this;
                }
                insertionPos++;
            }

            int newSize = insertionPos;
            for (int j = insertionPos; j < size; j++) {
                if (!PackedCriteria.dominatesOrIsEqual(newTuple, frontier[j])) {
                    frontier[newSize++] = frontier[j];
                }
            }

            if (newSize + 1 > frontier.length) {
                frontier = Arrays.copyOf(frontier, (int) (frontier.length * 1.5));
            }

            int tailLength = newSize - insertionPos;
            if (tailLength > 0) {
                arraycopy(frontier, insertionPos, frontier, insertionPos + 1, tailLength);
            }
            frontier[insertionPos] = newTuple;
            size = newSize + 1;
            return this;
        }


        public Builder add (int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }


        public Builder addAll (Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.frontier[i]);
            }
            return this;
        }

        private long orderValue (long tuple) {
            return tuple & 0xFFFFFFFF00000000L;
        }


        public boolean fullyDominates (Builder that, int depMins) {
            int j = 0;
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
         * @return a new ParetoFront containing a copy of the frontier.
         */
        public ParetoFront build () {
            return new ParetoFront(Arrays.copyOf(frontier, size));
        }
    }

}
