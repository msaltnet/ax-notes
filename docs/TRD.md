# AX Notes App — TRD

상태: 검토용 초안 · 작성일: 2026-10-07

기준: [README](../README.md), [PRD](PRD.md). TRD는 Technical Requirements Document로 사용한다. PRD 9장의 사용자 결정은 확정 사항이고 Kotlin/Compose 등 세부 구현 기술은 제안이다. 아직 구현하거나 의존성을 설치하지 않았다. SDK·라이브러리 버전은 구현 착수 시 공식 문서와 배포 요구사항을 확인해 고정한다.

## 1. 기술 선택

| 접근 | 장점 | 비용·제약 |
| --- | --- | --- |
| Kotlin + Jetpack Compose | Android 전용 요구와 로컬 저장·알림에 직접 대응 | Android 구현 학습과 본문 표시 설계 필요 |
| Flutter | 향후 다른 플랫폼으로 확장 가능 | 현재 Android only 범위에서는 추가 런타임·플러그인 관리 |
| 웹 중심 WebView 셸 | 기존 사이트 화면 재사용 | 로컬 메모·통합 검색·알림과의 경계가 복잡해짐 |

우선안은 **Kotlin + Compose 기반 네이티브 앱**이다. 탐색·보관함·검색은 네이티브 화면, 글 본문은 앱이 내려받은 HTML을 제한된 WebView로 표시한다. Labs 전용 연결은 MVP 이후 범위다. 플랫폼 선택은 사용자 검토 후 확정한다.

최소 지원은 사용자 결정에 따라 Android 8.0(API 26)으로 확정한다. 초기 배포는 내부 APK다. compileSdk와 targetSdk는 구현 착수 시 안정 SDK와 테스트 기기를 확인해 확정한다. Google Play 적합성 검토는 후속 배포 범위다.

## 2. 구성과 책임

```text
AX Notes Public의 Notes 메타데이터와 Markdown 본문
    → Astro 정적 콘텐츠 export (구현 범위 확정)
    → HTTPS manifest + note detail JSON
    → ContentRepository → Room 콘텐츠 캐시 → 읽기 / 검색 화면

보관함 / 메모 / 알림 화면
    → PersonalRepository → Room 개인 데이터
    → ReminderScheduler → WorkManager (기술 제안) → 로컬 알림

글 본문 → 제한된 WebView
본문의 외부 링크 → Custom Tabs 또는 외부 브라우저
```

- UI: Compose, ViewModel, 상태 스트림. DB·HTTP에 직접 접근하지 않는다.
- ContentRepository: 정적 웹 데이터 검증, 갱신, 로컬 콘텐츠 제공.
- PersonalRepository: 북마크·메모·읽기 알림의 생성·수정·삭제.
- SearchRepository: 로컬 콘텐츠와 개인 메모의 키워드 조회.
- ReminderScheduler: 예약·취소·복구와 알림 수신 처리.
- DataStore: 표시 설정 등 작은 환경설정. 검색 데이터나 메모는 넣지 않는다.
- 최초에는 하나의 앱 모듈에서 책임별 패키지로 나눈다. 다중 모듈·자체 API 서버는 요구가 생길 때 추가한다.

