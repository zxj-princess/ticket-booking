package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.Train;
import com.ticket.mapper.TrainMapper;
import com.ticket.service.TrainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.List;

@Service
public class TrainServiceImpl implements TrainService {

    @Autowired
    private TrainMapper trainMapper;
    @Autowired private com.ticket.mapper.OrderMapper orders;

    @Override
    public List<Train> queryTrains(Long fromStationId, Long toStationId, String date) {
        LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        int dayOfWeek = localDate.getDayOfWeek().getValue();
        return trainMapper.selectByStationAndDate(fromStationId, toStationId, dayOfWeek);
    }

    @Override
    public Train getTrainById(Long id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BusinessException("车次不存在");
        }
        return train;
    }

    @Override
    public Page<Train> listTrains(int page, int size) {
        Page<Train> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Train::getCreateTime);
        return trainMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public void addTrain(Train train) {
        LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Train::getTrainNumber, train.getTrainNumber());
        Long count = trainMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("车次号已存在");
        }
        train.setStatus(1);
        trainMapper.insert(train);
    }

    @Override
    public void updateTrain(Train train) {
        Train exist = trainMapper.selectById(train.getId());
        if (exist == null) {
            throw new BusinessException("车次不存在");
        }
        trainMapper.updateById(train);
    }

    @Override
    public void deleteTrain(Long id) {
        Train train = trainMapper.selectById(id);
        if (train == null) {
            throw new BusinessException("车次不存在");
        }
        if(orders.selectCount(new LambdaQueryWrapper<com.ticket.entity.Order>().eq(com.ticket.entity.Order::getTrainId,id).in(com.ticket.entity.Order::getStatus,0,1))>0) throw new BusinessException("该车次存在待支付或待出行订单，不能删除");
        trainMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void importTrains(List<Train> trains) {
        if (trains == null || trains.isEmpty()) {
            throw new BusinessException("导入数据不能为空");
        }
        for (Train train : trains) {
            LambdaQueryWrapper<Train> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Train::getTrainNumber, train.getTrainNumber());
            Train exist = trainMapper.selectOne(wrapper);
            if (exist != null) {
                train.setId(exist.getId());
                trainMapper.updateById(train);
            } else {
                train.setStatus(1);
                trainMapper.insert(train);
            }
        }
    }
}
