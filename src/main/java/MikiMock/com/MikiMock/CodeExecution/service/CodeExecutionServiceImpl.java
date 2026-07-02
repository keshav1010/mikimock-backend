package MikiMock.com.MikiMock.CodeExecution.service;


import MikiMock.com.MikiMock.CodeExecution.client.CodeRunnerClient;
import MikiMock.com.MikiMock.CodeExecution.dto.*;
import MikiMock.com.MikiMock.CodeExecution.repository.QuestionTestCaseRepository;
import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import MikiMock.com.MikiMock.Interview.Repository.CodeTemplateRepository;
import MikiMock.com.MikiMock.Interview.entity.QuestionCodeTemplate;
import MikiMock.com.MikiMock.Interview.entity.QuestionTestCase;
import MikiMock.com.MikiMock.Room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeExecutionServiceImpl implements CodeExecutionService {

    private final CodeRunnerClient codeRunnerClient;

    private final CodeInputInjectionService inputInjectionService;

    private final QuestionTestCaseRepository testCaseRepository;

    private final CodeTemplateRepository codeTemplateRepository;

    @Override
    public CodeRunResponse runCode(
            CodeRunRequest request
    ) {

        QuestionCodeTemplate template =
                codeTemplateRepository
                        .findByQuestionIdAndLanguageIgnoreCase(
                                request.getQuestionId(),
                                request.getLanguage()
                        )
                        .orElseThrow(() ->
                                new BusinessException("Code template not found")
                        );

        RunnerRunRequest runnerRequest =
                RunnerRunRequest.builder()
                        .language(request.getLanguage())
                        .code(request.getCode())
                        .executionConfig(template.getExecutionConfig())
                        .input(
                                request.getInput() == null
                                        ? ""
                                        : request.getInput()
                        )
                        .build();

        return codeRunnerClient.runFast(
                runnerRequest
        );
    }

//    @Override
//    public CodeRunResponse runCode(CodeRunRequest request) {
//        if (
//                request.getCode() == null ||
//                        request.getCode().isBlank()
//        ) {
//            throw new BusinessException("Code is required");
//        }
//
//        if (
//                request.getLanguage() == null ||
//                        request.getLanguage().isBlank()
//        ) {
//            throw new BusinessException("Language is required");
//        }
//
//        request.setInput(
//                request.getInput() == null
//                        ? ""
//                        : request.getInput()
//        );
//
//        return codeRunnerClient.run(
//                request
//        );

//        String executableCode =inputInjectionService.injectInput(
//                        request.getLanguage(),
//                        request.getCode(),
//                        request.getInput()
//                );
//
//        request.setCode(
//                executableCode
//        );
//
//        request.setInput("");
//
//        return codeRunnerClient.run(
//                request
//        );
//    }

    @Override
    public CodeSubmitResponse submitCode(
            CodeSubmitRequest request
    ) {

        QuestionCodeTemplate template =
                codeTemplateRepository
                        .findByQuestionIdAndLanguageIgnoreCase(
                                request.getQuestionId(),
                                request.getLanguage()
                        )
                        .orElseThrow(() ->
                                new BusinessException("Code template not found")
                        );

        List<QuestionTestCase> testCases =
                testCaseRepository
                        .findByQuestionIdAndActiveTrueOrderByTestOrderAsc(
                                request.getQuestionId()
                        );

        List<RunnerTestCase> runnerTestCases =
                new ArrayList<>();

        int index = 1;

        for (QuestionTestCase testCase : testCases) {

            runnerTestCases.add(
                    RunnerTestCase.builder()
                            .testCaseNumber(index++)
                            .input(testCase.getInputData())
                            .expectedOutput(testCase.getExpectedOutput())
                            .build()
            );
        }

        RunnerSubmitRequest runnerRequest =
                RunnerSubmitRequest.builder()
                        .language(request.getLanguage())
                        .code(request.getCode())
                        .executionConfig(template.getExecutionConfig())
                        .testCases(runnerTestCases)
                        .build();

        return codeRunnerClient.submit(runnerRequest);
    }

    private String normalize(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replace("\r\n", "\n")
                .trim();
    }


}


//@Service
//@RequiredArgsConstructor
//public class CodeExecutionServiceImpl implements CodeExecutionService {
//
//    private final CodeRunnerClient codeRunnerClient;
//
//    private final RoomRepository roomRepository;
//
//    @Override
//    public CodeRunResponse runCode(
//            CodeRunRequest request
//    ) {
//
//        if (
//                request.getRoomCode() == null ||
//                        request.getRoomCode().isBlank()
//        ) {
//            throw new BusinessException("Room code is required");
//        }
//
//        if (
//                request.getCode() == null ||
//                        request.getCode().isBlank()
//        ) {
//            throw new BusinessException("Code is required");
//        }
//
//        if (
//                request.getCode().length() > 100_000
//        ) {
//            throw new BusinessException("Code size is too large");
//        }
//
//        if (
//                request.getLanguage() == null ||
//                        !request.getLanguage().equalsIgnoreCase("java")
//        ) {
//            throw new BusinessException("Only Java is supported currently");
//        }
//
//        roomRepository.findByRoomCode(
//                request.getRoomCode()
//        ).orElseThrow(() ->
//                new BusinessException("Room not found")
//        );
//
//        return codeRunnerClient.run(
//                request
//        );
//    }
//
//}