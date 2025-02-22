package ch.epfl.rechor.journey;

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

}
