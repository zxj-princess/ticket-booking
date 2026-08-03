package com.ticket.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ticket.common.result.R;
import com.ticket.entity.Station;
import com.ticket.entity.Train;
import com.ticket.mapper.TrainMapper;
import com.ticket.service.StationService;
import com.ticket.service.TrainService;
import com.ticket.vo.TrainVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/train")
public class TrainController {

    @Autowired
    private TrainService trainService;

    @Autowired
    private StationService stationService;

    @Autowired
    private TrainMapper trainMapper;

    /**
     * 车次查询：支持城市级查询
     * 用户输入"北京"时，匹配该城市下所有站点（北京南、北京西、北京），合并查询结果
     */
    @GetMapping("/search")
    public R<List<TrainVO>> searchTrains(@RequestParam String fromStation,
                                          @RequestParam String toStation,
                                          @RequestParam String date) {
        // 查询出发城市和到达城市的所有站点
        List<Station> fromStations = stationService.searchStations(fromStation);
        List<Station> toStations = stationService.searchStations(toStation);

        if (fromStations.isEmpty() || toStations.isEmpty()) {
            return R.success(List.of());
        }

        List<Long> fromIds = fromStations.stream().map(Station::getId).toList();
        List<Long> toIds = toStations.stream().map(Station::getId).toList();

        // 解析日期对应的星期
        int dayOfWeek = 1;
        try {
            LocalDate localDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            dayOfWeek = localDate.getDayOfWeek().getValue();
        } catch (Exception e) {
            dayOfWeek = 1;
        }

        // 跨站点查询车次
        List<Train> trains = trainMapper.selectByStationIdsAndDate(fromIds, toIds, dayOfWeek);

        // 站点ID→名称映射
        java.util.Map<Long, Station> stationMap = new java.util.HashMap<>();
        for (Station s : fromStations) stationMap.put(s.getId(), s);
        for (Station s : toStations) stationMap.put(s.getId(), s);

        List<TrainVO> voList = trains.stream()
                .map(t -> convertToVO(t, stationMap))
                .collect(Collectors.toList());
        return R.success(voList);
    }

    @GetMapping("/{id}")
    public R<TrainVO> getTrain(@PathVariable Long id) {
        Train train = trainService.getTrainById(id);
        Station from = stationService.getStationById(train.getFromStationId());
        Station to = stationService.getStationById(train.getToStationId());
        java.util.Map<Long, Station> map = new java.util.HashMap<>();
        if (from != null) map.put(from.getId(), from);
        if (to != null) map.put(to.getId(), to);
        return R.success(convertToVO(train, map));
    }

    @SuppressWarnings("unchecked")
    @GetMapping("/list")
    public R<Object> listTrains(@RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "10") int size) {
        Page<Train> trainPage = trainService.listTrains(page, size);
        // 加载所有站点用于名称映射
        List<Station> allStations = stationService.listStations();
        java.util.Map<Long, Station> stationMap = allStations.stream()
                .collect(Collectors.toMap(Station::getId, s -> s, (a, b) -> a));
        IPage<TrainVO> voPage = trainPage.convert(t -> convertToVO(t, stationMap));
        return R.success((Object) voPage);
    }

    @PostMapping("/add")
    public R<Void> addTrain(@RequestBody Train train) {
        trainService.addTrain(train);
        return R.success();
    }

    @PutMapping("/update")
    public R<Void> updateTrain(@RequestBody Train train) {
        trainService.updateTrain(train);
        return R.success();
    }

    @DeleteMapping("/{id}")
    public R<Void> deleteTrain(@PathVariable Long id) {
        trainService.deleteTrain(id);
        return R.success();
    }

    /**
     * 将 Train 转换为 TrainVO，填充站点名、车型字符串、历时文案等前端友好字段
     */
    private TrainVO convertToVO(Train train, java.util.Map<Long, Station> stationMap) {
        TrainVO vo = new TrainVO();
        BeanUtils.copyProperties(train, vo);

        // 车次号别名
        vo.setTrainNo(train.getTrainNumber());

        // 车型字符串
        vo.setTrainTypeStr(trainTypeStr(train.getTrainType()));

        // 站点名
        Station from = stationMap.get(train.getFromStationId());
        Station to = stationMap.get(train.getToStationId());
        if (from != null) {
            vo.setFromStationName(from.getName());
            vo.setFromStation(from.getName());
        }
        if (to != null) {
            vo.setToStationName(to.getName());
            vo.setToStation(to.getName());
        }

        // 时间格式化为 HH:mm
        vo.setDepartTime(formatTime(train.getDepartTime()));
        vo.setArriveTime(formatTime(train.getArriveTime()));

        // 历时文案
        vo.setDurationText(formatDuration(train.getDuration()));

        return vo;
    }

    private String trainTypeStr(Integer type) {
        if (type == null) return "G";
        switch (type) {
            case 0: return "G";
            case 1: return "D";
            case 2: return "K";
            case 3: return "Z";
            case 4: return "T";
            default: return "G";
        }
    }

    private String formatTime(String time) {
        if (time == null) return "";
        // 数据库 TIME 类型返回 HH:mm:ss，截取 HH:mm
        if (time.length() >= 5) return time.substring(0, 5);
        return time;
    }

    private String formatDuration(Integer minutes) {
        if (minutes == null) return "";
        int h = minutes / 60;
        int m = minutes % 60;
        if (h > 0 && m > 0) return h + "小时" + m + "分";
        if (h > 0) return h + "小时";
        return m + "分钟";
    }
}
