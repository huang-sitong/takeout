package com.sky.controller.admin;

import com.sky.result.Result;
import com.sky.service.ReportService;
import com.sky.vo.order.OrderReportVO;
import com.sky.vo.order.SalesTop10ReportVO;
import com.sky.vo.order.TurnoverReportVO;
import com.sky.vo.order.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController("adminReportController")
@RequestMapping("/admin/report")
@Slf4j
public class ReportController {

    @Autowired
    private ReportService reportService;

    /**
     * 营业额统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/turnoverStatistics")
    public Result<TurnoverReportVO> getTurnoverReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计从 {} 到 {} 时间段的营业额",  begin, end);
        return Result.success(reportService.getTurnoverReport(begin, end));
    }

    /**
     * 用户数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/userStatistics")
    public Result<UserReportVO> getUserReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计从 {} 到 {} 时间段的用户统计", begin, end);
        return Result.success(reportService.getUserReport(begin, end));
    }

    /**
     * 订单数据统计
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/ordersStatistics")
    public Result<OrderReportVO> getOrdersReport(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计从 {} 到 {} 时间段的订单统计", begin, end);
        return Result.success(reportService.getOrdersReport(begin, end));
    }

    /**
     * 统计销量top10
     * @param begin
     * @param end
     * @return
     */
    @GetMapping("/top10")
    public Result<SalesTop10ReportVO> getSalesTop10Report(
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate begin,
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        log.info("统计从 {} 到 {} 时间段的销量top10", begin, end);
        return Result.success(reportService.getSalesTop10Report(begin, end));
    }


    /**
     * 导出数据
     * @param httpServletResponse
     */
    @GetMapping("/export")
    public void export(HttpServletResponse httpServletResponse){
        log.info("正在导出数据");
        reportService.exportDataReport(httpServletResponse);
    }
}
