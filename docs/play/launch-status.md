# Play 출시 준비 상태 — 2026-10-24

## 공개 문의 이메일 변경

- 공개 문의 주소를 `msalt.net.help@gmail.com`으로 변경. Play Store 연락처는 ‘저장 및 출시’로 적용했고 API로 확인.
- 한국어·영어 상세 소개문의 문의 주소도 변경·저장 후 API로 확인. 이 소개문 변경은 검토 전송 준비 상태이며 이번 작업에서는 전송하지 않음.
- 작업 시작 시 앱의 기존 변경사항은 이미 ‘검토 중’으로 표시됨. 아래 초안·미제출 기록은 이전 준비 단계의 상태임.
- 공개 개인정보처리방침도 새 주소로 배포(커밋 `34a19051c02f865a9a06f6fd7c40d3fcbd465bb0`, Pages run `40086346761` 성공). 실제 HTTPS 응답에서 새 주소 및 이전 주소 제거 확인.
- 베타 모집 글은 새 주소를 사용하며 `draft: true` 유지. 로그인 계정·서비스 계정 인증 정보는 변경하지 않음.

## Console 저장 완료

- 앱 `net.msalt.axnotes`, 개발자 계정 `5356485458671903294`.
- 한국어와 영어 소개문: `store-info.txt`와 API 대조 일치 확인.
- 아이콘 512×512, 그래픽 1024×500, 실제 출시 앱의 1080×1920 스크린샷 2장 업로드.
- 공개 연락처 `msalt.net.help@gmail.com`, 웹사이트 `https://ax.msalt.net`.
- 카테고리 뉴스/잡지. 로그인·광고·정부·금융·건강 기능 없음.
- 타겟층 13~15세, 16~17세, 18세 이상.
- IARC 설문 저장: 한국 3세 이상, 북미 10세 이상. 공개 글의 가벼운 비속어와 약물에 대한 텍스트 언급을 반영했으며, 해당 콘텐츠가 중심 내용은 아님.
- 개인정보처리방침 URL과 데이터 보안 신고 저장 완료. 대시보드의 앱 설정 미완료 항목이 사라지고 비공개 테스트 설정이 활성화됨.
- 게시 개요에서 두 변경사항이 ‘아직 검토를 위해 제출되지 않음’ 영역에 있는 것을 확인.
- 추가 제출 차단 항목 처리: 광고 ID 미사용 선언 저장(출시 병합 매니페스트 AD_ID 권한 없음, 광고 ID SDK/호출 없음 확인).
- 뉴스 및 잡지 앱 선언 저장: 개발자 계정 맛소금, 공개 연락처 URL `https://ax.msalt.net/app-privacy/`, 개인 기술 미디어, 자체 콘텐츠(애그리게이터 아님), 교육 및 연구·과학 및 기술. 전용 YouTube 및 저널리즘 가이드라인·단체·수상 경력 없음으로 입력.
- 두 선언 저장 후 ‘문제 2개 보기’가 사라지고 ‘검토를 위해 변경사항 13개 제출’ 버튼이 활성화됨. Google 빠른 자동 검사는 재실행 중이며 심사 제출 버튼은 누르지 않음. UI 증거 `app/build/play-assets/play-declarations-completed.png`.
- 데이터 보안: 호스팅 IP 보안 로그를 기기 또는 기타 ID 수집·공유로 신고. 목적은 앱 기능 및 사기 예방·보안·규정 준수, 필수 수집, 비임시 처리. HTTPS 전송, 계정 생성·로그인 없음. 호스팅 로그의 삭제를 운영자가 보장할 수 없어 삭제 요청 지원은 아니요로 신고. 개인 기록은 기기 내에만 저장.

## 비공개 테스트 초안

- Console 트랙: `AX Notes Beta`, 트랙 페이지 ID `4699208920985105711`.
- API 트랙 이름도 `AX Notes Beta`. 예약된 `beta` 트랙과 구별한다.
- 릴리스 `0.3.0 Closed Beta`, versionCode `4`만 포함, 상태 `draft`를 API로 확인. 코드 3은 초안에서 제외했으며 라이브러리에는 보존됨.
- 업로드된 AAB SHA-256: `748880bc7bcda7045930e35381b3ebed297aac9553cf845d308bd3215ff6e7c2` (로컬 빌드와 API 일치).
- 첫 테스트 배포 국가: 대한민국. 테스터는 모집 예정이며 현재 0명.
- 심사 제출·테스터 배포·프로덕션 신청은 하지 않음. 14일 요건은 아직 시작되지 않음.
- 예비 미리보기 경고: 테스터 미지정, R8 가독화 파일 없음(난독화 비활성), 의존성 네이티브 코드 디버그 심볼 없음.

## 코드 4 검증 및 방침 배포

- 앱 설정에 `https://ax.msalt.net/app-privacy/` 개인정보처리방침 링크 추가.
- 이미 코드 3을 업로드했으므로 후속 빌드의 versionCode는 4, versionName은 0.3.0.
- 테스트 181개/23개 스위트, 실패·오류 0; release lint 오류 0/경고 22.
- `testDebugUnitTest lintRelease bundleRelease` 성공, AAB 서명 검증 성공.
- 코드 4 AAB SHA-256: `748880bc7bcda7045930e35381b3ebed297aac9553cf845d308bd3215ff6e7c2`.
- 웹 방침 소스: 인접 `ax-notes-public/src/pages/app-privacy.astro`. Astro check와 static build 성공, 웹 테스트 50개 통과.
- 공개 URL `https://ax.msalt.net/app-privacy/`: HTTP 200 및 렌더링 확인.
- 공개 저장소 커밋 `d764baab52c61d6c8baa3dec32996708bcc3acfb`, GitHub Pages Actions run `40071287533` 성공.
- GitHub Pages의 방문 IP 보안 로그를 정책과 데이터 보안 신고에 반영.
- UI 증거: `app/build/play-assets/play-code4-draft-saved.png`, `play-beta-code4-preview.png`, `play-changes-not-submitted.png`.

## 다음 단계

1. 모집한 테스터 이메일 목록 또는 Google 그룹 등록.
2. 베타 미리보기 경고를 확인하고 별도 승인으로 Google 심사 제출 및 테스트 게시.
3. 실제 12명 이상이 연속 14일 참여한 뒤 피드백·개선 증거를 바탕으로 프로덕션 액세스 신청.

## API 주의

이 앱의 listing commit에 `changesNotSentForReview=true`를 지정하면
"Changes are sent for review automatically" 오류가 발생했다. 오류 편집은 폐기했으며,
승인 범위를 넘는 자동 심사 전송 재시도 없이 Console에서 저장하고 API로 검증했다.
정확한 custom track을 지원하지 않는 helper의 `--track beta`로 비공개 테스트를 대체하지 않는다.
