package MikiMock.com.MikiMock.Analytics.service;

import MikiMock.com.MikiMock.Analytics.dto.DashboardResponse;
import org.springframework.stereotype.Service;

@Service
public interface AnalyticsService {

    DashboardResponse getDashboard();

}
