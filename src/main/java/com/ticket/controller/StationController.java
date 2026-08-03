package com.ticket.controller;

import com.ticket.common.result.R;
import com.ticket.entity.Station;
import com.ticket.service.StationService;
import com.ticket.vo.StationVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/station")
public class StationController {

    @Autowired
    private StationService stationService;

    @GetMapping("/list")
    public R<List<StationVO>> listStations() {
        List<Station> stations = stationService.listStations();
        List<StationVO> voList = stations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return R.success(voList);
    }

    @GetMapping("/search")
    public R<List<StationVO>> searchStations(@RequestParam String keyword) {
        List<Station> stations = stationService.searchStations(keyword);
        List<StationVO> voList = stations.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return R.success(voList);
    }

    @PostMapping("/add")
    public R<Void> addStation(@RequestBody Station station) {
        stationService.addStation(station);
        return R.success();
    }

    @PutMapping("/update")
    public R<Void> updateStation(@RequestBody Station station) {
        stationService.updateStation(station);
        return R.success();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
        return R.success();
    }

    private StationVO convertToVO(Station station) {
        StationVO vo = new StationVO();
        BeanUtils.copyProperties(station, vo);
        return vo;
    }
}
