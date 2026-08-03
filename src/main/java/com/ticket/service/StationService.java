package com.ticket.service;

import com.ticket.entity.Station;

import java.util.List;

public interface StationService {

    Station getStationById(Long id);

    List<Station> listStations();

    void addStation(Station station);

    void updateStation(Station station);

    void deleteStation(Long id);

    List<Station> searchStations(String keyword);
}
