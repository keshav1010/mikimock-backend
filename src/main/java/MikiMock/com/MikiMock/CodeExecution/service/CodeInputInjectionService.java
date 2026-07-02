package MikiMock.com.MikiMock.CodeExecution.service;

import MikiMock.com.MikiMock.Common.Exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;

@Service
@Slf4j
public class CodeInputInjectionService {

    public String injectInput(
            String language,
            String code,
            String rawInput
    ) {
        log.info("Raw input = [{}]", rawInput);

        String replacement =
                buildReplacement(
                        language,
                        rawInput
                );

        log.info("Replacement = [{}]", replacement);

        return code.replaceAll(
                "(?s)// MIKIMOCK_INPUT_START.*?// MIKIMOCK_INPUT_END",
                Matcher.quoteReplacement(replacement)
        );
    }

    private String buildReplacement(
            String language,
            String rawInput
    ) {
        log.info("Language ={}",language);

        String normalized =
                rawInput.trim();

        return switch (
                language.toLowerCase()
                ) {
            case "java" ->
                    """
                    // MIKIMOCK_INPUT_START
                    int[] nums = new int[]{%s};
                    // MIKIMOCK_INPUT_END
                    """.formatted(
                            normalized
                                    .replace("[", "")
                                    .replace("]", "")
                    );

            case "cpp" ->
                    """
                    // MIKIMOCK_INPUT_START
                    vector<int> nums = %s;
                    // MIKIMOCK_INPUT_END
                    """.formatted(
                            normalized
                                    .replace("[", "{")
                                    .replace("]", "}")
                    );

            case "python" ->
                    """
                    # MIKIMOCK_INPUT_START
                    nums = %s
                    # MIKIMOCK_INPUT_END
                    """.formatted(normalized);

            case "javascript" ->
                    """
                    // MIKIMOCK_INPUT_START
                    const nums = %s;
                    // MIKIMOCK_INPUT_END
                    """.formatted(normalized);

            default ->
                    throw new BusinessException(
                            "Unsupported language"
                    );
        };
    }
}