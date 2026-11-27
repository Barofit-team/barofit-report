package com.barofit.report.service;

import tools.jackson.databind.JsonNode;

/** Estimates burned kcal from performed session durations and routine metadata. */
final class CalorieEstimate {
  private static final double DEFAULT_WEIGHT_KG = 65.0;

  private CalorieEstimate() {}

  static Integer estimateKcal(JsonNode session) {
    if (session == null || session.isMissingNode()) return null;
    int activeSeconds = Math.max(0, session.path("activeDurationSeconds").asInt(0));
    if (activeSeconds <= 0) return null;

    int restSeconds = Math.max(0, session.path("restDurationSeconds").asInt(0));
    JsonNode snapshot = session.path("routineSnapshot");

    int plannedKcal = snapshot.path("estimatedKcal").asInt(0);
    int plannedMinutes = snapshot.path("estimatedMinutes").asInt(0);
    if (plannedKcal > 0 && plannedMinutes > 0) {
      double ratio = (activeSeconds + restSeconds) / (plannedMinutes * 60.0);
      return Math.max(1, (int) Math.round(plannedKcal * ratio));
    }

    String category = snapshot.path("category").asText("");
    String difficulty = snapshot.path("difficulty").asText("");
    double met = "CARDIO".equals(category) && "ADVANCED".equals(difficulty) ? 7.5 : 3.8;
    double energy =
        (met * activeSeconds + 1.3 * restSeconds) / 60.0 * 3.5 * DEFAULT_WEIGHT_KG / 200.0;
    return Math.max(1, (int) Math.round(energy));
  }
}
