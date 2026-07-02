package MikiMock.com.MikiMock.CodeExecution.client;

import MikiMock.com.MikiMock.CodeExecution.config.CodeRunnerProperties;
import MikiMock.com.MikiMock.CodeExecution.dto.*;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import tools.jackson.databind.ObjectMapper;


@Service
@RequiredArgsConstructor
@Slf4j
public class CodeRunnerClient {

    private final RestTemplate restTemplate;

    private final CodeRunnerProperties properties;

    public CodeRunResponse runFast(
            RunnerRunRequest request
    ) {

        try {

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.set(
                    "X-Runner-Secret",
                    properties.getSecret()
            );

            HttpEntity<RunnerRunRequest> entity =
                    new HttpEntity<>(
                            request,
                            headers
                    );

            ResponseEntity<CodeRunResponse> response =
                    restTemplate.exchange(
                            properties.getUrl() + "/run",
                            HttpMethod.POST,
                            entity,
                            CodeRunResponse.class
                    );

            return response.getBody();

        } catch (Exception e) {

            log.error(
                    "Runner run request failed",
                    e
            );

            throw new BusinessException(
                    "Code runner run failed"
            );
        }
    }

    public CodeSubmitResponse submit(
            RunnerSubmitRequest request
    ) {

        try {

            HttpHeaders headers =
                    new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            headers.set(
                    "X-Runner-Secret",
                    properties.getSecret()
            );

            HttpEntity<RunnerSubmitRequest> entity =
                    new HttpEntity<>(
                            request,
                            headers
                    );

            ResponseEntity<CodeSubmitResponse> response =
                    restTemplate.exchange(
                            properties.getUrl() + "/submit",
                            HttpMethod.POST,
                            entity,
                            CodeSubmitResponse.class
                    );

            return response.getBody();

        } catch (Exception e) {
            log.info(
                    "Runner submit request: {}",
                    new ObjectMapper().writeValueAsString(request)
            );
            throw new BusinessException(
                    e.getMessage()
            );
        }
    }
}

