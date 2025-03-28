package ch.epfl.rechor.timetable;

import java.time.LocalDate;

/**
 * Interface provides methods to access and manage various components of a public transport timetable
 *
 * @author Amine AMIRA (393410)
 * @author Malak Berrada (379791)
 */
public interface TimeTable {

    /**
     * Returns all indexed stations in this timetable.
     *
     * @return an instance of {@link Stations} containing all the stations.
     */
    Stations stations ();

    /**
     * Returns the alternate names (aliases) of the stations in this timetable.
     *
     * @return station's aliases.
     */
    StationAliases stationAliases ();

    /**
     * Returns all indexed platforms or tracks in this timetable.
     *
     * @return an instance of {@link StationAliases} containing stations' aliases.
     */
    Platforms platforms ();

    /**
     * Returns all indexed public transport routes in this timetable.
     *
     * @return an instance of {@link Routes} containing all the routes.
     */
    Routes routes ();

    /**
     * Returns all indexed transfers in this timetable.
     *
     * @return an instance of {@link Transfers} containing all the transfers.
     */
    Transfers transfers ();

    /**
     * Retrieves all trips that are valid on the specified date.
     *
     * @param date the date for which the trips should be retrieved.
     * @return an instance of {@link Trips} containing the trips valid on the given date.
     */
    Trips tripsFor (LocalDate date);

    /**
     * Retrieves all connections that are valid on the specified date.
     *
     * @param date the date for which the connections should be retrieved.
     * @return an instance of {@link Connections} containing the connections valid on the given date.
     */
    Connections connectionsFor (LocalDate date);

    /**
     * Determines whether the given stop ID corresponds to a station.
     *
     * @param stopId the ID of the stop to test
     * @return {@code true} if {@code stopId} is a station ID; {@code false} otherwise
     */
    default boolean isStationId (int stopId) {
        return stopId >= 0 && stopId < stations().size();
    }

    /**
     * Determines whether the given stop ID corresponds to a platform or track rather than a station.
     *
     * @param stopId the ID of the stop to test
     * @return {@code true} if {@code stopId} is a platform/track ID; {@code false} otherwise
     */
    default boolean isPlatformId (int stopId) {
        int stationCount = stations().size();
        return stopId >= stationCount && stopId < stationCount + platforms().size();
    }

    /**
     * Returns the station ID corresponding to the given stop ID.
     *
     * @param stopId the ID of the stop
     * @return the station ID associated with the stop
     */
    default int stationId (int stopId) {
        return isStationId(stopId) ? stopId : platforms().stationId(stopId - stations().size());
    }

    /**
     * Returns the name of the platform corresponding to the given stop ID
     *
     * @param stopId the ID of the stop
     * @return the name of the platform, or {@code null} if this stop is a station
     */
    default String platformName (int stopId) {
        return isPlatformId(stopId) ? platforms().name(stopId - stations().size()) : null;
    }
}
