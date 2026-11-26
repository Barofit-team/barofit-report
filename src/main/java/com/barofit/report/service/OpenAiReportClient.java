package com.barofit.report.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Generates Korean workout report coaching text from deterministic posture facts via OpenAI. */
@Component
public class OpenAiReportClient {
  private static final String INSTRUCTIONS =
      """
      한국어 AI 운동 코치입니다. 제공된 운동 수행 사실(Facts JSON)만 사용해 리포트 문장을 작성하세요.
      제공된 수치·오류 코드·항목 외에 없는 오류나 성과, 횟수를 지어내지 마세요.
      의료적 진단·치료·부상 예방을 보장하는 표현은 사용하지 말고, 통증 시 즉시 중단 원칙을 지키세요.
      feedbackMessages는 상세 화면의 AI 자세 피드백 말풍선에 들어가므로, 종목당 1~4개의 짧고 구체적인 교정 문장(각 60자 이내)으로 작성하세요.
      """;

  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
  private final ObjectMapper json;
  private final String key;
  private final String model;
  private final URI endpoint;

  public record LlmExerciseFeedback(
      String routineExerciseId,
      String strengths,
      String improvements,
      String nextGoal,
      List<String> feedbackMessages) {}

  public record LlmReportResult(String overallFeedback, List<LlmExerciseFeedback> exercises) {}

  public OpenAiReportClient(
      ObjectMapper json,
      @Value("${barofit.openai.api-key:}") String key,
      @Value("${barofit.openai.model:gpt-6-luna}") String model,
      @Value("${barofit.openai.base-url:https://api.openai.com}") String baseUrl) {
    this.json = json;
    this.key = key == null ? "" : key;
    this.model = model;
    this.endpoint = URI.create(baseUrl + "/v1/responses");
  }

  public boolean configured() {
    return !key.isBlank();
  }

  public LlmReportResult generate(Map<String, Object> facts)
      throws IOException, InterruptedException {
    if (!configured()) {
      throw new IOException("OpenAI credentials are not configured");
    }

    Map<String, Object> exerciseSchema =
        Map.of(
            "type",
            "object",
            "additionalProperties",
            false,
            "properties",
            Map.of(
                "routineExerciseId", Map.of("type", "string"),
                "strengths", Map.of("type", "string"),
                "improvements", Map.of("type", "string"),
                "nextGoal", Map.of("type", "string"),
                "feedbackMessages", Map.of("type", "array", "items", Map.of("type", "string"))),
            "required",
            List.of(
                "routineExerciseId", "strengths", "improvements", "nextGoal", "feedbackMessages"));

    Map<String, Object> format =
        Map.of(
            "type",
            "json_schema",
            "name",
            "workout_report_feedback",
            "strict",
            true,
            "schema",
            Map.of(
                "type",
                "object",
                "additionalProperties",
                false,
                "properties",
                Map.of(
                    "overallFeedback", Map.of("type", "string"),
                    "exercises", Map.of("type", "array", "items", exerciseSchema)),
                "required",
                List.of("overallFeedback", "exercises")));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("model", model);
    body.put("store", false);
    body.put("max_output_tokens", 900);
    if ("gpt-6-luna".equals(model)) {
      body.put("reasoning", Map.of("effort", "none"));
    }
    body.put("instructions", INSTRUCTIONS);
    body.put("input", json.writeValueAsString(facts));
    body.put("text", Map.of("format", format));

    HttpRequest request =
        HttpRequest.newBuilder(endpoint)
            .timeout(Duration.ofSeconds(8))
            .header("Authorization", "Bearer " + key)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
            .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IOException("OpenAI returned HTTP " + response.statusCode());
    }

    JsonNode result = json.readTree(response.body());
    if (!"completed".equals(result.path("status").asText())) {
      throw new IOException("OpenAI response was not completed");
    }

    for (JsonNode item : result.path("output")) {
      if (!"message".equals(item.path("type").asText())) continue;
      for (JsonNode content : item.path("content")) {
        if (!"output_text".equals(content.path("type").asText())) continue;
        return parseResult(json.readTree(content.path("text").asText()));
      }
    }
    throw new IOException("OpenAI response did not contain report text");
  }

  static final int MAX_SENTENCE_LENGTH = 200;
  static final int MAX_MESSAGE_LENGTH = 80;

  LlmReportResult parseResult(JsonNode root) throws IOException {
    String overall = singleLine(root.path("overallFeedback").asText(""), MAX_SENTENCE_LENGTH);
    if (overall == null) {
      throw new IOException("OpenAI overallFeedback did not meet the length and format rules");
    }
    List<LlmExerciseFeedback> exercises = new ArrayList<>();
    for (JsonNode node : root.path("exercises")) {
      String id = node.path("routineExerciseId").asText("").trim();
      if (id.isBlank()) continue;
      List<String> messages = new ArrayList<>();
      for (JsonNode msg : node.path("feedbackMessages")) {
        // Over-long or multi-line messages are dropped, never truncated mid-sentence.
        String text = singleLine(msg.asText(""), MAX_MESSAGE_LENGTH);
        if (text != null && messages.size() < 5 && !messages.contains(text)) {
          messages.add(text);
        }
      }
      exercises.add(
          new LlmExerciseFeedback(
              id,
              singleLine(node.path("strengths").asText(""), MAX_SENTENCE_LENGTH),
              singleLine(node.path("improvements").asText(""), MAX_SENTENCE_LENGTH),
              singleLine(node.path("nextGoal").asText(""), MAX_SENTENCE_LENGTH),
              List.copyOf(messages)));
    }
    return new LlmReportResult(overall, List.copyOf(exercises));
  }

  /** Returns the trimmed text, or null when blank, multi-line, or longer than maxLength. */
  private static String singleLine(String value, int maxLength) {
    String trimmed = value == null ? "" : value.trim();
    if (trimmed.isEmpty()
        || trimmed.length() > maxLength
        || trimmed.indexOf('\n') >= 0
        || trimmed.indexOf('\r') >= 0) {
      return null;
    }
    return trimmed;
  }
}
