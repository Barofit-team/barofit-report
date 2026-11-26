# 23종 자세 평가 항목명 보완

GeneratedReportService의 4개 항목 한글 매핑을 pose 공통 라벨 카탈로그 전체로 확장합니다.
서버 응답 구조·점수 계산·원시 운동 기록은 바꾸지 않습니다. strengths/improvements/nextGoal을
새로 생성할 때 공통 한국어 이름을 사용하고 미래 미등록 항목은 원시 id를 문장에 노출하지 않습니다.

검증용 카탈로그 스냅샷은 src/test/resources/pose-item-labels.json에 있습니다. 해당 파일의
원본은 Barofit-team/barofit-pose의 schemas/posture-item-labels.json입니다. 동기화 검사와
신규 항목을 사용하는 실제 generate 경로의 단위 테스트를 추가했습니다.

생성 완료 리포트는 기존 정책대로 불변입니다. 이 수정이 이미 저장된 리포트 문장을
소급해서 다시 쓰지는 않습니다.

현재 에이전트 환경에서는 Java 컴파일러(javac), Docker, minikube를 사용할 수 없으며 Gradle
배포 다운로드도 실패했습니다. ./gradlew spotlessApply test integrationTest spotlessCheck를
시도했으나 실행 기반이 준비되지 않아 통과 여부를 확인하지 못했습니다. 여러 레포 변경에 대한
infra E2E도 실행할 수 없습니다. 테스트·포맷 검증 전에는 이 변경을 병합하지 않습니다.

GitHub 이슈 생성과 Git tree 쓰기 모두 연동 권한 오류(403)로 실패했습니다. 따라서 원격 PR 대신 적용 가능한 패치로 공유합니다.
