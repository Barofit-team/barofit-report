package com.barofit.report.service;

import com.barofit.report.repository.ReportRepository.ScoreItem;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Immutable, deterministic report generated once from the finished workout and its uploaded AI
 * results.
 */
@Service
public class GeneratedReportService {
  private static final Logger log = LoggerFactory.getLogger(GeneratedReportService.class);
  private final ReportSourceClient sources;
  private final ReportService reports;
  private final JdbcTemplate jdbc;
  private final ObjectMapper json;
  private final OpenAiReportClient llm;

  public GeneratedReportService(
      ReportSourceClient sources, ReportService reports, JdbcTemplate jdbc, ObjectMapper json) {
    this(sources, reports, jdbc, json, null);
  }

  @Autowired
  public GeneratedReportService(
      ReportSourceClient sources,
      ReportService reports,
      JdbcTemplate jdbc,
      ObjectMapper json,
      OpenAiReportClient llm) {
    this.sources = sources;
    this.reports = reports;
    this.jdbc = jdbc;
    this.json = json;
    this.llm = llm;
  }

  public record Item(
      String itemId,
      String aggregationBasis,
      String unit,
      double compliant,
      double warning,
      Double compliancePct) {}

  public record Exercise(
      String routineExerciseId,
      String exerciseId,
      String name,
      int performedSets,
      int actualReps,
      int actualDurationSeconds,
      Double postureScore,
      Map<String, Long> errorCounts,
      List<Item> postureItems,
      String strengths,
      String improvements,
      String nextGoal,
      List<String> feedbackMessages) {
    /**
     * Reports stored before feedbackMessages existed deserialize with null; expose an empty list.
     */
    public Exercise {
      feedbackMessages = feedbackMessages == null ? List.of() : List.copyOf(feedbackMessages);
    }
  }

  public record Generated(
      UUID sessionId,
      String routineName,
      String sessionStatus,
      int performedSets,
      int plannedSets,
      Integer restSeconds,
      int activeDurationSeconds,
      Long elapsedSeconds,
      Double routineScore,
      String overallFeedback,
      List<Exercise> exercises,
      String generationMethod) {}

