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
     * @return all indexed transfers in this timetable.
     */
    Transfers transfers();

    /**
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
     * Determines whether the given stop ID corresponds to a station
     * @param stopId the ID of the stop to test
     */
    default boolean isStationId(int stopId) {
        return stopId >= 0 && stopId < stations().size();
    }

    /**
     * @param stopId the ID of the stop to test
     */
    default boolean isPlatformId(int stopId) {
        return stopId >= 0 && stopId >= stations().size() && stopId < platforms().size();
    }

    /**
     * Returns the station ID corresponding to the stop ID
     * @param stopId the ID of the stop
     */
    default int stationId(int stopId) {
        if (isStationId(stopId)) {
            return stopId;
        } else {
            return platforms().stationId(stopId);
        }
    }

    /**
     * Returns the name of the platform corresponding to the stop ID
     * @param stopId the ID of the stop
     */
    default String platformName(int stopId) {
        if (isPlatformId(stopId)) {
            return platforms().name(stopId);
        } else {
            return null;
        }
    }
}