Room을 화면에서 읽는 로컬 기준 데이터로 두는 방향은 [Android offline-first 가이드](https://developer.android.com/topic/architecture/data-layer/offline-first)를 따른다.

## 3. 웹 콘텐츠 계약

### 현재 소스에서 확인한 사실

Public은 Astro + Markdown이다. `src/content.config.ts`에는 Notes와 Project/Series 스키마가 있고, `src/pages/rss.xml.ts`에는 글 제목·설명·날짜·링크가 있다. 현재 RSS에는 글 본문이나 Labs 카탈로그가 없다. Labs 목록은 `src/pages/labs/index.astro`에 정의되어 있다.

따라서 RSS만으로 PRD의 본문 읽기·본문 검색 요구를 충족할 수 없다. HTML DOM 스크래핑은 사이트 레이아웃 변경에 취약하다. 사용자 결정에 따라 **기존 Public 웹 빌드에 정적 JSON 출력을 추가**한다. 별도 실행 서버는 필요하지 않지만 Public 저장소 변경과 웹 배포가 선행 의존성이다. 데이터 제공에 필요한 웹 저장소 변경은 승인된 범위이며, 앱과 웹의 계약 세부 구조는 아래 초안을 검토해 확정한다. 아직 엔드포인트는 존재하지 않는다. Labs 카탈로그는 MVP 이후에 추가한다.

### 제안 인터페이스 v1

배포 기준 URL은 구성값으로 주입하며 운영 도메인은 검증 후 확정한다. 사이트의 base path를 유지한다.

| 상대 경로 | 내용 |
| --- | --- |
| `app/v1/manifest.json` | schemaVersion, generatedAt, author, notes[] |
| `app/v1/notes/{id}.json` | 글 메타데이터, 정제된 bodyHtml, 본문 검색용 bodyText |

- `author`: name, 소개 URL, 공개 채널 링크. 실제 Public 설정을 사용한다.
- Note 요약: id, title, description, publishedAt, updatedAt, canonicalUrl, detailUrl, revision, collectionId(선택), projectUrl(선택).
- Note 상세: 같은 id·revision, bodyHtml, bodyText. 공개 글만 제공하고 draft·Inside 콘텐츠는 제외한다.
- id는 기존 Markdown id를 유지한다. 갱신 시각과 revision은 빌드 메타데이터·콘텐츠 해시 등 일관된 규칙으로 생성한다.
- canonicalUrl·detailUrl·이미지 및 본문 링크는 배포 URL과 base path를 반영한 절대 HTTPS URL이다. 자산을 앱 저장소로 복제하지 않는다.
- 본문의 앱용 HTML은 article 내용만 포함한다. 사이트 탐색·광고·스크립트를 넣지 않고 기존 Markdown 정제 정책에 앱용 허용 태그 규칙을 추가한다.
- manifest는 전체 공개 목록의 스냅샷이다. JSON 스키마, 중복 id, 링크, draft 제외를 Public 빌드에서 검증한다.
- Labs 카탈로그와 relatedLabIds는 MVP 이후 별도 계약 확장으로 설계한다. MVP에서 본문에 포함된 Lab URL은 일반 외부 링크로 처리한다.

배포 원자성: 새 상세 파일을 먼저 제공하고 manifest를 마지막에 갱신한다. 파일마다 revision을 대조한다. CDN 갱신 지연으로 버전이 어긋나면 이전 캐시를 유지하고 재시도한다. 이전 콘텐츠 파일을 잠시 유지하는 배포 정책도 필요하다.

## 4. 갱신과 오프라인

앱 시작과 사용자 새로고침에서 manifest를 받는다. 변경된 상세만 가져오며 상세 화면 열기는 해당 글 다운로드를 우선한다. 첫 버전은 백그라운드 새 글 수집이나 서버 푸시를 요구하지 않는다.

- 네트워크 타임아웃·HTTP 오류·파싱 오류에는 기존 캐시를 보존한다.
- manifest 전체 검증 후 DB 트랜잭션으로 메타데이터를 반영한다. 미지원 schemaVersion은 갱신 실패 상태로 표시한다.
- 상세는 id·revision과 본문을 검증하고 성공한 항목만 반영한다. 일부 실패가 다른 글을 지우지 않는다.
- 다음 실행에서 본문 다운로드를 이어간다. 검색에는 실제 캐시된 본문만 넣고 메타데이터만 있는 글도 제목·요약으로 검색한다.
- 목록에서 사라진 글은 unavailable로 표시한다. 다운로드 실패나 잘못된 manifest를 삭제 신호로 사용하지 않는다.
- 글 북마크·메모·알림은 웹 동기화로 수정하거나 삭제하지 않는다.
- 초기 한도 제안은 공개 글 1,000건, 글 본문 1MiB, manifest 2MiB, 본문 캐시 총 50MiB다. 초과 응답을 거부하거나 다운로드를 중단하고 상태를 표시한다. 한도는 샘플 데이터 측정 후 조정한다.
- 캐시 한도에 도달하면 저장한 글을 우선 보존하되 개인 데이터는 항상 유지한다. 이미지 캐시는 별도이며 오프라인 이미지 완전성을 보장하지 않는다.

## 5. 로컬 데이터 모델

콘텐츠 DB와 개인 DB를 분리해 캐시 초기화가 개인 데이터에 영향을 주지 않도록 한다.

| 테이블 | 핵심 필드와 제약 |
| --- | --- |
| Article (콘텐츠) | id PK, title, description, dates, canonicalUrl, revision, bodyHtml/bodyText nullable, cachedAt, availability |
| SavedItem (개인) | UUID PK, type ARTICLE 또는 MEMO, articleId NOT NULL, titleSnapshot, urlSnapshot, textTitle/textBody nullable, createdAt, updatedAt |
| Reminder (개인) | UUID PK, articleId, titleSnapshot, urlSnapshot, dueAt epoch millis, state, generation, notifiedAt nullable |

- ARTICLE 타입은 articleId당 한 건만 허용한다. MEMO 타입도 articleId를 필수로 가지며 글에 연결된 메모만 생성한다. 연결 대상은 저장 시 공개 글 또는 기존 보관함의 글 스냅샷으로 검증한다.
- MEMO에만 사용자 본문이 있고 ARTICLE에는 본문을 중복 저장하지 않는다. 별도 글 북마크 없이도 연결 메모를 작성할 수 있다.
- 글 하나에 여러 메모를 허용한다. 현재 요구에 없는 외부 링크 타입은 추가하지 않는다.
- Reminder는 articleId당 활성 일정 하나만 허용한다. 일정 변경은 generation을 증가시킨다.
- 개인 DB는 콘텐츠 DB에 삭제 연쇄 외래키를 두지 않는다. 콘텐츠 누락 시 스냅샷으로 화면과 알림을 표시한다.
- DB 버전마다 마이그레이션을 작성한다. 개인 DB에 destructive migration fallback을 사용하지 않는다.
- 캐시 삭제는 콘텐츠 DB만 초기화한다. 개인 데이터 전체 삭제는 알림 취소와 개인 DB 삭제를 함께 처리한다.

## 6. 로컬 검색

MVP는 파라미터 바인딩한 SQLite 부분 일치 조회로 시작한다. 대소문자·주변 공백을 정규화하고 `%`, `_`, escape 문자를 리터럴로 처리한다. 입력 길이는 200자까지로 제한한다.

Article의 title·description·bodyText와 MEMO의 textTitle·textBody를 조회하고 타입별 결과를 통합한다. 메모 연결 글 제목도 스냅샷에서 조회한다. 입력 후 약 250ms debounce, 이전 작업 취소, DB 조회는 UI 스레드 밖에서 수행한다.

정렬은 제목 일치 우선, 동일 그룹에서는 글 발행일 또는 메모 수정일 내림차순이다. 페이징 50건을 기본으로 둔다. 한국어 부분 문자열 검색을 기준으로 검증하며 영어 토큰 기반 FTS만으로 한국어 검색을 충족한다고 가정하지 않는다. 1,000개 글·1,000개 메모 기준 검색 응답 p95 500ms 이하를 측정 목표로 두고 대상 실기기에서 확인한다. 초과할 경우 색인 전략을 다시 선택한다.

## 7. 읽기 화면과 외부 링크

글 본문은 검증한 캐시 HTML과 앱 제공 CSS만 제한된 WebView에 로드한다. JavaScript·파일 접근·content URI 접근·네이티브 JavaScript bridge는 사용하지 않는다. 글 메모와 저장 조작은 네이티브 UI에서 처리한다.

- 외부 이동은 HTTPS 링크만 허용하고 Custom Tabs 또는 브라우저로 연다. `javascript:`, `file:`, 임의 `intent:`는 실행하지 않는다.
- WebView의 다운로드·리다이렉트·새 창 요청도 동일한 URL 정책을 적용한다. 인증서 오류를 무시하지 않는다.
- Custom Tabs가 없으면 외부 브라우저로 폴백하고, 처리 앱이 없으면 안내한다.
- 모바일 글, 코드·표 가로 스크롤, 큰 글자, TalkBack, 다크 모드, 외부 링크 이동과 앱 복귀를 검증한다.

WebView 정책은 [native bridge 위험 가이드](https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges)를 참고했다. 외부 웹사이트의 동작을 위해 앱의 본문 WebView 제한을 풀지 않는다.

## 8. 읽기 알림

사용자가 확정한 정밀도는 **스케줄러 수준**이다. 이를 구현하는 초안으로 WorkManager의 일회성 지연 작업을 제안한다. 예약 시각은 실행 가능한 최소 시점이며 정확한 실행 시각이나 최대 지연 시간을 보장하지 않는다. initial delay와 OS 최적화에 따른 실행 지연은 [공식 작업 요청 가이드](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work)를 참고한다. MVP에 AlarmManager와 exact alarm 권한은 요구하지 않는다.

- 로컬 DB의 dueAt이 일정 기준이다. 입력한 로컬 날짜·시각은 저장 시 epoch millis로 변환하고 시간대 변경 후에도 같은 실제 시점을 유지한다.
- PRD-05의 권한 요청은 최초 예약 시점에 진행한다. Android 13 이상에서는 POST_NOTIFICATIONS를 요청하며 앱 알림과 채널 상태를 확인한다. [알림 권한 문서](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
- Reminder UUID별 unique work를 사용하고 initial delay는 `max(0, dueAt - now)`로 계산한다. 변경은 기존 작업을 교체하고 취소는 DB 상태 변경과 작업 취소를 함께 처리한다. 로컬 알림 작업에는 네트워크 제약을 두지 않는다.
- 알림 클릭용 PendingIntent 식별자는 Reminder UUID로 구분하고 immutable을 사용한다. 알림 탭은 앱 내부 글 id로 이동한다.
- worker는 DB의 state·generation·dueAt을 다시 확인한다. 삭제·취소·이전 generation 작업은 무시한다. dueAt 이전이면 남은 지연으로 재예약한다.
- 전달 claim과 notifiedAt 갱신을 트랜잭션으로 처리하고 동일 notification id를 사용한다. 실패 시 복구 가능한 상태를 보존한다.
- 재부팅 이후 작업 유지에는 WorkManager를 사용하고 실제 기기에서 검증한다. 앱 시작과 시스템 시각 변경 시 DB와 작업 예약을 재조정한다. 강제 종료 등 OS 제한으로 전달할 수 없는 경우는 다음 앱 시작에서 처리한다. 앱이 별도 부팅 예약 receiver를 중복 구현하지 않는다.
- 미래 알림은 재예약하고, 지연된 알림은 한 요약 알림으로 묶는다. 권한 차단 상태이면 OS 알림을 시도하지 않고 앱 보관함에 남긴다.
- 알림 클릭 자체로 글을 읽었다고 단정하지 않는다. 앱의 글 진입 시 재열기 대상 상태를 갱신한다.

콘텐츠 갱신 작업은 별도 범위다. 읽기 알림에 주기적 polling, expedited work 또는 foreground service를 요구하지 않는다.

## 9. 개인 데이터와 네트워크 경계

- 개인 DB·설정은 앱 전용 내부 저장소에 둔다. 메모·검색어·보관함을 서버에 보내지 않는다.
- 사용자 결정에 따라 계정과 백업 기능을 제공하지 않는다. 자체 클라우드 동기화, 분석 SDK, 광고 SDK도 MVP 의존성에서 제외한다.
- 자동 백업·기기 이전으로 개인 데이터가 이동하지 않도록 Android 버전별 백업 규칙을 명시한다. `allowBackup` 한 속성만으로 보장을 단정하지 않고 cloud-backup 및 device-transfer 규칙을 검증한다. [Android 백업 문서](https://developer.android.com/identity/data/autobackup)
- 앱 삭제·초기화 후 복구를 보장하지 않는다. 개인 데이터 내보내기는 별도 설계 항목이다.
- 네트워크는 Public 콘텐츠 수신과 사용자가 여는 웹 링크에 사용된다. 웹 브라우저·Lab의 저장 데이터까지 앱 내부 저장 정책이 통제하지는 않는다.
- 로그에는 메모 본문·검색어를 넣지 않는다. 콘텐츠 검증 실패는 오류 코드와 공개 id 위주로 기록한다.
- 앱 기능에는 INTERNET와 필요한 알림 권한을 선언한다. WorkManager가 병합하는 시스템 권한은 최종 manifest에서 확인한다. 광범위 저장소·위치·주소록 권한은 요구하지 않는다.

## 10. 품질·테스트와 배포

| PRD | 검증 항목 |
| --- | --- |
| 01 | manifest·상세 계약, 하위 경로 링크, 오프라인 읽기, 갱신 실패 시 캐시 유지, draft 제외 |
| 03~04 | 중복 저장 방지, 글 연결 없는 메모 생성 거부, 메모 수정·삭제, 앱 재실행, 콘텐츠 삭제·캐시 초기화 시 글 연결과 개인 데이터 보존 |
| 05 | 권한 거부, 채널 차단, 일정 수정·취소, 중복 worker, 재부팅, 시스템 시각·시간대 변경, 지연 복구 |
| 06 | 한국어 부분 일치, 특수문자, 글·메모 통합 결과, 오프라인, 성능 측정 |
| 07 | 공유 URL, 개인 메모 제외, 작성자 링크 |

필수 기술 검증: DB 마이그레이션에서 개인 데이터 보존, 미지원 계약 버전·잘못된 URL·과대 응답 거부, WebView bridge/JavaScript 비활성화, 개인 DB 자동 백업 제외.

자동화는 repository·검색·일정 상태 전이 단위 테스트, Room 마이그레이션 테스트, 웹 계약 테스트, 주요 화면 instrumentation 테스트로 구성한다. 알림·외부 링크 이동·접근성은 최소 지원 버전 및 최신 안정 버전 실기기/에뮬레이터 조합으로 확인하고 실제 기기 결과를 별도로 남긴다. PRD-02의 Labs 전용 실행과 다운로드 검증은 MVP 이후에 진행한다.

성능 초안: 1,000개 글·1,000개 메모에서 검색 p95 500ms 이하, 캐시 목록 표시 1초 이내. 측정 기기·빌드 모드·데이터 조건을 기록하고 측정 전 달성한 것으로 표현하지 않는다.

초기 배포는 사용자 결정에 따라 내부 APK로 확정한다. CI는 빌드·정적 검사·단위 테스트를 실행한다. 내부 배포용 APK의 버전·서명과 전달 경로를 기록하고, 동일 서명으로 업그레이드해 개인 데이터가 유지되는지 확인한다. 서명 키는 저장소에 넣지 않는다. 앱 id·최종 SDK·APK 전달 경로는 구현 계획에서 정하고 Google Play 배포·스토어 자료·광고는 후속으로 검토한다.

## 11. 의존성과 결정 순서

1. 확정 사항 반영: 글 연결 메모만, 계정·백업 없음, 스케줄러 수준 알림, Android 8.0 이상, 정적 JSON 및 필요한 웹 변경, 내부 APK 배포.
2. TRD 세부 검토: Kotlin/Compose, WorkManager 등 구현 기술과 정적 JSON 계약 구조 확정.
3. Public 데이터 제공 작업과 앱 구현 작업의 변경 범위·구현 계획 작성.
4. 웹 계약 샘플 → 로컬 저장·읽기 → 메모·검색 → 알림 → 공유·외부 링크 이동을 검증.
5. 내부 APK 배포와 실사용 피드백 수집. 후속 공개 배포 여부는 별도로 결정.

이 문서는 실행 계획을 대체하지 않는다. 현재는 웹 엔드포인트·앱 코드·CI·테스트가 구현되지 않은 상태다.

MVP 이후 Labs 연결을 도입할 때 카탈로그·관련 Lab 메타데이터, 로컬 캐시 모델, 전용 탐색·실행 진입점과 복사·다운로드·앱 복귀 검증을 함께 설계한다. 앱 개인 DB를 Lab에 자동 전달하지 않는다.

## 12. 변경 기록

- 2026-10-07: 현재 Public 소스 확인과 Android 공식 문서를 바탕으로 첫 TRD 초안 작성.
- 2026-10-07: 사용자 결정에 따라 Labs 카탈로그·데이터 모델·전용 실행·검증을 MVP 이후로 이동.
- 2026-10-07: PRD의 여섯 확정 사항을 데이터 모델·웹 연동·지원 버전·배포에 반영하고, 스케줄러 수준 알림의 구현안을 WorkManager로 수정.
