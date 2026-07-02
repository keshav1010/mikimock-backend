package MikiMock.com.MikiMock.CodeExecution.service;

import MikiMock.com.MikiMock.CodeExecution.dto.CodeRunRequest;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeRunResponse;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeSubmitRequest;
import MikiMock.com.MikiMock.CodeExecution.dto.CodeSubmitResponse;
import org.springframework.stereotype.Service;

@Service
public interface CodeExecutionService {

    CodeRunResponse runCode(CodeRunRequest request);

    CodeSubmitResponse submitCode(CodeSubmitRequest request);


}
