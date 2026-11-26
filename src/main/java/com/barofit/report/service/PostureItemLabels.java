package com.barofit.report.service;

import java.util.Map;

/** Korean labels shared with the pose set-report catalog. */
final class PostureItemLabels {
  private PostureItemLabels() {}

  private static final Map<String, String> LABELS =
      Map.ofEntries(
          Map.entry("arm_extension", "팔 펴기"),
          Map.entry("arm_raise", "팔 벌림 높이"),
          Map.entry("axial_rotation", "몸통 회전"),
          Map.entry("body_line", "몸통 정렬"),
          Map.entry("body_movement", "몸통 안정성"),
          Map.entry("chest", "상체 이동 범위"),
          Map.entry("coordination", "팔·다리 협응"),
          Map.entry("depth", "동작 깊이"),
          Map.entry("elbow", "팔꿈치 굽힘"),
          Map.entry("elbow_flare", "팔꿈치 벌어짐"),
          Map.entry("foot_position", "발 위치"),
          Map.entry("form", "자세 평가"),
          Map.entry("hand", "손 위치"),
          Map.entry("head_path", "머리 이동 경로"),
          Map.entry("hip", "골반 정렬"),
          Map.entry("hip_alignment", "골반 정렬"),
          Map.entry("hip_extension", "골반 올리기"),
          Map.entry("hip_height", "골반 높이"),
          Map.entry("knee", "무릎 펴기"),
          Map.entry("knee_extension", "무릎 펴기"),
          Map.entry("knee_tracking", "무릎 진행 방향"),
          Map.entry("left_arm_high", "왼쪽 팔 높이"),
          Map.entry("left_arm_low", "왼쪽 팔 처짐"),
          Map.entry("left_elbow_bent", "왼쪽 팔꿈치 펴기"),
          Map.entry("left_hand_alignment", "왼쪽 지지 손 정렬"),
          Map.entry("left_knee_bent", "왼쪽 무릎 펴기"),
          Map.entry("left_leg_high", "왼쪽 다리 높이"),
          Map.entry("left_support_knee_alignment", "왼쪽 지지 무릎 정렬"),
          Map.entry("leg_open", "다리 벌림"),
          Map.entry("leg_spread", "다리 벌어짐"),
          Map.entry("lowering_control", "내려오는 속도 조절"),
          Map.entry("lumbar_contact", "허리 바닥 접촉"),
          Map.entry("lumbar_neutral", "허리 중립"),
          Map.entry("movement_control", "동작 속도 조절"),
          Map.entry("opposite_arm", "반대쪽 팔 협응"),
          Map.entry("pike_shape", "파이크 자세"),
          Map.entry("press_return", "밀어 올리고 돌아오는 자세"),
          Map.entry("range", "동작 범위"),
          Map.entry("return", "시작 자세 복귀"),
          Map.entry("right_arm_high", "오른쪽 팔 높이"),
          Map.entry("right_arm_low", "오른쪽 팔 처짐"),
          Map.entry("right_elbow_bent", "오른쪽 팔꿈치 펴기"),
          Map.entry("right_hand_alignment", "오른쪽 지지 손 정렬"),
          Map.entry("right_knee_bent", "오른쪽 무릎 펴기"),
          Map.entry("right_leg_high", "오른쪽 다리 높이"),
          Map.entry("right_support_knee_alignment", "오른쪽 지지 무릎 정렬"),
          Map.entry("same_side_pair", "반대쪽 팔·다리 협응"),
          Map.entry("shoulder", "어깨·팔꿈치 정렬"),
          Map.entry("shoulder_shrug", "어깨 으쓱임"),
          Map.entry("toe_loading", "발끝 지지"),
          Map.entry("torso", "몸통 기울기"),
          Map.entry("trunk", "몸통 기울기"));

  static String label(String id) {
    return id == null ? "자세 평가 항목" : LABELS.getOrDefault(id, "자세 평가 항목");
  }
}
