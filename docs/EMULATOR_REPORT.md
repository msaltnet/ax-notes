# Android 에뮬레이터 검증 부록

검증일: 2026-10-07. **내부 테스트용 APK의 실행 기록이며, 실기기 검증이나 출시 승인 보고서가 아니다.** 빌드·단위 테스트·정적 검사 결과는 [TEST_REPORT.md](TEST_REPORT.md)를 함께 확인한다.

## 검증한 최종 APK

- 패키지: `net.msalt.axnotes.internal`
- 버전: `0.1.0-internal` / versionCode 1
- 크기: 10,474,638 bytes
- SHA-256: `b67160f935d66c455350ad310efa355df865cc6b4c6dee95d3cbe680654b42b2`
- 설치 후 에뮬레이터의 `base.apk`를 다시 가져와 **동일한 SHA-256**임을 확인했다: [설치본 체크섬](emulator-evidence/api26-installed-sha256.txt)
- 이 문서에 연결한 최종 스크린샷과 회귀 테스트 로그는 모두 위 APK 기준이다

## 환경과 데이터

- 클라우드 Linux의 공식 Android Emulator 37.2.12
- Android 8.0 / API 26, Google APIs x86 이미지 revision 16
- WebView 제공자: Chrome `69.0.3497.100`
- 540 × 960 px, 240 dpi. 최종 재실행은 소프트웨어 CPU 에뮬레이션, 1 CPU, 1 GB RAM, SwiftShader 구성으로 진행했다
- 하드웨어 가속 KVM을 사용하지 않았다. 따라서 설치·시작·렌더링 지연은 실제 기기 성능을 나타내지 않는다
- 앱에 포함된 8개 글의 **내부 테스트용 콘텐츠 스냅샷**과 직접 만든 테스트 메모만 사용했다. 실제 서비스 JSON 피드 배포·연동 결과가 아니다
- [환경 확인 결과](emulator-evidence/api26-final-environment.txt)

## 최종 APK에서 통과한 확인

### 기기 내 자동화 테스트: 3개 모두 통과

[전체 instrumentation 로그](emulator-evidence/api26-lean-instrumentation.txt), 결과 `OK (3 tests)`, 실행 시간 107.471초. 이 시간은 기능 테스트 실행 시간이며 성능 측정값이 아니다.

1. Notes ↔ 내 보관함을 세 번 반복해 이동한 뒤 안정된 화면으로 돌아옴
2. 글에 연결된 메모 편집 중 뒤로 가기 → 저장하지 않은 변경 안내 → 계속 작성 및 변경 버리기
3. 검색어가 없을 때 안내, 특수문자를 포함한 검색의 결과 없음, 검색어 지우기 후 안내 복귀

### 화면·저장 흐름 확인

- **설치와 첫 실행:** 8개 글, 샘플 콘텐츠 표시, 한국어 Notes 화면을 확인했다. [Notes 화면](emulator-evidence/api26-lean-01-notes.png)
- **실제 WebView 본문:** 캐시 HTML의 제목·소제목·한국어 본문이 표시됐다. 텍스트 폴백 화면이 아니다. [본문과 북마크](emulator-evidence/api26-lean-03-bookmarked-reader.png)
- **글 연결 메모 저장:** 제목을 비워 두고 `Cloud emulator test memo persistence`를 명시적 저장 버튼으로 저장했다. 연결된 글 제목도 표시됐다. [저장된 메모](emulator-evidence/api26-lean-07-saved-memo.png)
- **북마크와 메모의 독립성:** 북마크 해제 뒤 북마크 보관함은 비었고, 메모는 남았다. [빈 북마크](emulator-evidence/api26-lean-10-bookmarks.png), [검증 로그](emulator-evidence/api26-lean-save.log)
- **프로세스 재시작 후 보존:** 앱을 force-stop한 뒤 다시 실행해 같은 메모와 글 연결이 유지됨을 확인했다. [재실행 뒤 메모](emulator-evidence/api26-lean-14-persisted-memo.png)
- **메모 통합 검색:** `persistence`를 검색해 해당 메모와 연결 글을 찾았다. [검색 결과](emulator-evidence/api26-lean-16-memo-search.png)
- **콘텐츠 캐시만 삭제:** 북마크를 다시 추가한 뒤 캐시 삭제 확인창에서 진행했다. Notes는 0개가 됐고, 북마크의 제목 스냅샷과 메모는 보존됐다. 삭제 확인창(별도 전달 ZIP의 `android/docs/emulator-evidence/api26-lean-21-cache-confirm.png`), 빈 콘텐츠 캐시(별도 전달 ZIP의 `android/docs/emulator-evidence/api26-lean-23-empty-cache.png`), [보존된 북마크](emulator-evidence/api26-lean-25-preserved-bookmark.png), [보존된 메모](emulator-evidence/api26-lean-26-preserved-memo.png)
- [재시작·검색·캐시 삭제 검증 로그](emulator-evidence/api26-lean-persistence-cache.log)

