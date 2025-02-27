package ch.epfl.rechor.journey;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static ch.epfl.rechor.PackedRange.pack;


public final class ParetoFront {
    /**
     * A constant representing an empty Pareto frontier.
     */
    public static final ParetoFront EMPTY = new ParetoFront(new long[0]);

    private final long [] tupleFront;

    private ParetoFront (long [] tupleFront){
        this.tupleFront = tupleFront.clone();
    }

    public int size() {
        return tupleFront.length;
    }

    public long get(int arrMins, int changes){

        long searchValue = pack(arrMins, changes);

        for (long tuple : tupleFront) {
            if (tuple == searchValue) {
                return tuple;
            }
        }
        throw new NoSuchElementException("No tuple with arrMins=" + arrMins + " and changes=" + changes);
    }

    public void forEach(LongConsumer action){
        for (long value : tupleFront) {
            action.accept(value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ParetoFront[");
        for (int i = 0; i < size(); i++) {
            long first = get(i, 0);
            long second = get(i, 1);

            sb.append("(").append(first).append(", ").append(second).append(")");

            if (i < size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
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
        public boolean isEmpty() {
            return size == 0;
        }
        public void forEach(LongConsumer action){
            for (int i = 0; i < size; i++) {
                action.accept(frontier[i]);
            }
        }
        public Builder clear() {
            size = 0;
            return this;
        }

        public Builder add(long packedTuple) {
            int pos = 0;
            while (pos < size && frontier[pos] < packedTuple) {
                pos++;
            }

            if (pos < size && frontier[pos] == packedTuple) {
                return this; // Déjà présent
            }

            int dominatedCount = 0;
            for (int i = pos; i < size; i++) {
                if (dominates(packedTuple, frontier[i])) {
                    dominatedCount++;
                } else {
                    break;
                }
            }

            if (size + 1 - dominatedCount > frontier.length) {
                resize();
            }

            System.arraycopy(frontier, pos + dominatedCount, frontier, pos + 1, size - pos - dominatedCount);
            frontier[pos] = packedTuple;
            size = size + 1 - dominatedCount;
            return this;
        }

        private void resize() {
            frontier = Arrays.copyOf(frontier, (int) (frontier.length * 1.5));
        }
        private boolean dominates(long a, long b) {

            int arrA = (int) (a >>> 32);
            int arrB = (int) (b >>> 32);
            int changesA = (int) a;
            int changesB = (int) b;

            return (arrA < arrB || (arrA == arrB && changesA < changesB));
        }

        public boolean fullyDominates(Builder that, int depMins) {
            int j = 0; // Index pour parcourir `this`
            for (int i = 0; i < that.size; i++) {
                long tuple = that.frontier[i];

                int changes = (int) tuple;

                tuple = pack(depMins, changes);

                while (j < this.size && !dominates(this.frontier[j], tuple)) {
                    j++;
                }

                if (j == this.size) {
                    return false; // Aucun tuple de `this` ne domine celui-ci
                }
            }
            return true;
        }

        public ParetoFront build() {
            return new ParetoFront(Arrays.copyOf(frontier, size));
        }

        @Override
        public String toString() {
            return Arrays.toString(Arrays.copyOf(frontier, size));
        }
    }
}