  public Generated get(UUID user, UUID session, String auth) {
    sources.workout("/api/v1/workout-sessions/" + session, auth);
    Generated report = find(user, session);
    if (report == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Report has not been generated");
    return report;
  }

  public Generated generate(UUID user, UUID sessionId, String auth) {
    JsonNode session = sources.workout("/api/v1/workout-sessions/" + sessionId, auth);
    Generated saved = find(user, sessionId);
    if (saved != null) return saved;
    if (!Set.of("COMPLETED", "ABANDONED").contains(session.path("status").asText())
        || !session.path("endedAt").isTextual())
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Workout has not finished");
    Map<UUID, List<ScoreItem>> itemsBySet = new HashMap<>();
    for (ScoreItem item : reports.scoreItems(user, sessionId))
      itemsBySet.computeIfAbsent(item.routineSetId(), ignored -> new ArrayList<>()).add(item);
    Map<String, JsonNode> actual = new HashMap<>();
    for (JsonNode set : session.path("sets")) actual.put(set.path("routineSetId").asText(), set);
    List<Exercise> exercises = new ArrayList<>();
    int totalPerformed = 0, totalPlanned = 0, scored = 0, plannedExercises = 0;
    double scoreSum = 0;
    for (JsonNode exercise : session.path("routineSnapshot").path("exercises")) {
      Map<String, double[]> totals = new TreeMap<>();
      Map<String, String[]> bases = new HashMap<>();
      Map<String, Long> errors = new TreeMap<>();
      int performed = 0, reps = 0, duration = 0;
      totalPlanned += exercise.path("sets").size();
      if (!exercise.path("sets").isEmpty()) plannedExercises++;
      for (JsonNode planned : exercise.path("sets")) {
        JsonNode set = actual.get(planned.path("id").asText());
        if (set == null
            || (set.path("actualReps").asInt(0) <= 0
                && set.path("actualDurationSeconds").asInt(0) <= 0)) continue;
        performed++;
        totalPerformed++;
        reps += set.path("actualReps").asInt(0);
        duration += set.path("actualDurationSeconds").asInt(0);
        set.path("errorCounts")
            .forEachEntry((key, value) -> errors.merge(key, value.asLong(), Long::sum));
        List<ScoreItem> setItems = itemsBySet.get(UUID.fromString(planned.path("id").asText()));
        if (setItems == null)
          throw new ResponseStatusException(
              HttpStatus.CONFLICT, "AI results are missing for performed sets");
        for (ScoreItem item : setItems) {
          if (item.itemId() == null) continue;
          String[] existing =
              bases.putIfAbsent(item.itemId(), new String[] {item.basis(), item.unit()});
          if (existing != null
              && (!existing[0].equals(item.basis()) || !existing[1].equals(item.unit())))
            throw new ResponseStatusException(
                HttpStatus.CONFLICT, "Posture item aggregation differs across sets");
          double[] values = totals.computeIfAbsent(item.itemId(), ignored -> new double[2]);
          values[0] += item.compliant();
          values[1] += item.warning();
        }
      }
      List<Item> posture = new ArrayList<>();
      double exerciseSum = 0;
      int evaluated = 0, warnings = 0;
      for (var entry : totals.entrySet()) {
        double[] value = entry.getValue();
        String[] basis = bases.get(entry.getKey());
        Double percentage = value[0] + value[1] > 0 ? 100 * value[0] / (value[0] + value[1]) : null;
        posture.add(new Item(entry.getKey(), basis[0], basis[1], value[0], value[1], percentage));
        if (percentage != null) {
          exerciseSum += percentage;
          evaluated++;
        }
        if (value[1] > 0) warnings++;
      }
      Double score = evaluated > 0 ? exerciseSum / evaluated : null;
      if (score != null && !exercise.path("sets").isEmpty()) {
        scored++;
        scoreSum += score;
      }
      Item best =
          posture.stream()
              .filter(i -> i.compliant() > 0 && i.compliancePct() != null)
              .max(Comparator.comparingDouble(Item::compliancePct))
              .orElse(null);
      Item worst =
          posture.stream()
              .filter(i -> i.warning() > 0 && i.compliancePct() != null)
              .min(Comparator.comparingDouble(Item::compliancePct))
              .orElse(null);
      String strengths =
          performed == 0
              ? null
              : evaluated == 0
                  ? "자세 평가 자료가 부족해 잘한 점을 확인하지 못했어요."
                  : best == null
                      ? "정상 판정된 자세 항목이 없어요."
                      : String.format(
                          Locale.ROOT,
                          "%s의 정상 준수율은 %.1f%%예요.",
                          itemLabel(best.itemId()),
                          best.compliancePct());
      String improvements =
          performed == 0 || evaluated == 0
              ? null
              : warnings == 0
                  ? "측정된 자세 항목에서 경고가 확인되지 않았어요."
                  : String.format(Locale.ROOT, "%s에서 자세 경고가 확인됐어요.", itemLabel(worst.itemId()));
      String goal =
          performed == 0
              ? null
              : worst == null
                  ? "다음 운동에서도 시작 전 자세 안내를 확인해 보세요."
                  : String.format("다음 운동에서는 %s의 자세 안내를 다시 확인해 보세요.", itemLabel(worst.itemId()));
      List<String> feedbackMessages =
          performed == 0 ? List.of() : PostureErrorMessages.buildFeedbackMessages(errors, posture);
      exercises.add(
          new Exercise(
              exercise.path("id").asText(),
              exercise.path("exerciseId").asText(),
              exercise.path("name").asText(),
              performed,
              reps,
              duration,
              score,
              Map.copyOf(errors),
              List.copyOf(posture),
              strengths,
              improvements,
              goal,
              List.copyOf(feedbackMessages)));
    }
    if (totalPerformed == 0)
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Workout has no performed sets");
    Double routineScore =
        plannedExercises > 0 && scored == plannedExercises ? scoreSum / plannedExercises : null;
    String overall =
        String.format(Locale.ROOT, "오늘 계획한 %d세트 중 %d세트를 수행했어요.", totalPlanned, totalPerformed);
    JsonNode rest = session.path("restDurationSeconds");
    Generated generated =
        new Generated(
            sessionId,
            session.path("routineSnapshot").path("name").asText(),
            session.path("status").asText(),
            totalPerformed,
            totalPlanned,
            rest.isInt() ? rest.asInt() : null,
            session.path("activeDurationSeconds").asInt(),
            elapsed(session),
            routineScore,
            overall,
            List.copyOf(exercises),
            "TEMPLATE_V1");
    generated = enrichWithLlm(generated);
    try {
      jdbc.update(
          "INSERT INTO generated_workout_reports(session_id,user_id,report_json) VALUES (?,?,?::jsonb) ON CONFLICT (session_id) DO NOTHING",
          sessionId,
          user,
          json.writeValueAsString(generated));
    } catch (JacksonException ex) {
      throw new IllegalStateException(ex);
    }
    Generated result = find(user, sessionId);
    if (result == null)
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found");
    return result;
  }

  private Generated enrichWithLlm(Generated template) {
    if (llm == null || !llm.configured()) {
      return template;
    }
    try {
      Map<String, Object> facts = buildLlmFacts(template);
      OpenAiReportClient.LlmReportResult ai = llm.generate(facts);
      Map<String, OpenAiReportClient.LlmExerciseFeedback> byId = new HashMap<>();
      for (var entry : ai.exercises()) {
        byId.put(entry.routineExerciseId(), entry);
      }
      List<Exercise> enrichedExercises = new ArrayList<>(template.exercises().size());
      for (Exercise ex : template.exercises()) {
        OpenAiReportClient.LlmExerciseFeedback aiEx = byId.get(ex.routineExerciseId());
        if (ex.performedSets() == 0 || aiEx == null) {
          enrichedExercises.add(ex);
          continue;
        }
        List<String> messages =
            aiEx.feedbackMessages().isEmpty() ? ex.feedbackMessages() : aiEx.feedbackMessages();
        enrichedExercises.add(
            new Exercise(
                ex.routineExerciseId(),
                ex.exerciseId(),
                ex.name(),
                ex.performedSets(),
                ex.actualReps(),
                ex.actualDurationSeconds(),
                ex.postureScore(),
                ex.errorCounts(),
                ex.postureItems(),
                aiEx.strengths() != null ? aiEx.strengths() : ex.strengths(),
                aiEx.improvements() != null ? aiEx.improvements() : ex.improvements(),
                aiEx.nextGoal() != null ? aiEx.nextGoal() : ex.nextGoal(),
                messages));
      }
      return new Generated(
          template.sessionId(),
          template.routineName(),
          template.sessionStatus(),
          template.performedSets(),
          template.plannedSets(),
          template.restSeconds(),
          template.activeDurationSeconds(),
          template.elapsedSeconds(),
          template.routineScore(),
          ai.overallFeedback(),
          List.copyOf(enrichedExercises),
          "LLM_V1");
    } catch (IOException | JacksonException ex) {
      log.warn(
          "Falling back to TEMPLATE_V1 for session {}: {}", template.sessionId(), ex.getMessage());
      return template;
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      log.warn("LLM report generation interrupted for session {}", template.sessionId());
      return template;
    }
  }

  private static Map<String, Object> buildLlmFacts(Generated template) {
    Map<String, Object> facts = new LinkedHashMap<>();
    facts.put("routineName", template.routineName());
    facts.put("performedSets", template.performedSets());
    facts.put("plannedSets", template.plannedSets());
    facts.put("routineScore", template.routineScore());
    facts.put("activeDurationSeconds", template.activeDurationSeconds());
    List<Map<String, Object>> exerciseFacts = new ArrayList<>();
    for (Exercise ex : template.exercises()) {
      if (ex.performedSets() == 0) continue;
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("routineExerciseId", ex.routineExerciseId());
      item.put("name", ex.name());
      item.put("performedSets", ex.performedSets());
      item.put("actualReps", ex.actualReps());
      item.put("actualDurationSeconds", ex.actualDurationSeconds());
      item.put("postureScore", ex.postureScore());
      List<Map<String, Object>> items = new ArrayList<>();
      for (Item posture : ex.postureItems()) {
        items.add(
            Map.of(
                "label", PostureItemLabels.label(posture.itemId()),
                "compliant", posture.compliant(),
                "warning", posture.warning(),
                "compliancePct", posture.compliancePct() == null ? 0.0 : posture.compliancePct()));
      }
      item.put("postureItems", items);
      List<Map<String, Object>> errors = new ArrayList<>();
      for (var err : ex.errorCounts().entrySet()) {
        String meaning = PostureErrorMessages.message(err.getKey());
        errors.add(
            Map.of(
                "code", err.getKey(),
                "meaning", meaning == null ? err.getKey() : meaning,
                "count", err.getValue()));
      }
      item.put("errors", errors);
      exerciseFacts.add(item);
    }
    facts.put("exercises", exerciseFacts);
    return facts;
  }

  private static Long elapsed(JsonNode session) {
    if (!session.path("startedAt").isTextual() || !session.path("endedAt").isTextual()) return null;
    return java.time.Duration.between(
            java.time.Instant.parse(session.path("startedAt").asText()),
            java.time.Instant.parse(session.path("endedAt").asText()))
        .getSeconds();
  }

  private static String itemLabel(String id) {
    return PostureItemLabels.label(id);
  }

  private Generated find(UUID user, UUID session) {
    List<String> rows =
        jdbc.query(
            "SELECT report_json::text FROM generated_workout_reports WHERE user_id=? AND session_id=?",
            (rs, n) -> rs.getString(1),
            user,
            session);
    if (rows.isEmpty()) return null;
    try {
      return json.readValue(rows.get(0), Generated.class);
    } catch (JacksonException ex) {
      throw new IllegalStateException("Stored report is invalid", ex);
    }
  }
}
