package com.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.common.exception.BusinessException;
import com.ticket.entity.Station;
import com.ticket.mapper.StationMapper;
import com.ticket.service.StationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class StationServiceImpl implements StationService {

    @Autowired
    private StationMapper stationMapper;

    @Override
    public Station getStationById(Long id) {
        return stationMapper.selectById(id);
    }

    @Override
    public List<Station> listStations() {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Station::getStatus, 1);
        wrapper.orderByAsc(Station::getName);
        return stationMapper.selectList(wrapper);
    }

    @Override
    public void addStation(Station station) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Station::getCode, station.getCode());
        Long count = stationMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException("站点编码已存在");
        }
        station.setStatus(1);
        stationMapper.insert(station);
    }

    @Override
    public void updateStation(Station station) {
        Station exist = stationMapper.selectById(station.getId());
        if (exist == null) {
            throw new BusinessException("站点不存在");
        }
        stationMapper.updateById(station);
    }

    @Override
    public void deleteStation(Long id) {
        Station station = stationMapper.selectById(id);
        if (station == null) {
            throw new BusinessException("站点不存在");
        }
        stationMapper.deleteById(id);
    }

    @Override
    public List<Station> searchStations(String keyword) {
        LambdaQueryWrapper<Station> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(Station::getName, keyword)
                    .or().like(Station::getCode, keyword)
                    .or().like(Station::getPinyin, keyword)
                    .or().like(Station::getCity, keyword));
        }
        wrapper.eq(Station::getStatus, 1);
        wrapper.orderByAsc(Station::getName);
        return stationMapper.selectList(wrapper);
    }
}
