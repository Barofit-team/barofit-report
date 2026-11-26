package com.barofit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.barofit.report.repository.ReportRepository.ScoreItem;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import tools.jackson.databind.ObjectMapper;

class GeneratedReportLlmTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  void usesLlmFeedbackWhenConfiguredAndFallsBackOnFailure() throws Exception {
    UUID user = UUID.randomUUID(), session = UUID.randomUUID(), set = UUID.randomUUID();
    UUID exId = UUID.randomUUID();
    var source =
        json.readTree(
            """
            {"status":"COMPLETED","endedAt":"2026-11-26T10:00:00Z","activeDurationSeconds":120,
             "sets":[{"routineSetId":"%s","actualReps":10,"errorCounts":{"HIP_DROP":2}}],
             "routineSnapshot":{"name":"상체 루틴","exercises":[
               {"id":"%s","exerciseId":"pushup","name":"푸시업","sets":[{"id":"%s"}]}
             ]}}
            """
                .formatted(set, exId, set));

    ReportSourceClient sources = mock(ReportSourceClient.class);
    ReportService reports = mock(ReportService.class);
    JdbcTemplate jdbc = mock(JdbcTemplate.class);
    OpenAiReportClient llm = mock(OpenAiReportClient.class);

    AtomicReference<String> saved = new AtomicReference<>();
    when(sources.workout(anyString(), eq("Bearer test"))).thenReturn(source);
    when(reports.scoreItems(user, session))
        .thenReturn(List.of(new ScoreItem(set, "hip", "repetition", "repetitions", 8, 2)));
    when(jdbc.query(
            anyString(),
            org.mockito.ArgumentMatchers.<RowMapper<String>>any(),
            eq(user),
            eq(session)))
        .thenAnswer(inv -> saved.get() == null ? List.of() : List.of(saved.get()));
    when(jdbc.update(anyString(), eq(session), eq(user), anyString()))
        .thenAnswer(inv -> {
          saved.set(inv.getArgument(3));
          return 1;
        });

    when(llm.configured()).thenReturn(true);
    when(llm.generate(any()))
        .thenReturn(
            new OpenAiReportClient.LlmReportResult(
                "상체 루틴을 안정적으로 완주했어요!",
                List.of(
                    new OpenAiReportClient.LlmExerciseFeedback(
                        exId.toString(),
                        "골반 정렬이 전반적으로 잘 유지됐어요.",
                        "후반부에 골반이 살짝 내려가는 경향이 있었어요.",
                        "다음에는 코어에 힘을 주고 내려가 보세요.",
                        List.of("골반이 아래로 처지지 않게 복부에 힘을 유지해 주세요.")))));

    var service = new GeneratedReportService(sources, reports, jdbc, json, llm);
    var generated = service.generate(user, session, "Bearer test");

    assertThat(generated.generationMethod()).isEqualTo("LLM_V1");
    assertThat(generated.overallFeedback()).isEqualTo("상체 루틴을 안정적으로 완주했어요!");
    assertThat(generated.exercises().getFirst().feedbackMessages())
        .containsExactly("골반이 아래로 처지지 않게 복부에 힘을 유지해 주세요.");

    // Reset saved and verify fallback on IOException
    saved.set(null);
    when(llm.generate(any())).thenThrow(new IOException("timeout"));
    var fallback = service.generate(user, session, "Bearer test");

    assertThat(fallback.generationMethod()).isEqualTo("TEMPLATE_V1");
    assertThat(fallback.exercises().getFirst().feedbackMessages())
        .containsExactly("골반이 아래로 처지지 않도록 몸통을 곧게 유지해 주세요.");
  }
}