## 해석상 주의할 점

- **엄격한 네트워크 차단 오프라인 테스트로 계산하지 않는다.** Wi-Fi와 모바일 데이터 비활성화를 요청했지만 최종 확인에서 Wi-Fi는 연결 상태였고 네트워크는 인터넷 연결 검증을 통과하지 못한 상태였다. 모바일 데이터는 꺼져 있었다. 확인한 것은 로컬 샘플/캐시 HTML과 개인 데이터 흐름이며, 실제 피드를 받은 뒤 연결을 완전히 끊는 시나리오는 별도 검증이 필요하다
- 테스트 북마크가 에뮬레이터 재시작과 중간 APK 교체 뒤 남아 있는 것은 확인했다. 정식 버전 간 업데이트, 서명 키 변경, 데이터베이스 마이그레이션을 검증한 것으로 확대 해석하지 않는다
- 초기 AOSP 이미지에는 사용할 수 있는 WebView 제공자가 없어 이전 빌드에서 오류가 발생했다. 구현에는 안내 문구와 캐시된 일반 텍스트 폴백 및 WebView 정리 처리를 추가했다. 최종 APK의 정상 WebView 경로는 통과했으나 **최종 APK의 제공자 부재 경로를 에뮬레이터에서 다시 실행하지는 않았다**
- 첫 부팅 중 앱 설치 전의 System UI ANR와 에뮬레이터 프로세스 종료(exit 137)가 있었다. 원인을 앱 오류로 확정하지 않았다. 더 가벼운 설정으로 환경을 복구하고 위 최종 APK의 3개 회귀 테스트와 저장 흐름을 다시 검증했다

## 아직 실행하지 않은 항목

- API 35 및 Android 13 이상 알림 권한 요청·거부·재허용, 알림 채널 차단
- 실제 예약 알림 전달·탭 이동, 변경·취소 경합, 재부팅·Doze·강제 종료 뒤 일정 복구, 시스템 시각·시간대 변경
- 실기기 설치·사용, 실기기의 같은 서명 버전 업그레이드와 개인 데이터 보존
- 큰 글자/글자 배율 변경, TalkBack, 다크 모드, 회전, 키보드가 열린 상태에서 저장 버튼 배치 등 접근성·레이아웃 검증
- 최종 APK의 WebView 제공자 부재 폴백, 이미지 온라인 로딩, 코드·표의 가로 스크롤, 외부 브라우저·OS 공유창 왕복
- 실제 배포 JSON 피드의 갱신·오류 복구·오프라인 재열기, 1,000개 글/1,000개 메모의 검색 성능
- 기기 수준의 자동 백업·기기 이전 제외 동작과 개인 데이터 전체 삭제

API 35 이미지는 도구 준비 단계에서 내려받았지만 부팅하거나 앱 테스트를 실행하지 않았다. 위 미실행 항목은 통과로 표시하지 않는다. 내부 후보 APK 전달 범위에 맞춰 이 단계에서 추가 기기 매트릭스 검증을 중단했다.

## 소스 PR의 증빙 범위

두 선택적 화면 캡처(`api26-lean-21-cache-confirm.png`, `api26-lean-23-empty-cache.png`)는 GitHub 소스 PR에 포함하지 않았다. 전체 화면 증빙은 사용자에게 전달한 `AX-Notes-MVP-source-and-verification.zip`에 그대로 보존되어 있다. 위 경로는 그 ZIP 내부 경로이며 비공개 다운로드 URL이 아니다. 캐시 삭제와 개인 데이터 보존 결과를 기록한 텍스트 로그 및 최종 APK 체크섬은 이 저장소에도 포함한다. 앱 코드·테스트·의존성·서명 정보는 전달 당시와 같다.
