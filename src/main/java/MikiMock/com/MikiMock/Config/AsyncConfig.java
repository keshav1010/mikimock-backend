package MikiMock.com.MikiMock.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "analyticsExecutor")
    public Executor analyticsExecutor() {

        ThreadPoolTaskExecutor executor =
                new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);

        executor.setMaxPoolSize(16);

        executor.setQueueCapacity(100);

        executor.setThreadNamePrefix("analytics-");

        executor.initialize();

        return executor;
    }

//    @Bean(destroyMethod = "shutdown")       //When Spring shuts down, it automatically calls for shutdown
//    public ExecutorService analyticsExecutor() {
//
//        return Executors.newFixedThreadPool(8);
//    }

}