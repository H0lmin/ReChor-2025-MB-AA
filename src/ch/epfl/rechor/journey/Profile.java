package ch.epfl.rechor.journey;

import ch.epfl.rechor.timetable.Connections;
import ch.epfl.rechor.timetable.TimeTable;
import ch.epfl.rechor.timetable.Trips;

import java.time.LocalDate;
import java.util.List;

public record Profile(TimeTable timeTable, LocalDate date, int arrStationId, List<ParetoFront> stationFront) {
    public Profile {
        stationFront = List.copyOf(stationFront);
    }

    public Connections connections() {
        return timeTable.connectionsFor(date);
    }

    public Trips trips() {
        return timeTable.tripsFor(date);
    }

    public ParetoFront forStation(int stationId) {
        if (stationId < 0 || stationId >= stationFront.size()) {
            throw new IndexOutOfBoundsException("Invalid station id: " + stationId);
        }
        return stationFront.get(stationId);
    }

    public final static class Builder {
        private final TimeTable timeTable;
        private final LocalDate date;
        private final int arrStationId;
        private final ParetoFront.Builder[] stationBuilders;
        private final ParetoFront.Builder[] tripBuilders;

        public Builder(TimeTable timeTable, LocalDate date, int arrStationId) {
            this.timeTable = timeTable;
            this.date = date;
            this.arrStationId = arrStationId;

            int stationCount = timeTable.stations().size();
            int tripCount = timeTable.tripsFor(date).size();
            this.stationBuilders = new ParetoFront.Builder[stationCount];
            this.tripBuilders = new ParetoFront.Builder[tripCount];
        }

        public ParetoFront.Builder forStation(int stationId) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid stationId: " + stationId);
            }
            return stationBuilders[stationId];
        }

        public void setForStation(int stationId, ParetoFront.Builder builder) {
            if (stationId < 0 || stationId >= stationBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid stationId: " + stationId);
            }
            stationBuilders[stationId] = builder;
        }

        public ParetoFront.Builder forTrip(int tripId) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid tripId: " + tripId);
            }
            return tripBuilders[tripId];
        }

        public void setForTrip(int tripId, ParetoFront.Builder builder) {
            if (tripId < 0 || tripId >= tripBuilders.length) {
                throw new IndexOutOfBoundsException("Invalid tripId: " + tripId);
            }
            tripBuilders[tripId] = builder;
        }

        public Profile build() {
            ParetoFront[] finalFronts = new ParetoFront[stationBuilders.length];
            for (int i = 0; i < stationBuilders.length; i++) {
                finalFronts[i] = (stationBuilders[i] == null)
                        ? ParetoFront.EMPTY
                        : stationBuilders[i].build();
            }
            return new Profile(timeTable, date, arrStationId, List.of(finalFronts));
        }
    }
}
