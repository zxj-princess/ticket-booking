package com.ticket.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.entity.Train;

import java.util.List;

public interface TrainService {

    List<Train> queryTrains(Long fromStationId, Long toStationId, String date);

    Train getTrainById(Long id);

    Page<Train> listTrains(int page, int size);

    void addTrain(Train train);

    void updateTrain(Train train);

    void deleteTrain(Long id);

    void importTrains(List<Train> trains);
}
