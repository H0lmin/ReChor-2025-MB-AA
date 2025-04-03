package ch.epfl.rechor.timetable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CachedTimeTable implements TimeTable {
    private final TimeTable underlying;

    private final Map<LocalDate, Trips> cachedTrips = new HashMap<>();
    private final Map<LocalDate, Connections> cachedConnections = new HashMap<>();

    public CachedTimeTable(TimeTable timeTable) {
        this.underlying = timeTable;
    }

    @Override
    public Stations stations() {
        return underlying.stations();
    }

    @Override
    public StationAliases stationAliases() {
        return underlying.stationAliases();
    }

    @Override
    public Platforms platforms() {
        return underlying.platforms();
    }

    @Override
    public Routes routes() {
        return underlying.routes();
    }

    @Override
    public Transfers transfers() {
        return underlying.transfers();
    }

    @Override
    public Trips tripsFor(LocalDate date) {
        if (!cachedTrips.containsKey(date)) {
            Trips trips = underlying.tripsFor(date);
            cachedTrips.put(date, trips);
        }
        return cachedTrips.get(date);
    }

    @Override
    public Connections connectionsFor(LocalDate date) {
        if (!cachedConnections.containsKey(date)) {
            Connections connections = underlying.connectionsFor(date);
            cachedConnections.put(date, connections);
        }
        return cachedConnections.get(date);
    }
}