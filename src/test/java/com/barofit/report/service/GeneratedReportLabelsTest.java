package com.barofit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.barofit.report.repository.ReportRepository.ScoreItem;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

class GeneratedReportLabelsTest {
  @Test
  void generatedFeedbackUsesKoreanNamesBeyondTheOriginalFourLabels() throws Exception {
    UUID user = UUID.randomUUID(), session = UUID.randomUUID(), set = UUID.randomUUID();
    ObjectMapper json = new ObjectMapper();
    ReportSourceClient sources = mock(ReportSourceClient.class);
    ReportService reports = mock(ReportService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    AtomicReference<String> saved = new AtomicReference<>();
    var source =
        json.readTree(
            """
            {"status":"COMPLETED","startedAt":"2026-11-25T00:00:00Z",
             "endedAt":"2026-11-25T00:01:00Z","activeDurationSeconds":60,
             "routineSnapshot":{"name":"테스트","exercises":[
               {"id":"exercise","exerciseId":"leg_raise","name":"레그 레이즈",
                "sets":[{"id":"%s"}]}]},
             "sets":[{"routineSetId":"%s","actualReps":4,"actualDurationSeconds":0,
                      "errorCounts":{"KNEE_BENT":1}}]}
            """
                .formatted(set, set));
    when(sources.workout(anyString(), eq("Bearer test"))).thenReturn(source);
    when(reports.scoreItems(user, session))
        .thenReturn(
            List.of(new ScoreItem(set, "knee_extension", "repetition", "repetitions", 3, 1)));
    when(jdbc.query(
            anyString(),
            org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
            eq(user),
            eq(session)))
        .thenAnswer(invocation -> saved.get() == null ? List.of() : List.of(saved.get()));
    when(jdbc.update(anyString(), eq(session), eq(user), anyString()))
        .thenAnswer(invocation -> {
          saved.set(invocation.getArgument(3));
          return 1;
        });
    var service = new GeneratedReportService(sources, reports, jdbc, json);
    var generated = service.generate(user, session, "Bearer test");
    assertThat(generated.exercises().getFirst().strengths()).contains("무릎 펴기");
    assertThat(generated.exercises().getFirst().improvements()).contains("무릎 펴기");
    assertThat(generated.exercises().getFirst().nextGoal()).contains("무릎 펴기");
    assertThat(generated.exercises().getFirst().errorCounts()).containsEntry("KNEE_BENT", 1L);
  }

  @Test
  void allPoseCatalogItemsHaveTheSameKoreanLabel() throws Exception {
    try (var input = getClass().getResourceAsStream("/pose-item-labels.json")) {
      assertThat(input).isNotNull();
      var labels = new ObjectMapper().readTree(input).path("labels");
      assertThat(labels.size()).isGreaterThan(40);
      labels.forEachEntry(
          (id, label) -> assertThat(PostureItemLabels.label(id)).isEqualTo(label.asText()));
    }
  }

  @Test
  void labelsCoverSharedAndSideSpecificItemsAndDoNotExposeUnknownIds() {
    assertThat(PostureItemLabels.label("hip")).isEqualTo("골반 정렬");
    assertThat(PostureItemLabels.label("pike_shape")).isEqualTo("파이크 자세");
    assertThat(PostureItemLabels.label("opposite_arm")).isEqualTo("반대쪽 팔 협응");
    assertThat(PostureItemLabels.label("left_support_knee_alignment")).isEqualTo("왼쪽 지지 무릎 정렬");
    assertThat(PostureItemLabels.label("right_elbow_bent")).isEqualTo("오른쪽 팔꿈치 펴기");
    assertThat(PostureItemLabels.label("unknown_future_id")).isEqualTo("자세 평가 항목");
    assertThat(PostureItemLabels.label(null)).isEqualTo("자세 평가 항목");
  }
}
