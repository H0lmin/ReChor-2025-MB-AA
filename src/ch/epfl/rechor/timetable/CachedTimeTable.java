package ch.epfl.rechor.timetable;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * A {@link TimeTable} implementation that caches the results of
 * {@link #tripsFor(LocalDate)} and {@link #connectionsFor(LocalDate)}
 * to avoid repeated expensive lookups on the underlying timetable.
 * <p>
 * All other methods delegate directly to the wrapped {@code TimeTable}.
 * </p>
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public class CachedTimeTable implements TimeTable {
    private final TimeTable underlying;

    private final Map<LocalDate, Trips> cachedTrips = new HashMap<>();
    private final Map<LocalDate, Connections> cachedConnections = new HashMap<>();

    /**
     * Constructs a new CachedTimeTable that wraps the given underlying timetable.
     *
     * @param timeTable the underlying {@link TimeTable} to delegate to
     * @throws NullPointerException if {@code timeTable} is {@code null}
     */
    public CachedTimeTable(TimeTable timeTable) {
        this.underlying = timeTable;
    }

    /**
     * <p>
     * Delegates directly to the underlying timetable.
     * </p>
     *
     * @return the {@link Stations} from the underlying timetable
     */
    @Override
    public Stations stations() {
        return underlying.stations();
    }

    /**
     * <p>
     * Delegates directly to the underlying timetable.
     * </p>
     *
     * @return the {@link StationAliases} from the underlying timetable
     */
    @Override
    public StationAliases stationAliases() {
        return underlying.stationAliases();
    }

    /**
     * <p>
     * Delegates directly to the underlying timetable.
     * </p>
     *
     * @return the {@link Platforms} from the underlying timetable
     */
    @Override
    public Platforms platforms() {
        return underlying.platforms();
    }

    /**
     * <p>
     * Delegates directly to the underlying timetable.
     * </p>
     *
     * @return the {@link Routes} from the underlying timetable
     */
    @Override
    public Routes routes() {
        return underlying.routes();
    }

    /**
     * <p>
     * Delegates directly to the underlying timetable.
     * </p>
     *
     * @return the {@link Transfers} from the underlying timetable
     */
    @Override
    public Transfers transfers() {
        return underlying.transfers();
    }

    /**
     * Returns the {@link Trips} for the given date, caching the result
     * so that subsequent calls with the same {@code date} return the cached
     * value instead of querying the underlying timetable again.
     *
     * @param date the date for which to retrieve trips
     * @return the {@link Trips} for {@code date}
     * @throws NullPointerException if {@code date} is {@code null}
     */
    @Override
    public Trips tripsFor(LocalDate date) {
        if (!cachedTrips.containsKey(date)) {
            Trips trips = underlying.tripsFor(date);
            cachedTrips.put(date, trips);
        }
        return cachedTrips.get(date);
    }

    /**
     * Returns the {@link Connections} for the given date, caching the result
     * so that subsequent calls with the same {@code date} return the cached
     * value instead of querying the underlying timetable again.
     *
     * @param date the date for which to retrieve connections
     * @return the {@link Connections} for {@code date}
     * @throws NullPointerException if {@code date} is {@code null}
     */
    @Override
    public Connections connectionsFor(LocalDate date) {
        if (!cachedConnections.containsKey(date)) {
            Connections connections = underlying.connectionsFor(date);
            cachedConnections.put(date, connections);
        }
        return cachedConnections.get(date);
    }
}