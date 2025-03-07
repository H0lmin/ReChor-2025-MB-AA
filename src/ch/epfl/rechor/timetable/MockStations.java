package ch.epfl.rechor.timetable;

public class MockStations implements Stations {
    private final String[] names;

    public MockStations(String... names) {
        this.names = names.clone();
    }

    @Override
    public int size() {
        return names.length;
    }

    @Override
    public String name(int id) {
        if (id < 0 || id >= names.length) {
            throw new IndexOutOfBoundsException("Station index out of bounds: " + id);
        }
        return names[id];
    }

    @Override
    public double longitude(int id) {
        throw new UnsupportedOperationException("Not needed for this test");
    }

    @Override
    public double latitude(int id) {
        throw new UnsupportedOperationException("Not needed for this test");
    }
}

