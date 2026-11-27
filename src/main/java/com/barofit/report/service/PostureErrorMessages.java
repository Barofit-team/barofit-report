package com.barofit.report.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Maps pose error codes from barofit-pose to user-facing Korean feedback messages. */
final class PostureErrorMessages {
  private static final int MAX_MESSAGES = 5;

  private PostureErrorMessages() {}

  private static final Map<String, String> MESSAGES =
      Map.ofEntries(
          // Pushup / Plank / Side Plank
          Map.entry("HIP_DROP", "골반이 아래로 처지지 않도록 몸통을 곧게 유지해 주세요."),
          Map.entry("HIP_RAISE", "골반이 너무 올라가지 않게 몸을 일직선으로 맞춰 주세요."),
          Map.entry("ELBOW_SHALLOW", "팔꿈치를 조금 더 깊게 굽혀 내려가 주세요."),
          Map.entry("CHEST_RANGE_LOW", "가슴이 바닥에 더 가까워지도록 가동 범위를 넓혀 주세요."),
          Map.entry("HAND_TOWARD_HEAD", "손 위치가 머리 쪽으로 치우쳤어요. 어깨 아래에 두어 주세요."),
          Map.entry("HAND_TOWARD_HIP", "손 위치가 골반 쪽으로 내려갔어요. 어깨 라인에 맞춰 주세요."),
          Map.entry("RETURN_INCOMPLETE", "시작 자세까지 끝까지 밀어 올려 복귀해 주세요."),
          Map.entry("SHOULDER_ELBOW_OFFSET", "어깨 바로 아래에 팔꿈치가 오도록 정렬해 주세요."),
          Map.entry("KNEE_BENT", "무릎이 구부러지지 않도록 다리를 곧게 펴 주세요."),
          // Squat / Cycles lower body
          Map.entry("SQUAT_SHALLOW", "내려가는 깊이가 얕습니다. 의자에 앉는 느낌으로 끝까지 내려가 주세요."),
          Map.entry("TRUNK_LEAN", "상체가 앞으로 쏠렸어요. 가슴을 펴고 균형을 잡아 주세요."),
          Map.entry("LUNGE_RANGE", "내려가는 깊이가 얕습니다. 무릎을 조금 더 굽혀 내려가 주세요."),
          Map.entry("LUNGE_TRUNK", "상체가 앞으로 쏠렸어요. 허리를 세우고 중심을 유지해 주세요."),
          Map.entry("SPLIT_SQUAT_RANGE", "내려가는 깊이가 얕습니다. 앞뒤 다리에 체중을 실어 더 내려가 주세요."),
          Map.entry("SPLIT_SQUAT_TRUNK", "상체가 앞으로 기울었어요. 몸통을 곧게 세워 주세요."),
          Map.entry("BULGARIAN_SPLIT_SQUAT_RANGE", "내려가는 깊이가 얕습니다. 의자에 앉는 느낌으로 끝까지 내려가 주세요."),
          Map.entry("BULGARIAN_SPLIT_SQUAT_TRUNK", "상체가 앞으로 쏠렸어요. 허리를 조금 더 펴 주세요."),
          Map.entry("STANDING_CALF_RAISE_RANGE", "발뒤꿈치를 조금 더 높이 들어 올려 주세요."),
          Map.entry("STANDING_CALF_RAISE_KNEE_EXTENSION", "무릎을 곧게 편 상태에서 종아리 힘으로 올려 주세요."),
          Map.entry("SIDE_LYING_LEG_RAISE_RANGE", "다리를 조금 더 높이 들어 올려 주세요."),
          Map.entry("SIDE_LYING_LEG_RAISE_KNEE_EXTENSION", "무릎이 구부러지지 않게 다리를 길게 뻗어 올려 주세요."),
          // Incline Pushup / Dead Bug
          Map.entry("INCLINE_PUSHUP_RANGE", "팔꿈치를 더 굽혀 상체를 충분히 내려 주세요."),
          Map.entry("INCLINE_PUSHUP_BODY_LINE", "머리부터 발끝까지 몸통이 일직선이 되도록 유지해 주세요."),
          Map.entry("DEAD_BUG_RANGE", "다리를 조금 더 넓게 뻗어 가동 범위를 확보해 주세요."),
          Map.entry("DEAD_BUG_OPPOSITE_ARM", "반대쪽 팔도 함께 끝까지 뻗어 균형을 맞춰 주세요."),
          // Jumping Jack
          Map.entry("ARMS_LOW", "팔을 머리 위까지 더 높이 벌려 올려 주세요."),
          Map.entry("LEGS_NARROW", "다리를 어깨너비보다 조금 더 넓게 벌려 주세요."),
          Map.entry("ARMS_LEGS_ASYNC", "팔과 다리의 움직임 박자를 함께 맞춰 주세요."),
          Map.entry("TORSO_LEAN", "점프할 때 상체가 흔들리지 않도록 몸통을 곧게 세워 주세요."),
          // Glute Bridge
          Map.entry("HIP_LIFT_LOW", "골반을 조금 더 높이 들어 올려 엉덩이에 힘을 주세요."),
          Map.entry("FOOT_POSITION_OFFSET", "발 위치를 엉덩이 가까이 편안한 너비로 맞춰 주세요."),
          Map.entry("LOWERING_FAST", "내려올 때 천천히 버티며 속도를 조절해 주세요."),
          // Superman
          Map.entry("ELBOW_BENT", "팔꿈치가 구부러지지 않도록 팔을 길게 뻗어 주세요."),
          Map.entry("MOVEMENT_FAST", "반동 없이 천천히 들어 올리고 버텨 주세요."),
          // Pike Pushup
          Map.entry("PIKE_HIPS_LOW", "엉덩이를 더 높이 들어 올려 몸을 ㅅ자 형태로 유지해 주세요."),
          Map.entry("HEAD_PATH_BACK", "머리가 손 앞쪽 대각선 방향으로 내려가도록 경로를 맞춰 주세요."),
          Map.entry("PRESS_OUT", "팔을 끝까지 밀어 올려 시작 자세로 돌아와 주세요."),
          // Mountain Climber
          Map.entry("HIPS_HIGH", "엉덩이가 너무 들리지 않도록 플랭크 높이를 유지해 주세요."),
          // Bird Dog (left / right / stability)
          Map.entry("LEFT_LEG_HIGH", "왼쪽 다리가 너무 높이 올라갔어요. 골반 높이에 맞춰 뻗어 주세요."),
          Map.entry("RIGHT_LEG_HIGH", "오른쪽 다리가 너무 높이 올라갔어요. 골반 높이에 맞춰 뻗어 주세요."),
          Map.entry("LEFT_KNEE_BENT", "왼쪽 다리를 뻗을 때 무릎을 곧게 펴 주세요."),
          Map.entry("RIGHT_KNEE_BENT", "오른쪽 다리를 뻗을 때 무릎을 곧게 펴 주세요."),
          Map.entry("LEFT_ARM_HIGH", "왼쪽 팔이 너무 높이 올라갔어요. 어깨 높이에 맞춰 주세요."),
          Map.entry("RIGHT_ARM_HIGH", "오른쪽 팔이 너무 높이 올라갔어요. 어깨 높이에 맞춰 주세요."),
          Map.entry("LEFT_ARM_LOW", "왼쪽 팔을 어깨 높이까지 조금 더 들어 올려 주세요."),
          Map.entry("RIGHT_ARM_LOW", "오른쪽 팔을 어깨 높이까지 조금 더 들어 올려 주세요."),
          Map.entry("LEFT_ELBOW_BENT", "왼쪽 팔꿈치를 곧게 펴서 앞으로 뻗어 주세요."),
          Map.entry("RIGHT_ELBOW_BENT", "오른쪽 팔꿈치를 곧게 펴서 앞으로 뻗어 주세요."),
          Map.entry("LEFT_HAND_ALIGNMENT", "바닥을 짚은 왼손이 어깨 아래에 오도록 맞춰 주세요."),
          Map.entry("RIGHT_HAND_ALIGNMENT", "바닥을 짚은 오른손이 어깨 아래에 오도록 맞춰 주세요."),
          Map.entry("LEFT_SUPPORT_KNEE_ALIGNMENT", "지지하는 왼쪽 무릎이 골반 아래에 오도록 정렬해 주세요."),
          Map.entry("RIGHT_SUPPORT_KNEE_ALIGNMENT", "지지하는 오른쪽 무릎이 골반 아래에 오도록 정렬해 주세요."),
          Map.entry("SAME_SIDE_PAIR", "같은 쪽이 아닌 반대쪽 팔과 다리를 함께 뻗어 주세요."),
          Map.entry("BODY_MOVEMENT", "동작 중 몸통이 흔들리지 않도록 코어에 힘을 유지해 주세요."),
          // Legacy / Workout catalog codes
          Map.entry("ELBOW_FLARE", "팔꿈치가 바깥으로 벌어지지 않게 몸통 가까이에 유지해 주세요."),
          Map.entry("SHALLOW_RANGE", "가능한 범위에서 조금 더 깊게 움직여 주세요."),
          Map.entry("KNEE_IN", "무릎이 안쪽으로 모이지 않도록 발끝 방향에 맞춰 주세요."));

