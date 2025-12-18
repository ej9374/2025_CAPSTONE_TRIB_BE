package triB.triB.global.utils;

import org.springframework.stereotype.Component;
import triB.triB.global.exception.CustomException;
import triB.triB.global.exception.ErrorCode;

import java.util.Arrays;
import java.util.List;

@Component
public class CheckBadWordsUtil {

    private static final List<String> KOREAN_BAD_WORDS = Arrays.asList(
            "시발", "씨발", "ㅅㅂ", "ㅆㅂ",
            "개새끼", "새끼", "ㄱㅅㄲ",
            "병신", "ㅂㅅ",
            "지랄", "ㅈㄹ",
            "닥쳐", "꺼져",
            "미친", "애미", "년"
    );

    private static final List<String> ENGLISH_BAD_WORDS = Arrays.asList(
            "fuck", "shit", "bitch", "asshole",
            "bastard", "damn", "crap", "ass",
            "dick", "pussy"
    );

    /**
     * 욕설이 있으면 예외를 던지는 검증 함수
     */
    public void validateNoBadWords(String text) {
        if (containsBadWords(text)) {
            throw new CustomException(ErrorCode.USE_BAD_WORDS);
        }
    }

    /**
     * 텍스트에 욕설이 포함되어 있는지 확인
     */
    private boolean containsBadWords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();

        // 한국어 욕설 확인
        for (String word : KOREAN_BAD_WORDS) {
            if (text.contains(word) || lowerText.contains(word.toLowerCase())) {
                return true;
            }
        }

        // 영어 욕설 확인
        for (String word : ENGLISH_BAD_WORDS) {
            if (lowerText.contains(word)) {
                return true;
            }
        }

        return false;
    }
}