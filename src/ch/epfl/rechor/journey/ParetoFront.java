package ch.epfl.rechor.journey;

import ch.epfl.rechor.FormatterFr;

import java.time.Duration;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static java.lang.System.arraycopy;


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
        for (long tuple : tupleFront) {
            if (PackedCriteria.arrMins(tuple) == arrMins && PackedCriteria.changes(tuple) == changes) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParetoFront{");
        for (long tuple : tupleFront) {
            if (PackedCriteria.hasDepMins(tuple)){
                sb.append("Heure de départ : ")
                        .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.depMins(tuple))))
                        .append(" ; ");
            }
            sb.append("Heure d'arrivée: ")
                    .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.arrMins(tuple))))
                    .append(" ; ");
            sb.append("Nombre de changements: ")
                    .append(PackedCriteria.changes(tuple))
                    .append(" ; \n");
        }
        sb.append("}");
        return sb.toString();
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
            // Compute the "order value" (upper 32 bits) of the new tuple.
            long newOrder = orderValue(newTuple);

            // Step 1: Find the insertion position (first index where the order value is not less than newOrder).
            int pos = 0;
            while (pos < size && orderValue(frontier[pos]) < newOrder) {
                pos++;
            }

            // Step 2: Check only the tuples BEFORE the insertion position.
            for (int i = 0; i < pos; i++) {
                if (PackedCriteria.dominatesOrIsEqual(frontier[i], newTuple)) {
                    return this;
                }
            }

            int tempPos = pos;
            int tempSize = size;
            for (int j = pos; j < tempSize; j++){
                if(!PackedCriteria.dominatesOrIsEqual(newTuple, frontier[j])){
                    frontier[tempPos++] = frontier[j];
                } else {
                    size --;
                }
            }

            // Step 4: Resize the array if needed.
            if (size == frontier.length) {
                frontier = Arrays.copyOf(frontier, (int) (frontier.length * 1.5));
            }

            // Shift the remaining (non-dominated) tuples one position to the right.
            arraycopy(frontier, pos, frontier, pos + 1, tempPos - pos);
            frontier[pos] = newTuple;

            size = tempPos + 1;
            return this;
        }


        public Builder add(int arrMins, int changes, int payload) {
            return add(PackedCriteria.pack(arrMins, changes, payload));
        }


        public Builder addAll(Builder that) {
            for (int i = 0; i < that.size; i++) {
                add(that.frontier[i]);
            }
            return this;
        }

        private long orderValue(long tuple) {
            return tuple & 0xFFFFFFFF00000000L; // keep only the bits from 32 to 50
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
            StringBuilder sb = new StringBuilder("ParetoFront{");
            for (int i = 0; i<size ; i++) {
                if (PackedCriteria.hasDepMins(frontier[i])){
                    sb.append("Heure de départ : ")
                            .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.depMins(frontier[i]))))
                            .append(" ; ");
                }
                sb.append("Heure d'arrivée: ")
                        .append(FormatterFr.formatDuration(Duration.ofMinutes(PackedCriteria.arrMins(frontier[i]))))
                        .append(" ; ");
                sb.append("Nombre de changements: ")
                        .append(PackedCriteria.changes(frontier[i]))
                        .append(" ; \n");
            }
            sb.append("}");
            return sb.toString();
        }
    }

}
