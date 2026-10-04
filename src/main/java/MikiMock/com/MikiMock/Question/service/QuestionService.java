package MikiMock.com.MikiMock.Question.service;

import MikiMock.com.MikiMock.Question.dto.QuestionResponse;

import java.util.List;

public interface QuestionService {

    List<QuestionResponse> getAllDSAQuestions();
}