  static String message(String code) {
    if (code == null || code.isBlank() || "GOOD_FORM".equals(code)) {
      return null;
    }
    return MESSAGES.get(code);
  }

  static List<String> buildFeedbackMessages(
      Map<String, Long> errorCounts, List<GeneratedReportService.Item> postureItems) {
    Set<String> result = new LinkedHashSet<>();

    if (errorCounts != null && !errorCounts.isEmpty()) {
      errorCounts.entrySet().stream()
          .filter(e -> e.getValue() != null && e.getValue() > 0)
          .sorted(
              Comparator.<Map.Entry<String, Long>>comparingLong(Map.Entry::getValue)
                  .reversed()
                  .thenComparing(Map.Entry::getKey))
          .forEach(entry -> {
            String msg = message(entry.getKey());
            if (msg != null && result.size() < MAX_MESSAGES) {
              result.add(msg);
            }
          });
    }

    if (result.isEmpty() && postureItems != null) {
      postureItems.stream()
          .filter(item -> item.warning() > 0)
          .sorted(
              Comparator.comparingDouble(
                  item -> item.compliancePct() == null ? 100.0 : item.compliancePct()))
          .forEach(item -> {
            if (result.size() < MAX_MESSAGES) {
              result.add(PostureItemLabels.label(item.itemId()) + " 자세를 조금 더 신경 써 주세요.");
            }
          });
    }

    return new ArrayList<>(result);
  }
}
