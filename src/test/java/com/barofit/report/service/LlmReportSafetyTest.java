package com.barofit.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class LlmReportSafetyTest {
  private final ObjectMapper json = new ObjectMapper();
  private final OpenAiReportClient client =
      new OpenAiReportClient(json, "", "test-model", "http://localhost");

  @Test
  void legacyStoredReportWithoutFeedbackMessagesDeserializesToEmptyList() throws Exception {
    var legacy =
        json.readValue(
            """
            {"routineExerciseId":"a","exerciseId":"pushup","name":"푸시업","performedSets":1,
             "actualReps":5,"actualDurationSeconds":0,"postureScore":80.0,"errorCounts":{},
             "postureItems":[],"strengths":null,"improvements":null,"nextGoal":null}
            """,
            GeneratedReportService.Exercise.class);
    assertThat(legacy.feedbackMessages()).isEmpty();
  }

  @Test
  void dropsOverlongMultilineAndDuplicateMessagesButKeepsValidOnes() throws Exception {
    String tooLong = "가".repeat(OpenAiReportClient.MAX_MESSAGE_LENGTH + 1);
    var root =
        json.readTree(
            """
            {"overallFeedback":"잘했어요.",
             "exercises":[{"routineExerciseId":"a","strengths":" 좋아요 ","improvements":"줄\\n바꿈",
               "nextGoal":"","feedbackMessages":["골반을 곧게 유지해 주세요.","골반을 곧게 유지해 주세요.","%s","두\\n줄"]}]}
            """
                .formatted(tooLong));

    var result = client.parseResult(root);
    var exercise = result.exercises().getFirst();
    assertThat(exercise.feedbackMessages()).containsExactly("골반을 곧게 유지해 주세요.");
    assertThat(exercise.strengths()).isEqualTo("좋아요");
    assertThat(exercise.improvements()).isNull();
    assertThat(exercise.nextGoal()).isNull();
  }

  @Test
  void rejectsBlankOrInvalidOverallFeedbackSoServiceFallsBackToTemplate() throws Exception {
    var blank = json.readTree("{\"overallFeedback\":\"  \",\"exercises\":[]}");
    assertThatThrownBy(() -> client.parseResult(blank)).isInstanceOf(IOException.class);
    var multiline = json.readTree("{\"overallFeedback\":\"a\\nb\",\"exercises\":[]}");
    assertThatThrownBy(() -> client.parseResult(multiline)).isInstanceOf(IOException.class);
  }

  @Test
  void notConfiguredClientNeverCallsOpenAi() {
    assertThat(client.configured()).isFalse();
    assertThatThrownBy(() -> client.generate(java.util.Map.of())).isInstanceOf(IOException.class);
  }
}
