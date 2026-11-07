# Hephaestus — 정비 기록 코파일럿

## 제출 정보

### ① 앱 이름 · 한 줄 소개

**Hephaestus(헤파이스토스)** — 정비소에서 출차할 때 "어떤 정비 받으셨나요?"라고 먼저 묻고 **그 시점 주행거리를 자동으로 붙여** 기록한 뒤, 주행거리 기준으로 다음 교체 시기를 알려주는 Pleos Connect 차계부 앱 (내연기관·전기차 모드).

- 시연 영상: [docs/demo/hephaestus_demo.mp4](docs/demo/hephaestus_demo.mp4) (2분 4초, 1920×1080) · 대본: [docs/demo/video-script.md](docs/demo/video-script.md)
- 화면: [docs/screenshots/](docs/screenshots/)

### ② 실행 방법

```bash
# 1) Pleos Connect 에뮬레이터를 켠다 (아래 ③ 참고). adb 시리얼은 환경에 맞게 바꾼다.
adb -s emulator-5554 install -r -g apk/김지원_Hephaestus_release.apk
adb -s emulator-5554 shell am start -n com.zigae.hephaestus/.MainActivity
```

- 첫 실행: 차종(전기차 / 내연기관)을 고른다. 전기차 에뮬레이터는 배터리 용량으로 전기차를 추천한다.
- 앱을 한 번 열면 감지 서비스(포그라운드)가 시작되고, 이후 재부팅 때도 자동으로 올라온다.
- 소스에서 빌드하려면: `export JAVA_HOME=<JDK 17>` 후 `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (Android SDK 디버그 키로 서명, 별도 키스토어 불필요).

### ③ 테스트 환경 · 필요한 설정

| 항목 | 내용 |
| --- | --- |
| 에뮬레이터 | **Pleos Connect v2.1.0** (Android 14, `system-images;android-34;pleos_car_emulator;arm64-v8a`), macOS Apple Silicon에서 검증. `-no-audio`로 실행 권장(오디오를 켜면 부팅 직후 멈추는 경우가 있음) |
| 실행 예 | `emulator -avd <AVD 이름> -no-snapshot -no-boot-anim -no-metrics -no-audio` |
| SDK | Vehicle 2.3.2 (조회만), NaviHelper 3.0.5 — 모두 Playground에서 사용 가능한 범위. 차량 **제어 권한은 쓰지 않는다** |
| 권한 | 알림, `pleos.car.permission.CAR_INFO`(차종 추정), `pleos.car.permission.PRIVATE_CAR_SAFETY`(경고등). 모두 normal 등급이라 별도 허용 절차 없음 |
| 외부 API | 정비소 판별용 장소 검색. **카카오 키가 없어도 동작**한다 — 저장소 루트 `secrets.properties`에 `KAKAO_REST_KEY=...`가 없으면 OpenStreetMap(Overpass)으로 검색한다. 키 없이는 "내 정비소"·내비 목적지·설정 > 개발용 데모로 정비소를 판별한다 |
| 주행거리 | 에뮬레이터 주행거리는 항상 0 → 설정 > 개발용 패널의 "주행거리 보정(+100 / +1,000 / +5,000 / +50,000km)"으로 흉내 낸다 |

**Virtual Console로 차량 상태 조작** (에뮬레이터 안의 차량 시뮬레이터, 포트 8088)

```bash
adb -s emulator-5554 forward tcp:8088 tcp:8088
open http://127.0.0.1:8088            # 웹 페이지를 열어 두어야 아래 명령이 반영된다
curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Park"      # 정차 (Park | Drive)
curl -X PUT "http://127.0.0.1:8088/vehicle/gear?gear=Drive"     # 출차 → 정비소였다면 기록 질문
curl -X PUT "http://127.0.0.1:8088/vehicle/telltales/simulator?type=LowVoltBattery&state=On&color=Red"   # 12V 경고등 켬 (color 필수)
curl -X PUT "http://127.0.0.1:8088/vehicle/telltales/simulator?type=LowVoltBattery&state=Off&color=Red"  # 경고등 끔 → "교체하셨나요?" 질문
curl -X PUT "http://127.0.0.1:8088/vehicle/ev-battery?field=charge_port_open&value=true"                 # 충전 정차(묻지 않음)
```

**시동 OFF → ON** (정비소에 맡겼다가 다시 시동 거는 상황)

```bash
adb -s emulator-5554 shell cmd car_service suspend --simulate --skip-garagemode --wakeup-after 8
```

**설정 > 개발용 · 데모와 진단** (앱 안에서 펼침)

| 도구 | 용도 |
| --- | --- |
| 주행거리 보정 | 교체 시기 계산용 주행거리를 더한다 |
| 현재 위치 카센터를 목적지로 | 내비 목적지를 "시흥 오토 카센터"로 설정 → 기어 P → D 하면 출차 질문 |
| 방문 질문 바로 만들기 | 정비소 판별 없이 질문 카드·알림을 바로 띄운다 |
| 차량 신호 흉내 | 에뮬레이터가 지원하지 않는 엔진오일·브레이크액·변속기 오일 레벨 신호를 켜고 끈다. 끄면 "레벨이 정상으로 돌아왔어요" 질문 |
| 진단 | 신호 지원 여부, 켜진 경고등, 기어, 내비 연결, 정비소 판별 결과 |

자세한 데모 순서는 [9장 데모 순서](#데모-순서-virtual-console--앱-설정--데모), 영상 대본은 [docs/demo/video-script.md](docs/demo/video-script.md).

### ④ APK 위치

[`apk/김지원_Hephaestus_release.apk`](apk/김지원_Hephaestus_release.apk) — release 빌드, versionName 1.0.0

---

> **이름의 유래**: 헤파이스토스(Hephaestus) — 대장장이와 기술의 신. 신들의 무기와 수레를 만들고 고치던 **정비공**. 차를 손보는 순간을 놓치지 않고 기록한다는 의미.

| 항목 | 내용 |
| --- | --- |
| 한 줄 소개 | 정비소에 다녀오면 앱이 먼저 "어떤 정비 받으셨나요?"라고 묻고, **그 시점 주행거리를 자동으로 붙여** 기록한 뒤, 다음 교체 시기를 주행거리 기준으로 알려주는 차계부 앱 |
| 대상 | B2C — 블루핸즈가 아닌 동네 카센터·체인 정비소를 이용하는 운전자, 소모품 교체 시기를 기억하지 못하는 운전자 |
| 원조 모델 | 차계부 앱(마이클, 카닥 등), 마이현대 정비 이력(블루핸즈 한정) |
| 사용 맥락 | 정비소에서 출차하기 직전(정차) + 주행 전후 리마인드(정차) |
| 카테고리 | **모빌리티 서비스** — 가이드의 "주행과 밀접한 차량 관리 서비스"에 해당 |
| 상태 | MVP 구현·에뮬레이터 검증 완료 (2026-11-01). 공모전 제출용 — 9장 참고 |

---

## 0. 개발 제약 (2026-11-01 Playground 확인)

| 사용 가능 ✅ | 사용 불가 ❌ |
| --- | --- |
| Vehicle State 조회 (Odometer 주행거리, Safety 엔진오일 레벨 경고, 주행 상태·브레이크) | Vehicle Control (전부 준비 중) |
| NaviHelper (목적지 도착 이벤트, 목적지 POI, 현재 위치) | TTS / STT / FusedLocation / LLM SDK |
| 외부 API (지도 장소 검색으로 정비소 판별) | — |

→ 이 앱은 **제어 권한이 전혀 필요 없음** (조회 + 기록 + 안내만). Hestia 이전 기획이 막힌 원인(Vehicle Control)에 해당하지 않는다.

## 1. 문제 정의 (Pain Points)

| 대상 | Pain Point | 현재 대응 |
| --- | --- | --- |
| 운전자 | 엔진오일·타이어·브레이크 패드 등 교체 시기를 기억 못 함. 기록하려 해도 **그때 주행거리**를 따로 적어야 해서 귀찮음 | 영수증 보관, 엔진룸 스티커, 기억 |
| 운전자 | 차계부 앱은 "내가 먼저 열어서 입력"해야 해서 몇 번 쓰다 방치 | 방치 |
| 동네 카센터 이용자 | 마이현대 정비 이력은 블루핸즈 방문만 자동 기록 | 없음 |

핵심 통찰: **기록의 귀찮음은 "기억해서 앱을 여는 것"과 "주행거리를 확인해 적는 것"** 두 가지다. 차는 정비소에 있었다는 사실과 주행거리를 둘 다 알고 있다.

## 2. 핵심 기능

### F1. 정비소 방문 감지 ★차량특화
- **경로 A (확실)**: NaviHelper `onDestinationArrived()` → 목적지 `poiName`/`address`가 정비소인지 판별.
- **경로 B (일반)**: 정차(기어 P·차속 0) 시 NaviHelper `getCurrentLocationInfo()` 좌표 → 외부 장소 검색 API(카카오/네이버)로 반경 내 정비소 여부 확인.
- 오탐 방지: 좁은 반경 + 일정 시간 이상 체류 + "아니요" 응답한 장소는 이후 제외(학습).

### F2. 출차 시점 기록 유도
- **시점을 "주차했을 때"가 아니라 "정비소에서 다시 시동을 걸었을 때"로 잡는다.**
  - 정비소에 맡기면 보통 시동을 끄므로 헤드유닛·앱도 꺼진다 → 주차 중에는 알림 불가.
  - 출차 직전이 기억이 가장 정확하고, 아직 정차 상태라 주행 규제에도 걸리지 않음.
  - 판정 근거: 시동 ON 위치가 직전 시동 OFF 위치와 같은 정비소 + 주행거리 변화가 거의 없음.
- **노출 방식 (결정 2026-11-01)**: 헤즈업 배너에 의존하지 않고, 되는 방식을 아무거나 쓴다. 기본은 알림 센터 알림 + 앱을 열면 보이는 카드. 미니 앱(AppWidget)은 필요하면 추가.
- 카드 내용: "○○카센터에서 출발하시네요. 어떤 정비를 받으셨나요?"
  - [엔진오일] [타이어] [브레이크] [기타] [정비 안 받음] — **터치 한두 번으로 끝**.
- 주행 시작 시 카드 자동 닫힘, 미응답 건은 다음 정차 때 한 번 더 묻기.

### F2-1. 차량 신호 기반 기록 유도 (2026-11-07 추가) ★차량특화
- Vehicle SDK에는 소모품 교체 주기·잔여 수명이 없고 **레벨 상태**(엔진오일·브레이크액·변속기 오일: `OilLevelState`)와 **경고등**(`Telltales`)만 있다.
- 켜짐은 보충·누유 신호라 기록을 묻지 않고, **"지금 신경 쓸 항목"에 `차량 경고` 배지**로 올린다.
- **꺼짐**(LOW → NORMAL, 경고등 ON → OFF)은 누군가 손을 봤다는 증거 → 해당 항목을 **미리 고른 기록 질문**을 띄운다.
  - 정비소 정차 중이면 출차 질문에 "차량 신호로 확인된 항목"으로 합쳐 한 번만 묻는다. 머문 시간이 짧아도 신호 항목만은 묻는다.
  - 정비소가 아닌 곳이면 "12V 배터리 경고등이 꺼졌어요 · 교체하셨나요?" 카드. **[보충만 했어요]** 로 기록 없이 닫을 수 있다 (보충을 교체로 적으면 다음 알림이 틀어진다).
  - 주행 중에 꺼지면 다음 정차(P) 때 묻는다.
- 흔들림 방지: 레벨 센서는 경사·온도로 LOW/NORMAL을 오가므로, **시동 ON을 한 번 넘겼거나 10분 이상 켜져 있던 신호**만 인정한다 (`SignalTracker`).
- 매핑: 엔진오일 레벨 → 엔진오일, 브레이크액 레벨 → 브레이크액, 변속기 오일 레벨 → 변속기 오일, `LOW_VOLT_BATTERY` 경고등 → 12V 배터리. TPMS는 공기압 보충이 대부분이라 제외.
- 실차 범위: 엔진오일 레벨 센서는 일부 차종만, 브레이크액·12V 경고등은 대부분 → Capability API로 시작 때 지원 여부 확인.

### F3. 주행거리 자동 기록
- 기록 시 Odometer `getCurrentDistanceDriven()` 값을 자동으로 붙임 → 사용자는 거리 입력 불필요.
- 날짜·장소(정비소명)도 자동 기록. 금액·메모는 선택(정차 중에만 입력 가능).

### F4. 주행거리 기반 교체 리마인더
- 항목별 교체 주기(예: 엔진오일 7,000km / 1년) 기본값 + 사용자 수정.
- `registerDistanceDriven`으로 주행거리 변화를 구독해 임박 시 정차 중 알림: "엔진오일 교체까지 800km 남았어요".
- 보조 신호: Safety `getEngineOilLevelState` 경고 시 즉시 안내 (내연기관 차량만. 에뮬레이터·전기차는 미지원 확인).

### F4-1. 내연기관 / 전기차 2개 모드
- **판별**: 첫 실행 시 자동 추정 → 사용자 확인.
  - SDK에 연료 종류를 직접 주는 getter는 없음(`FuelType` 상수만 존재). `CarInfo.getCapabilityInfo()`의 capability 목록, `EvBattery.getEvBatteryCapacity()` 성공 여부로 추정한다 (런타임 미검증).
  - 추정이 실패하면 사용자가 직접 선택하고, 설정에서 언제든 바꿀 수 있다.
- **모드별 기본 관리 항목** (주기는 기본값, 사용자 수정 가능):

| 항목 | 내연기관 | 전기차 |
| --- | --- | --- |
| 엔진오일·오일필터 | ✅ 7,000km / 1년 | — |
| 에어클리너 | ✅ 15,000km | — |
| 점화플러그 | ✅ 차종별 | — |
| 변속기(미션)오일 | ✅ 차종별 | — |
| 감속기 오일 | — | ✅ 차종별 |
| 배터리 냉각수 | — | ✅ 차종별 |
| 타이어 (교체·위치 교환) | ✅ | ✅ 차가 무거워 마모가 빠름 → 주기 짧게 |
| 브레이크 패드 | ✅ | ✅ 회생제동 덕분에 주기 길게 |
| 브레이크액 | ✅ 2년 | ✅ 2년 |
| 에어컨 필터 | ✅ | ✅ |
| 와이퍼 | ✅ | ✅ |
| 12V 배터리 | ✅ | ✅ |

- **모드별 차량 신호**:
  - 내연기관: Safety `getEngineOilLevelState` / `getTransmissionOilLevelState` 경고 → 즉시 안내.
  - 전기차: 오일 신호 없음. 공통 신호(Safety `getBrakeFluidLevelState`, 경고등 `getTelltalesInfo`)만 사용.
- **오탐 구분**: 전기차는 충전소에 오래 정차하는 일이 많음 → 충전 중(`EvChargeState`)이면 정비소 감지에서 제외.

### F5. 정비 이력 보기 / 내보내기 (선택)
- 항목별 마지막 교체 거리·날짜 타임라인.
- QR → 폰으로 이력 내보내기 (중고차 매매 시 정비 이력 증빙 용도).

## 3. 사용 시나리오 (데모 스토리)

1. 10:00 동네 카센터 도착 → 시동 OFF, 차 맡김.
2. 11:00 정비 완료, 시동 ON (Virtual Console로 위치·기어 시뮬레이션) → "○○카센터에서 출발하시네요. 어떤 정비를 받으셨나요?"
3. [엔진오일] 탭 → "엔진오일 교체 기록 완료 · 52,340km · 다음 교체 59,340km".
4. (주행거리 시뮬레이션) 58,540km 도달 후 정차 → "엔진오일 교체까지 800km 남았어요".

## 4. 사용 SDK / API

| 구분 | 사용 기능 | 권한 |
| --- | --- | --- |
| Vehicle — Odometer | 총 주행거리 조회·구독 (`getCurrentDistanceDriven`, `registerDistanceDriven`) | `pleos.car.permission.CAR_DRIVE` |
| Vehicle — Brake / DrivingMode | 정차·주행 상태 판단 | `pleos.car.permission.CAR_DRIVE` |
| Vehicle — Safety | 엔진오일·브레이크액·변속기 오일 레벨 (`get/register*LevelState`, `check*Capabilities`) | `pleos.car.permission.CAR_SAFETY` |
| Vehicle — Safety (Telltales) | 경고등 (`getTelltalesInfo`, `registerTelltales`) → 12V 배터리 | `pleos.car.permission.PRIVATE_CAR_SAFETY` (normal) |
| NaviHelper | 목적지 도착 이벤트·POI, 현재 위치 | `pleos.car.permission.NAVI_ROUTE` |
| Android 표준 | 기어 P·차속(AAOS Car API), `CarUxRestrictions`, Notification(HUN) | — |
| 외부 | 장소 검색 API(정비소 판별) | — |

## 5. 평가 항목 대응

| 평가 항목 | 대응 포인트 |
| --- | --- |
| 차량특화성 | "정비소에서 출차하는 순간"과 "그 시점 주행거리"는 차만 앎. 폰 차계부로는 불가능한 자동화 |
| 창의성 | 사용자가 앱을 여는 차계부 → **차가 먼저 묻는** 차계부 |
| 기술 활용성 | Vehicle State(Odometer·Safety·주행 상태) + NaviHelper(도착·위치) |
| 사업성 | 정비소·체인 제휴(교체 임박 시 예약 연결), 보험·중고차 이력 증빙 |

## 6. 사업 모델 & 지표

- **과금**: 기본 무료 → 이력 내보내기·다차량 관리 프리미엄.
- **제휴**: 정비 체인·카센터 예약 연결(교체 임박 알림에서 예약), 중고차 플랫폼(정비 이력 증빙), 보험사.
- **지표**: 정비소 감지 대비 기록 응답률, 오탐률("정비 안 받음" 비율), 리마인더 후 실제 교체 기록률.
- 주의: 알림에 광고 표시 금지(심사 가이드) → 제휴 연결은 사용자 요청 시에만.

## 7. 리스크 & 사전 검증

| 리스크 | 영향 | 대응 |
| --- | --- | --- |
| ~~앱 상주 가능 여부 미확인~~ → ✅ 에뮬레이터에서 부팅 자동 시작·상주 확인 (7-1). 단 가이드는 "백그라운드 상주·폴링 최소화" 권장 → 리스너 기반으로만 동작, 폴링 없음 | 중 (심사) | 상주 서비스가 심사에서 문제 되는지 확인 |
| 헤즈업 알림이 화면에 안 뜸 (7-1) | 중 | 알림 센터 + 앱 내 카드로 대체하기로 결정. 주목도가 낮으니 응답률 지표로 확인 |
| 내연기관/전기차 자동 판별이 안 될 수 있음 | 하 | 첫 실행 시 사용자 선택으로 폴백 |
| 시동 OFF 중 앱 동작 불가 | 높음 | 알림 시점을 출차(시동 ON)로 변경 — F2에 반영 |
| 정비소 오탐 (옆 가게 주차, 세차장, 타이어 매장) | 중 | 좁은 반경 + 체류 시간 + 확인 질문 + "아니요" 장소 제외 |
| 외부 장소 검색 API 비용·정확도 | 중 | 정차 시에만 호출, 결과 캐시. 정비소 카테고리 코드 유무 확인 필요 |
| 마이현대 정비 이력과 중복 | 중 | 차별점을 "블루핸즈 외 정비소 + 주행거리 자동 기록"으로 명확히 |
| 위치 정보 처리 | 중 | 위치는 정비소 판별에만 사용, 원시 좌표는 저장하지 않음. 위치정보 약관 준비 |
| 시동 상태 API 없음 | 하 | 기어(`Gear` API ✅) + `SCREEN_ON`으로 대체 (7-1) |
| 에뮬레이터 주행거리 0.0 고정 | 중 (데모) | 데모용 주행거리 시뮬레이션 모드를 앱에 둠 |
| 주행 중 UI | 하 | 알림·입력은 정차 중에만, 주행 시작 시 카드 자동 닫힘 |

## 7-1. 구현 가능성 검증 결과 (2026-11-01, 에뮬레이터 Pleos Connect v2.1.0)

일회성 검증 앱(부팅 리시버 + `specialUse` 포그라운드 서비스 + Vehicle/NaviHelper 리스너)을 설치해 확인. 검증 후 삭제함.

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 부팅 후 자동 시작 | ✅ `BOOT_COMPLETED` 수신(user 10) → 포그라운드 서비스 시작 성공 | 앱을 한 번은 실행해야 함(설치 직후 stopped 상태). `LOCKED_BOOT_COMPLETED`는 미수신(directBootAware 미선언) |
| 백그라운드 상주 | ✅ 다른 앱이 전면일 때도 서비스 유지, 리스너 이벤트 수신 | `foregroundServiceType="specialUse"` |
| 시동 OFF→ON (Suspend-to-RAM) | ✅ `cmd car_service suspend --simulate` 후 프로세스 유지, 복귀 시 `SCREEN_OFF` → `SCREEN_ON` → `USER_PRESENT` 수신 | 실차 커널은 S2R 지원(`kernel support S2R: true`) → 시동 ON 시 재부팅이 아니라 복귀일 가능성 높음. **`SCREEN_ON`을 "시동 ON" 트리거로 사용** |
| 기어 조회·구독 | ✅ `Gear.getCurrentGearSelectionState` / `registerGearSelection`, 백그라운드에서 P→D 이벤트 수신 | 권한 `CAR_DRIVE` |
| 시동 상태(Ignition) 조회 | ❌ 공개 API 없음 (`VehicleIgnitionStateValue` 상수만 존재) | 시동 ON은 SCREEN_ON + 기어로 대체 |
| 주행거리 조회·구독 | ✅ API 동작 (`getCurrentDistanceDriven`, `registerDistanceDriven` 이벤트 수신) | ⚠️ 에뮬레이터 값은 항상 `0.0` — Virtual Console에 주행거리 조절 없음, 속도를 줘도 증가 안 함, VHAL 주입은 user 빌드라 불가 → 데모는 앱 내 시뮬레이션 필요 |
| 엔진오일 레벨 | ❌ `ENGINE_OIL_LEVEL` property 미지원 (에뮬레이터, IONIQ 5 형상 = 전기차) | 보조 신호에서 제외. 내연기관 실차에서만 의미 |
| NaviHelper 현재 위치 | ✅ 좌표 + 주소(`수정구 시흥동`) | 주소로 1차 필터 가능 |
| NaviHelper 목적지 | ✅ `name`, `address`, `poiId`, `poiProvider` 필드 존재 | 앱이 요청한 경로는 `poiId` 빈 값. 내비에서 직접 검색한 정비소의 POI 카테고리 정보는 없음 → 이름/외부 API로 판별 |
| 차량 화면 알림 등록 | ✅ `IMPORTANCE_HIGH` 알림 등록됨(Gleo AI도 수신 로그) | |
| **헤즈업(HUN) 배너 표시** | ❌ **화면에 뜨지 않음** — 정차·주행, 앱 전면·후면 모두. `heads_up_notifications_enabled=0`이고 1로 바꿔도 미표시 | 서드파티 HUN 정책이 문서에 없음. **기록 유도 UI를 알림 배너에 의존할 수 없음** → 대안 필요(아래) |

### 판정

**구현 가능.** 감지(부팅·상주·시동 복귀·기어·위치·주행거리)는 전부 동작 확인.

결정 사항 (2026-11-01):
- **알림**: 헤즈업 배너 대신 대체 수단을 쓴다 (알림 센터 + 앱 내 카드, 필요하면 미니 앱). 백그라운드에서 액티비티를 직접 띄우는 방식은 Android 14 제한과 심사 리스크 때문에 쓰지 않는다.
- **시동 상태**: 전용 API가 없어도 문제없음. 기어 + `SCREEN_ON`으로 판단한다.
- **차종**: 내연기관·전기차 2개 모드를 지원한다 (F4-1).

## 8. MVP 범위

- [x] **사전 검증**: 앱 상주 가능 여부, 기어·시동 상태 조회 가능 여부 (에뮬레이터) — 7-1 참조
- [x] 알림 노출 방식 결정 → 알림 센터 + 앱 내 카드
- [x] 내연기관/전기차 판별 — 배터리 용량(`CAR_INFO`)으로 전기차 추정 ✅, 첫 실행에서 사용자 확인
- [x] F1 정비소 방문 감지 — 내 정비소 → 내비 목적지 이름 → 지도 검색(카카오 키 있으면 카카오, 없으면 OSM)
- [x] F2 출차·시동 ON 시점 기록 카드 (알림 센터 + 홈 카드)
- [x] F3 주행거리 자동 기록 (로컬 JSON)
- [x] F4 주행거리·기간 기반 교체 리마인더 (정차 중에만 알림)
- [x] F4-1 모드별 기본 항목·주기, 주기 수정
- [x] Virtual Console 데모 시나리오 (기어·충전구) + 앱 데모 패널(주행거리 보정, 데모 목적지)
- [ ] F5 이력 내보내기(QR) — 선택 기능, 미구현

## 9. 구현 및 동작 검증 (2026-11-01, 에뮬레이터 `Pleos_Connect_Hephaestus`)

| 시나리오 | 결과 |
| --- | --- |
| 첫 실행 차종 추정 | ✅ 배터리 용량 77.4kWh → "차량 정보로 추정: 전기차" 제안 (권한 `CAR_INFO` 필요, 없으면 "need Permission") |
| 내비 목적지가 정비소 + 정차(P) | ✅ `정비소 감지: 시흥 오토 카센터 (내비 목적지)` |
| 정비소에서 출차(P → D) | ✅ 질문 카드·알림 생성, 주행거리 자동 첨부 ([스크린샷](docs/screenshots/01-home-visit-prompt.png)) |
| 시동 OFF → ON (`cmd car_service suspend --simulate`) | ✅ 복귀 `SCREEN_ON` → 정차 상태에서 바로 질문 |
| 항목 선택 → 기록 | ✅ 기록 저장, 교체 시기 목록 갱신, 그 정비소는 "내 정비소"로 자동 등록 ([스크린샷](docs/screenshots/02-history.png)) |
| 내 정비소에 다시 정차 | ✅ 지도 검색 없이 즉시 `(내 정비소)`로 판별 |
| 같은 곳에서 정차·출차 반복 | ✅ 6시간 안에는 다시 묻지 않음 (`prompt skipped`) |
| "정비소 아님" | ✅ 그 장소는 판별 단계에서 제외 (`ignored=true`) |
| 충전구 열림(전기차) 후 출차 | ✅ 충전 정차로 보고 묻지 않음 (Virtual Console `ev-battery?field=charge_port_open&value=true`) |
| 교체 시기 지남 | ✅ 정차 중 알림 "타이어 위치 교환 시기가 지났어요 · 2,000km 지남" ([스크린샷](docs/screenshots/01-home-visit-prompt.png)) |
| 마지막 교체 직접 입력 | ✅ 항목 탭 → 주행거리 입력 → 기록 |
| 내연기관 모드 전환 | ✅ 엔진오일·에어클리너·점화플러그·변속기 오일 항목으로 바뀜 |
| 재부팅 | ✅ `BOOT_COMPLETED` → 서비스 자동 시작 → 내 정비소 판별 |
| 내 정비소 등록 (설정) | ✅ 이름 제안 시트(주소 기반·내비 목적지·단골 카센터)에서 고르면 등록, 정차 중이던 현재 위치가 즉시 `(내 정비소)`로 재판별 |
| 주행 중 | ✅ DO 미선언 → 시스템이 "주행 중에는 사용할 수 없습니다"로 가림 ([스크린샷](docs/screenshots/04-blocked-while-driving.png)) |
| 차량 경고등 켜짐 (Virtual Console `telltales/simulator?type=LowVoltBattery&state=On&color=Red`) | ✅ 홈·소모품에 "12V 배터리 `차량 경고`" 맨 위 ([스크린샷](docs/screenshots/10-home-vehicle-warning.png)); 계기판에도 경고등 표시 |
| 정비소 정차 중 경고등 꺼짐 | ✅ `signals [BATTERY_12V_WARNING] attached to prompt` → 출차 질문에 "차량 신호로 확인된 항목: 12V 배터리" 미리 선택 → 기록 |
| 정비소 아닌 곳에서 경고등 꺼짐 | ✅ 카드 "12V 배터리 경고등이 꺼졌어요 · 교체하셨나요?" + [보충만 했어요] ([스크린샷](docs/screenshots/12-home-signal-prompt.png)) |
| 내연기관 모드 · 브레이크액 흉내 켬 | ✅ 소모품 탭 맨 위 "차량 경고" 묶음에 브레이크액 ([스크린샷](docs/screenshots/13-items-vehicle-warning.png)) |
| 브레이크액 흉내 끔 (12V 질문이 남아 있을 때) | ✅ 기다리던 질문에 합쳐져 "12V 배터리 경고등이 꺼졌어요 · 브레이크액 레벨이 정상으로 돌아왔어요", 두 항목 미리 선택 → "2개 기록하기" |
| 브레이크액 흉내 끔 → [보충만 했어요] | ✅ 기록 없이 카드만 닫힘 |
| 레벨 상태 Capability | ✅ 전기차 에뮬레이터는 엔진오일·브레이크액·변속기 오일 모두 `ERROR_NOT_SUPPORTED_PROPERTY` → 미지원으로 표시, 설정 > 개발용 "차량 신호 흉내"로 데모 |
| 단위 테스트 | ✅ 34개 (방문 감지 상태 기계, 차량 신호 추적·병합, 교체 시기, 정비소 판별, 쿨다운, 저장 형식) |

### 데모 순서 (Virtual Console + 앱 설정 > 데모)

1. 설정 > 감지 "바로", 데모 "+50,000km"
2. 설정 > 데모 "현재 위치 카센터를 목적지로" → 내비에 "시흥 오토 카센터" 설정
3. 기어 D → P (정차) → 홈 "정비소 감지: 시흥 오토 카센터"
4. 기어 D (출차) → 홈 카드 "시흥 오토 카센터에서 출발하시네요" → 항목 선택 → 기록
5. 설정 > 데모 "+5,000km" 두 번 → "타이어 위치 교환 시기가 지났어요" 알림
6. Virtual Console `PUT /vehicle/telltales/simulator?type=LowVoltBattery&state=On&color=Red` → 홈 "12V 배터리 · 차량 경고"
7. 시동 OFF→ON (`cmd car_service suspend --simulate …`) 뒤 `state=Off` → "12V 배터리 경고등이 꺼졌어요" 카드 (또는 정비소 정차 중이면 출차 질문에 미리 선택). 에뮬레이터가 지원하지 않는 오일 레벨은 설정 > 개발용 "차량 신호 흉내" 칩으로 켜고 끈다.

### 검증하지 못한 항목

| 항목 | 이유 | 확인 방법 |
| --- | --- | --- |
| 카카오 로컬 검색 | 키 없음 (OSM은 이 지역 정비소 데이터 0건, Overpass 502/타임아웃도 발생) | `secrets.properties`에 `KAKAO_REST_KEY` 입력 후 실제 정비소 좌표에서 정차 |
| 실차 헤즈업 알림 | 에뮬레이터는 서드파티 HUN 미표시 | 실차 |
| 실차 주행거리 값·단위 | 에뮬레이터는 항상 0.0 | 실차에서 `getCurrentDistanceDriven` 로그 |
| 내연기관 차종 추정 | 에뮬레이터가 전기차(IONIQ 5)뿐 | 내연기관 실차에서 `getFuelPercent` 동작 확인 |
| 실제 시동 OFF 시 프로세스 유지 | 에뮬레이터는 Suspend 시뮬레이션만 가능 | 실차 |
| 오일·브레이크액 레벨 신호 | 전기차 에뮬레이터는 property 미지원 (Virtual Console에도 레벨 항목 없음) | 내연기관 실차에서 `checkEngineOilLevelStateCapabilities`·`getBrakeFluidLevelState` 로그 |

## 10. 디자인 · 사용성

Pleos 기본 UI(밝은 톤)와 같은 앱처럼 보이도록 myHyundai 앱의 시각 언어(연회색 배경 + 흰 면 + 남색 주색)를 따랐다. 색은 "기본·그 위 글자·연한 면·면 위 글자" 세트(`Tone`)로 정의하고, 주간/야간 토큰 전부 대비 4.5:1 이상을 단위 테스트로 검증한다.

### 2026-11-07 정리: "생성형 UI" 티 걷어내기

`audit-ai-design-slop` 스킬로 감사한 결과 근본 원인은 셋이었고, 부품 단위로 한 번에 고쳤다.

| 원인 | 전 | 후 |
| --- | --- | --- |
| 모든 메시지를 "색 면 + 테두리 + 아이콘 + 문장" 배너로 처리 | 정차 안내(파랑)·저장 확인(초록)·급한 항목 없음(초록)·로딩(회색) 전부 배너 | 상태는 글자 한 줄(`StatusLine`), 저장 확인만 연한 면 한 겹(`SavedNote`). 색 면이 있는 블록은 질문 카드 머리 하나 |
| 알약 배지·파란 링크로 강조 | "차량 경고" 빨간 알약, 묶음 개수 초록 알약, "차량 정보로 추천" 알약, "입력하기"·"전체 N개 보기" 파란 글자 | 색은 오른쪽 값 하나에만. 경고는 작은 아이콘 + 빨간 값, 개수는 회색 숫자, 링크색 없음 |
| 카드 안에 카드, 큰 둥근 모서리, 설명하는 문장 라벨 | 이력·설정·소모품 묶음마다 카드, radius 20, "가장 먼저 돌아오는 항목"·"입력하면 알려드려요" | 화면당 면 하나 + 구분선, radius 12, 명사형 라벨("급한 항목 없음", "기록 없음 6", "정차 중 · 수정구 시흥동 · 05:31부터") |

그 밖에: 굵기 두 단계(Medium/SemiBold)만 사용, 숫자 고정폭(tnum), 칩 테두리 제거, 이력 항목의 × 아이콘 제거(누르면 확인 시트).

### 사용성 원칙 (Pleos 가이드: 1~2초 안에 파악, 한 손 조작)

| 원칙 | 적용 |
| --- | --- |
| 홈은 "지금 할 일"만 | 왼쪽: 정비 기록 질문(있으면 맨 위) + 차량 상태. 오른쪽: 교체 필요·곧 교체 항목만. 전체 목록은 소모품 탭 |
| 선택지 줄이기 | 질문 카드에서 **교체 예정이던 항목을 앞에** 따로 묶음 → 보통 1~2탭으로 끝 |
| 타이핑 없음 | 주행거리는 ±100/1,000/10,000 버튼, 교체 주기는 프리셋 칩. 키보드를 띄우지 않음 |
| 한 자리에서 끝내기 | 항목을 누르면 상세 시트: "지금 교체했어요"(1탭) · 다른 주행거리로 · 교체 주기 변경 |
| 피드백 | 기록·변경 후 연한 초록 한 줄("타이어 위치 교환 기록됨 · 48,900km · 다음 교체 56,900km"), 6초 뒤 사라짐 |
| 실수 방지 | 이력 삭제는 확인 시트. 개발용 데모·진단은 설정 맨 아래 접어 둠 |
| 주행 중 | DO 미선언 → 시스템이 화면을 가림. 알림은 정차 중에만 |

### 화면

- [홈 · 출차 질문 카드](docs/screenshots/01-home-visit-prompt.png) · [홈 · 급한 항목 없음](docs/screenshots/09-home-all-clear.png) · [홈 · 야간](docs/screenshots/15-home-night.png)
- [홈 · 차량 경고](docs/screenshots/10-home-vehicle-warning.png) · [차량 신호 질문](docs/screenshots/12-home-signal-prompt.png)
- [소모품 · 차량 경고 묶음](docs/screenshots/13-items-vehicle-warning.png) · [항목 시트](docs/screenshots/07-items.png) · [주행거리 고르기](docs/screenshots/06-odometer-picker.png) · [교체 주기 고르기](docs/screenshots/08-interval-picker.png)
- [정비 이력](docs/screenshots/02-history.png) · [설정](docs/screenshots/03-settings.png)

## 참고 문서

- [Connect SDK 레퍼런스](../docs/pleos/02-connect-sdk.md) · [메서드 인덱스](../docs/pleos/02a-sdk-method-index.md)
- [주행 규제·백그라운드·알림 가이드](../docs/pleos/03-app-design-dev-guide.md) · [심사 체크리스트](../docs/pleos/04-test-and-publish.md)
- [Playground SDK 실제 상태](../docs/pleos/README.md)
