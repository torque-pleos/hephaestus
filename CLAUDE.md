# Hephaestus — 개발 안내

정비소 출차 시 기록 유도 + 주행거리 기반 교체 알림 차계부 앱 (내연기관·전기차). 기획·검증 결과는 [README.md](README.md). 공모전 제출용(상용 수준 작업 불필요).

## 빌드 · 실행

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
adb -s emulator-5556 install -r -g app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 shell am start -n com.zigae.hephaestus/.MainActivity
adb -s emulator-5556 logcat -s Heph/Service Heph/Detector Heph/Vehicle Heph/Navi Heph/ShopSearch
```

- 전용 에뮬레이터 `Pleos_Connect_Hephaestus` (`-port 5556`, `-no-audio`). Hermes 에뮬레이터(5554)는 쓰지 않는다.
- 단위 테스트: `./gradlew :app:testDebugUnitTest`
- 선택: `secrets.properties`에 `KAKAO_REST_KEY` (커밋 금지). 없으면 OSM으로 검색.
- 구성: AGP 8.13.2 · Gradle 8.14.3 · Kotlin 2.2.21 · Compose · compileSdk 36 / targetSdk 34 / minSdk 28.

## 구조

| 경로 | 역할 |
| --- | --- |
| `domain/Maintenance.kt` | 차종(ICE/EV), 관리 항목·모드별 기본 주기, 정비 기록 |
| `domain/Reminders.kt` | 항목별 남은 거리·일수, 단계(기록 없음/여유/임박/지남), 급한 순 정렬 |
| `domain/Shops.kt` | 정비소 이름 판별, 내 정비소·정비소 아님·내비 목적지 규칙 |
| `domain/VisitTracker.kt` | 방문 감지 상태 기계(P 정차 → 시동 ON 또는 출차 시 질문), 같은 곳 6시간 쿨다운, 꺼진 차량 신호를 질문에 합침 |
| `domain/Signals.kt` | 차량 신호(오일·브레이크액·변속기 오일 레벨, 12V 경고등) → 소모품 매핑, 꺼짐 인정 규칙(시동 주기 넘김 또는 10분) |
| `data/` | 앱 상태 JSON 저장(`files/hephaestus.json`) |
| `vehicle/VehicleMonitor.kt` | 기어·주행거리·충전구·레벨 상태·경고등(Capability 확인 후 구독), 배터리 용량/연료로 차종 추정 |
| `navi/NaviMonitor.kt` | 현재 위치·주소·목적지, 데모 목적지 설정 |
| `service/Detector.kt` | 이벤트 → 상태 기계, 정비소 판별(규칙 → 지도 검색), 질문·교체 알림, 신호 질문 병합(한 번에 질문 하나) |
| `service/DetectionService.kt` | `specialUse` 포그라운드 서비스 + 부팅 리시버, `SCREEN_ON` = 시동 ON |
| `service/ShopSearch.kt` | 카카오 로컬(키 있을 때) / OSM Overpass |
| `ui/` | `HephApp`(탭 레일) · `HomeScreen`(질문 카드 + 급한 항목) · `ItemsScreen`(전체 소모품 + `ItemSheet` 기록/주기 변경) · `HistoryScreen` · `SettingsScreen`(차종·내 정비소·감지, 개발용 접힘) · `Components`(패널·배너·배지·주행거리/주기 피커) · `Theme`(주간/야간 토큰) |

## 에뮬레이터에서 확인한 사실

| 항목 | 결과 |
| --- | --- |
| 배터리 용량 | `CAR_INFO` 권한 필요(`INFO_EV_BATTERY_CAPACITY`), 77400.0 |
| 연료 잔량 | 전기차 에뮬레이터는 property 미지원 → 예외 |
| 엔진오일·브레이크액·변속기 오일 레벨 | Capability `ERROR_NOT_SUPPORTED_PROPERTY` (전기차) → 설정 > 개발용 "차량 신호 흉내"로 데모 |
| 경고등(Telltales) | `PRIVATE_CAR_SAFETY` 권한(normal) 필요. Virtual Console로 켜고 끌 수 있고 리스너로 수신 |
| 주행거리 | 항상 0.0 → 앱 데모 보정값 사용 |
| `requestRoute`로 현재 위치를 목적지로 | ✅ 내비가 1m 경로로 받아들이고 목적지 이름 유지 |
| 충전구 | Virtual Console `PUT /vehicle/ev-battery?field=charge_port_open&value=true` → 리스너 수신 |
| 주행 중 화면 | DO 미선언 → 시스템 차단 화면 |
| 서드파티 헤즈업 알림 | 표시 안 됨 → 알림 센터 + 홈 카드 |

## Virtual Console (Hephaestus 에뮬레이터)

```bash
adb -s emulator-5556 forward tcp:8089 tcp:8088
# 브라우저로 http://127.0.0.1:8089 를 열어 두거나 하트비트를 보낸다 (없으면 emulator_inactive)
while true; do curl -s -X POST "http://127.0.0.1:8089/browser/session?client=cli" >/dev/null; sleep 1; done &
curl -X PUT "http://127.0.0.1:8089/vehicle/gear?gear=Park"            # Park | Reverse | Neutral | Drive
curl -X PUT "http://127.0.0.1:8089/vehicle/controls?speed_kph=40"
adb -s emulator-5556 shell cmd car_service suspend --simulate --skip-garagemode --wakeup-after 8   # 시동 OFF→ON
curl -X PUT "http://127.0.0.1:8089/vehicle/telltales/simulator?type=LowVoltBattery&state=On&color=Red"   # 경고등 (color 없으면 400), Off도 color 필요
# 오일 레벨·브레이크액은 Virtual Console에 항목이 없다 (catalog: /vehicle/catalog)
```
