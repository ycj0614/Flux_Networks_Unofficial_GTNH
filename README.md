# fluxnetworksgtnh

This is personal, experimental work and sadly vibecoded. Recommend to do not fork or something...

## 소개

[Flux Networks](https://github.com/SonarSonic/Flux-Networks)(1.12.2)를 Minecraft 1.7.10 / Forge 10.13.4.1614로 옮긴 비공식 백포트입니다.
**GregTech New Horizons(GTNH) 전용**이며, 에너지는 GregTech 5 Unofficial의 **EU만** 다룹니다. RF·FE·IC2 에너지는 지원하지 않습니다.

## 요구 사항

- GTNH 2.9.0 (GregTech 5 Unofficial 5.09.54.264로 빌드)
- Java 8 바이트코드 (GTNH가 지원하는 Java로 실행)

## 기능

- 플러그·포인트·스토리지(3종)·컨트롤러로 네트워크를 이루어 EU를 무선으로 보냅니다. 내부 단위는 EU 1:1입니다
- 플러그는 장치별 최대 입력 티어, 포인트는 출력 티어와 출력 암페어를 정합니다 (ULV ~ MAX).
  포인트의 출력 전압이 GT 기계의 입력 전압보다 높으면 기계가 폭발합니다 (GT 규칙)
- 원작 GUI(네트워크 생성·선택, 연결 목록, 일괄 편집, 멤버, 통계, 무선 충전)와 구성기, 청크 로딩
- 컨트롤러 무선 충전: 네트워크 멤버의 손·핫바·갑옷·바우블 슬롯의 전기 아이템(IC2 `IElectricItem` 기반)을 충전합니다

## 연동 (모두 선택)

- **NEI**: 흑요석 위 레드스톤을 좌클릭해 플럭스를 만드는 방법을 보여줍니다
- **WAILA**: 네트워크 이름, 전송량, 버퍼, 한도, 우선순위, 청크 로딩, EU 티어·암페어. WAILA 설정의 "Flux Networks" 항목에서 끄고 켭니다
- **Baubles(Baubles-Expanded)**: 컨트롤러 무선 충전의 바우블 슬롯 (GTNH에는 GregTech가 필요로 해서 항상 들어 있습니다)

## 설정

`config/flux_networks.cfg`

- `Enable GT Recipes`(기본 true): GT 조립기 레시피를 씁니다. 끄면 원작 제작대 레시피를 씁니다
- 원작 설정(흑요석 제작, 청크 로딩, SuperAdmin 등)과 장치별 EU 용량·전송량

## 빌드

```
./gradlew build
```

결과물은 `build/libs/`에 생깁니다. 버전은 git 태그로 정해집니다.

## 크레딧과 라이선스

- 원작: Flux Networks, SonarSonic · BloCamLimb, MIT License (Copyright (c) 2019 BloCamLimb)
- 원작의 텍스처와 번역 파일을 포함합니다
- 이 포트도 MIT License를 따릅니다. [LICENSE](LICENSE)를 보세요
