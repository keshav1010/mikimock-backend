package MikiMock.com.MikiMock.Question.controller;


import MikiMock.com.MikiMock.Common.Response.ApiResponse;
import MikiMock.com.MikiMock.Common.Response.ResponseUtil;
import MikiMock.com.MikiMock.Question.dto.QuestionResponse;
import MikiMock.com.MikiMock.Question.service.QuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping("/DSA")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> getAllQuestions() {

        List<QuestionResponse> questions = questionService.getAllDSAQuestions();

        return ResponseUtil.success("Questions fetched successfully", questions);
    }
}