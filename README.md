# alarm

룸메이트를 깨우지 않는 **이어폰 전용 알람**. Galaxy S24와 AirPods Pro 2 조합으로 혼자 쓰는 안드로이드 앱이에요.

## 동작 방식

삼성 기본 알람은 알람용 오디오로 울려요. 안드로이드는 이어폰이 연결돼 있어도 알람용 오디오를 스피커로 같이 내보내요.
이 앱은 알람을 **미디어 소리**로 재생해요. 미디어 소리는 연결된 이어폰으로만 나가요. 여기에 안전장치를 겹쳐 뒀어요.

- 볼륨 0으로 재생을 시작하고, 출력 장치가 이어폰으로 확인된 뒤에만 점점 크게 올려요.
- 이어폰이 빠지거나 끊기거나 출력 경로가 바뀌는 신호가 오면 바로 음소거하고 정지해요.
- 이어폰이 없으면 폰 **진동만** 해요. 폰 스피커로는 절대 소리를 내지 않아요.
- AirPods 줄기 누르기는 무시해요. 잠결에 눌러서 알람이 꺼지는 일을 막아요.
- 알람은 `AlarmManager.setAlarmClock`으로 예약해요. 새벽 자동 재시작 뒤 잠금 해제 전 상태에서도 다시 등록되고 울려요.

자세한 요구사항과 결정 사항은 [docs/REQUIREMENTS.md](docs/REQUIREMENTS.md)에 있어요.

## 설치

- push할 때마다 GitHub Actions가 APK를 빌드해요. **Actions** 탭에서 최신 실행을 열고 **Artifacts**의 `alarm-apk`를 받아 압축을 풀면 돼요.
- 로컬에서는 `./gradlew assembleRelease`로 빌드해요. 결과물은 `app/build/outputs/apk/release/app-release.apk`예요.
- 설치 후 앱의 **상태** 항목이 모두 "허용"·"제한 없음"인지 확인해 주세요. 줄을 누르면 해당 설정 화면이 열려요.

서명 키 `app/debug.keystore`(비밀번호 `android`)는 저장소에 포함돼 있어요. 어느 컴퓨터에서 빌드해도 기존 앱 위에 덮어 설치할 수 있게 하려는 거예요. 개인용 앱이라 이렇게 했어요.

## 구조

| 패키지 | 역할 |
| --- | --- |
| `data` | 알람·설정 모델과 JSON 저장소. 잠금 해제 전에도 읽을 수 있는 저장 공간에 둬요. |
| `schedule` | 다음 울릴 시각 계산, `AlarmManager` 예약, 재부팅 후 재등록 |
| `ring` | 울리는 동안 동작하는 서비스, 이어폰 전용 재생(`EarphonePlayer`), 진동, 알림 |
| `system` | 알림·잠금 화면·배터리·방해 금지 설정 점검 |
| `ui` | 흑백 미니멀 화면 (Jetpack Compose) |

## 개발

```bash
./gradlew testDebugUnitTest   # 단위 테스트
./gradlew assembleRelease     # APK
```
