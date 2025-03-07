package ch.epfl.rechor.timetable;

public class MockPlatforms implements Platforms {
    private final String[] names;
    private final int[] stationIds; // platform at index i belongs to station stationIds[i]

    public MockPlatforms(String[] names, int[] stationIds) {
        if (names.length != stationIds.length) {
            throw new IllegalArgumentException("Names and stationIds arrays must have the same length");
        }
        this.names = names.clone();
        this.stationIds = stationIds.clone();
    }

    @Override
    public int size() {
        return names.length;
    }

    @Override
    public String name(int id) {
        if (id < 0 || id >= names.length) {
            throw new IndexOutOfBoundsException("Platform index out of bounds: " + id);
        }
        return names[id];
    }

    @Override
    public int stationId(int id) {
        if (id < 0 || id >= stationIds.length) {
            throw new IndexOutOfBoundsException("Platform index out of bounds: " + id);
        }
        return stationIds[id];
    }
}

