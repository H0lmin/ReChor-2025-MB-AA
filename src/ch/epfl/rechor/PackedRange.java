package ch.epfl.rechor;

import static ch.epfl.rechor.Preconditions.checkArgument;

public class PackedRange {
    private PackedRange(){
    }

    public static int pack (int startInclusive, int endExclusive){
        checkArgument((startInclusive >>> 24) == 0 && (endExclusive - startInclusive) >>> 8 == 0);
        return Bits32_24_8.pack(startInclusive, endExclusive - startInclusive);
    }

    public static int length(int interval){
        return Bits32_24_8.unpack8(interval);
    }

    public static int startInclusive(int interval){
        return Bits32_24_8.unpack24(interval);
    }

    public static int endExclusive (int interval){
        return startInclusive(interval) + length(interval);
    }
}
