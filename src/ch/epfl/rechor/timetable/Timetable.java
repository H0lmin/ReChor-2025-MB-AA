package ch.epfl.rechor.timetable;

import java.time.LocalDate;

public interface TimeTable {

    /**
     * @return all indexed stations in this timetable.
     */
    Stations stations();

    /**
     * @return the alternate names of stations in this timetable.
     */
    StationAliases stationAliases();

    /**
     * @return all indexed platforms or tracks in this timetable.
     */
    Platforms platforms();

    /**
     * @return all indexed public transport routes in this timetable.
     */
    Routes routes();

    /**
     *
     * @return all indexed transfers (walking connections) in this timetable.
     */
    Transfers transfers();

    /**
     *
     * @param date the date for which trips should be retrieved
     * @return all indexed trips valid on the given date.
     */
    Trips tripsFor(LocalDate date);

    /**
     * @param date the date for which connections should be retrieved
     * @return all indexed connections valid on the given date.
     */
    Connections connectionsFor(LocalDate date);

    /**
     * Determines whether the given stop ID corresponds to a station.
     * <p>
     * The default implementation checks if {@code stopId} is in the range [0, stations().size()).
     *
     * @param stopId the ID of the stop to test
     * @return {@code true} if {@code stopId} is a station ID; {@code false} otherwise
     */
    default boolean isStationId(int stopId) {
        return stopId >= 0 && stopId < stations().size();
    }

    /**
     * Determines whether the given stop ID corresponds to a platform or track rather than a station.
     * <p>
     * The default implementation checks if {@code stopId} is in the range
     * [stations().size(), platforms().size()). In other words, if {@code stopId} is
     * greater than or equal to the number of stations but less than the number of platforms.
     *
     * @param stopId the ID of the stop to test
     * @return {@code true} if {@code stopId} is a platform/track ID; {@code false} otherwise
     */
    default boolean isPlatformId(int stopId) {
        return stopId >= 0 && stopId >= stations().size() && stopId < (stations().size() + platforms().size());
    }

    /**
     * Returns the station ID corresponding to the given stop ID.
     * <p>
     * If the stop is already a station (i.e., {@link #isStationId(int)} is {@code true}),
     * then this method simply returns {@code stopId}. Otherwise, it retrieves the station
     * ID associated with the platform via {@code platforms().stationId(stopId)}.
     *
     * @param stopId the ID of the stop
     * @return the station ID associated with the stop
     */
    default int stationId(int stopId) {
        return isStationId(stopId) ? stopId : platforms().stationId(stopId - stations().size());
    }

    /**
     * Returns the name of the platform corresponding to the given stop ID, or
     * {@code null} if the stop is not a platform (i.e., if it is a station).
     *
     * @param stopId the ID of the stop
     * @return the name of the platform, or {@code null} if this stop is a station
     */
    default String platformName(int stopId) {
        return isPlatformId(stopId) ? platforms().name(stopId - stations().size()) : null;
    }
}
