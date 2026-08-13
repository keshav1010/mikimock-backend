package MikiMock.com.MikiMock.Analytics.controller;

import MikiMock.com.MikiMock.Analytics.dto.DashboardResponse;
import MikiMock.com.MikiMock.Analytics.service.AnalyticsService;
import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardResponse>> dashboard() {

        return ResponseUtil.success(

                "Dashboard Loaded",

                analyticsService.getDashboard()

        );

    }

}