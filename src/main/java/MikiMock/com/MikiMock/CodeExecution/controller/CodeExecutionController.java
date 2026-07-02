package MikiMock.com.MikiMock.CodeExecution.controller;

import MikiMock.com.MikiMock.CodeExecution.dto.CodeRunRequest;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeRunResponse;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeSubmitRequest;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeSubmitResponse;
import MikiMock.com.MikiMock.CodeExecution.service.CodeExecutionService;
import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/code")
public class CodeExecutionController {

    private final CodeExecutionService codeExecutionService;

    @PostMapping("/run")
    public ResponseEntity<ApiResponse<CodeRunResponse>> runCode(
            @RequestBody CodeRunRequest request
    ) {

        CodeRunResponse response = codeExecutionService.runCode(request);

        return ResponseUtil.success(
                "Code executed successfully",
                response

        );
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<CodeSubmitResponse>> submit(
            @RequestBody CodeSubmitRequest request
    ) {

        return  ResponseUtil.success(
                        "Code submitted successfully",
                        codeExecutionService.submitCode(request)
        );
    }
}
