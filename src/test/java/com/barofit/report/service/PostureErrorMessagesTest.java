package com.barofit.report.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostureErrorMessagesTest {
  @Test
  void mapsAllKnownPoseErrorCodesAndOrdersByFrequency() {
    List<String> messages =
        PostureErrorMessages.buildFeedbackMessages(
            Map.of("HIP_DROP", 1L, "SQUAT_SHALLOW", 4L, "GOOD_FORM", 2L, "UNKNOWN_CODE", 3L),
            List.of());
    assertThat(messages)
        .containsExactly(
            "내려가는 깊이가 얕습니다. 의자에 앉는 느낌으로 끝까지 내려가 주세요.", "골반이 아래로 처지지 않도록 몸통을 곧게 유지해 주세요.");
  }

  @Test
  void fallsBackToWarningPostureItemsWhenErrorCountsAreEmpty() {
    List<String> messages =
        PostureErrorMessages.buildFeedbackMessages(
            Map.of(),
            List.of(
                new GeneratedReportService.Item("hip", "repetition", "repetitions", 8, 2, 80.0),
                new GeneratedReportService.Item("depth", "repetition", "repetitions", 5, 5, 50.0)));
    assertThat(messages).containsExactly("동작 깊이 자세를 조금 더 신경 써 주세요.", "골반 정렬 자세를 조금 더 신경 써 주세요.");
  }
}
