package com.sky.service.impl;

import com.sky.dto.order.GoodsSalesDTO;
import com.sky.entity.Orders;
import com.sky.exception.BaseException;
import com.sky.feign.UserFeignClient;
import com.sky.mapper.OrderMapper;
import com.sky.service.ReportService;
import com.sky.service.WorkspaceService;
import com.sky.vo.order.BusinessDataVO;
import com.sky.vo.order.OrderReportVO;
import com.sky.vo.order.SalesTop10ReportVO;
import com.sky.vo.order.TurnoverReportVO;
import com.sky.vo.order.UserReportVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private UserFeignClient userFeignClient;
    @Autowired
    private WorkspaceService workspaceService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    public TurnoverReportVO getTurnoverReport(LocalDate begin, LocalDate end) {
        //设置日期字符串
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }
        //获取营业额列表
        List<Double> turnoverList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime dateBegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dateEnd = LocalDateTime.of(date, LocalTime.MAX);

            Map<String,Object> map = new HashMap<>();
            map.put("begin", dateBegin);
            map.put("end", dateEnd);
            map.put("status", Orders.COMPLETED);
            Double turnover = orderMapper.getTurnoverByDates(map);
            if(turnover == null){
                turnover = 0.0;
            }
            turnoverList.add(turnover);
        }

        return TurnoverReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .turnoverList(StringUtils.join(turnoverList, ","))
                .build();

    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    public UserReportVO getUserReport(LocalDate begin, LocalDate end) {
        //设置日期字符串
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        List<Integer> totalUserList = new ArrayList<>();
        List<Integer> newUserList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime dateBegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dateEnd = LocalDateTime.of(date, LocalTime.MAX);

            // 通过 Feign 调用 user-service 统计用户
            Integer totalUser = userFeignClient.countUserByDates(null, dateEnd).getData();
            Integer newUser = userFeignClient.countUserByDates(dateBegin, dateEnd).getData();
            totalUserList.add(totalUser);
            newUserList.add(newUser);
        }

        return UserReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .totalUserList(StringUtils.join(totalUserList, ","))
                .newUserList(StringUtils.join(newUserList, ","))
                .build();
    }

    /**
     * 订单数据统计
     * @param begin
     * @param end
     * @return
     */
    public OrderReportVO getOrdersReport(LocalDate begin, LocalDate end) {
        //设置日期字符串
        List<LocalDate> dateList = new ArrayList<>();
        dateList.add(begin);
        while(!begin.equals(end)){
            begin = begin.plusDays(1);
            dateList.add(begin);
        }

        //订单总数
        Integer ordersCountSum = 0;
        //有效订单数
        Integer validOrderCountSum = 0;
        //每日订单数
        List<Integer> ordersCountList = new ArrayList<>();
        //每日有效订单数
        List<Integer> validOrderCountList = new ArrayList<>();
        for(LocalDate date : dateList){
            LocalDateTime dateBegin = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime dateEnd = LocalDateTime.of(date, LocalTime.MAX);

            Map<String,Object> map = new HashMap<>();
            map.put("begin", dateBegin);
            map.put("end", dateEnd);
            Integer ordersCount = orderMapper.countOrdersBetweenTime(map);
            map.put("status", Orders.COMPLETED);
            Integer validOrderCount = orderMapper.countOrdersBetweenTime(map);

            ordersCountSum += ordersCount == null? 0: ordersCount;
            validOrderCountSum += validOrderCount == null? 0: validOrderCount;

            ordersCountList.add(ordersCount);
            validOrderCountList.add(validOrderCount);
        }

        //订单完成率
        Double orderCompletionRate = 0.0;
        if(ordersCountSum != 0){
            orderCompletionRate = (double)validOrderCountSum / (double)ordersCountSum;
        }

        return OrderReportVO.builder()
                .dateList(StringUtils.join(dateList, ","))
                .orderCountList(StringUtils.join(ordersCountList, ","))
                .validOrderCountList(StringUtils.join(validOrderCountList, ","))
                .totalOrderCount(ordersCountSum)
                .validOrderCount(validOrderCountSum)
                .orderCompletionRate(orderCompletionRate)
                .build();
    }

    /**
     * 统计销量top10
     * @param begin
     * @param end
     * @return
     */
    public SalesTop10ReportVO getSalesTop10Report(LocalDate begin, LocalDate end) {
        LocalDateTime dateBegin = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime dateEnd = LocalDateTime.of(end, LocalTime.MAX);

        List<GoodsSalesDTO> salesTop10 = orderMapper.getSalesTop10(dateBegin, dateEnd);

        List<String> names = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
        for(GoodsSalesDTO goodsSalesDTO : salesTop10){
            names.add(goodsSalesDTO.getName());
            numbers.add(goodsSalesDTO.getNum());
        }

        return SalesTop10ReportVO.builder()
                .nameList(StringUtils.join(names, ","))
                .numberList(StringUtils.join(numbers, ","))
                .build();
    }

    /**
     * 导出数据
     * @param httpServletResponse
     */
    public void exportDataReport(HttpServletResponse httpServletResponse) {
        //导出最近30天的数据
        LocalDate dateBegin = LocalDate.now().minusDays(30);
        LocalDate dateEnd = LocalDate.now().minusDays(1);
        //获取概览数据
        BusinessDataVO businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(dateBegin, LocalTime.MIN), LocalDateTime.of(dateEnd, LocalTime.MAX));
        //根据模板创建Excel文件
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("templates/outputTemplate.xlsx");
        if(inputStream == null){
            throw new BaseException("找不到模板文件");
        }
        try {
            XSSFWorkbook excel = new XSSFWorkbook(inputStream);
            XSSFSheet sheet = excel.getSheet("Sheet1");
            sheet.getRow(2).createCell(0).setCellValue("时间：" + dateBegin + " --- " + dateEnd);

            XSSFRow row = sheet.getRow(4);
            row.createCell(1).setCellValue(businessDataVO.getTurnover());
            row.createCell(3).setCellValue(businessDataVO.getOrderCompletionRate());
            row.createCell(5).setCellValue(businessDataVO.getNewUsers());

            row = sheet.getRow(5);
            row.createCell(1).setCellValue(businessDataVO.getValidOrderCount());
            row.createCell(3).setCellValue(businessDataVO.getUnitPrice());

            //计算每天的营业数据
            for(int i = 0; i < 30; i++){
                LocalDate date = dateBegin.plusDays(i);
                businessDataVO = workspaceService.getBusinessData(LocalDateTime.of(date, LocalTime.MIN), LocalDateTime.of(date, LocalTime.MAX));
                row = sheet.createRow(8 + i);
                row.createCell(0).setCellValue(date.toString());
                row.createCell(1).setCellValue(businessDataVO.getTurnover());
                row.createCell(2).setCellValue(businessDataVO.getValidOrderCount());
                row.createCell(3).setCellValue(businessDataVO.getOrderCompletionRate());
                row.createCell(4).setCellValue(businessDataVO.getUnitPrice());
                row.createCell(5).setCellValue(businessDataVO.getNewUsers());
            }

            ServletOutputStream outputStream = httpServletResponse.getOutputStream();
            excel.write(outputStream);

            outputStream.close();
            excel.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
