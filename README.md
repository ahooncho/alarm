# alarm

iOS / Android용 모바일 알람 앱 (Flutter).

## 시작하기

```bash
flutter pub get
flutter run
```

- Flutter 3.47 (stable) / Dart 3.13 기준으로 생성되었습니다.
- 패키지 이름은 `alarm_app`, 번들 ID는 `com.ahooncho.alarm_app` 입니다.

## 개발

```bash
flutter analyze   # 정적 분석
flutter test      # 테스트
```

## 다음 단계 (제안)

- [ ] 알람 데이터 모델과 로컬 저장소 (예: `shared_preferences` 또는 `sqflite`)
- [ ] 알람 목록 / 추가 / 편집 화면
- [ ] 알람 예약과 울림 (예: `alarm` 패키지). Android 정확한 알람 권한, iOS 백그라운드 제약 확인 필요
- [ ] 알림 권한 요청 흐름
