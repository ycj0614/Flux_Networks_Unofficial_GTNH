# Flux Networks GTNH (1.7.10 백포트)

Flux Networks(SonarSonic, 1.12.2-refactored 브랜치, MIT, Copyright (c) 2019 BloCamLimb)를
Minecraft 1.7.10 / Forge 10.13.4.1614로 옮기는 프로젝트다. **GTNH 전용**이며 에너지는
**GregTech 5 Unofficial(GT5U)의 EU만** 다룬다. RF·FE·IC2 에너지는 지원하지 않는다.

- 패키지 `com.ycj0614.fluxnetworks` (원작 `sonar.fluxnetworks`의 하위 구조 유지), 모드 ID `fluxnetworks`
- 의존성: `com.github.GTNewHorizons:GT5-Unofficial:5.09.54.205:dev` (GTNH 2.9.0-RC-2). 정식 2.9.0이 나오면 숫자만 바꾼다
- 빌드: GTNH ExampleMod1.7.10 템플릿(RetroFuturaGradle). 바이트코드는 Java 8
- 사용자는 한국어를 쓴다. 설명과 코드 주석은 한국어로 쓴다

## 명령

```
./gradlew build        # 빌드 (작업이 끝날 때마다 반드시 성공시킨다)
./gradlew runClient    # 클라이언트 실행 (게임 안 테스트는 사용자가 한다)
./gradlew runServer    # 데디케이티드 서버 실행 (멀티플레이 테스트용)
```

## 참고 소스 (저장소 밖에 둔다)

원작과 GT5U 소스를 읽어야 할 때 저장소 **밖** `../reference/`에 받아서 쓴다. 저장소 안에 넣지 않는다.

```
git clone --depth 1 --branch 1.12.2-refactored https://github.com/SonarSonic/Flux-Networks.git ../reference/Flux-Networks
git clone --depth 1 --filter=blob:none --sparse --branch 5.09.54.205 https://github.com/GTNewHorizons/GT5-Unofficial.git ../reference/GT5-Unofficial
git -C ../reference/GT5-Unofficial sparse-checkout set src/main/java/gregtech/api
```

- 원작: `../reference/Flux-Networks/src/main/java/sonar/fluxnetworks/`
- GT5U: `../reference/GT5-Unofficial/src/main/java/gregtech/api/` (blob:none 클론이므로 `git grep`은 매우 느리다. 체크아웃된 파일을 일반 검색으로 찾는다)
- 원작 코드는 GT EU용으로 **다시 설계된 부분이 많다**. 원작과 현재 코드가 다르면 현재 코드와 아래 규칙이 우선이다

## 진행 상태

| 단계 | 배치 | 상태 |
| --- | --- | --- |
| 1단계 EU 전송 MVP | A API / B 진입점·설정·프록시 / C 네트워크 코어 / D 저장 데이터·FluxUtils / E 전송 핸들러 / F 타일 / G 블록·등록·디버그 명령 | 완료, 게임 테스트 통과 |
| 2단계 GUI·패킷 | H 패킷 기반·SuperAdmin·동기화 패킷 / I 명령 패킷 / J 구성기·ItemBlock·색상 캐시 / K GUI 틀 | 완료 |
| 2단계 남은 배치 | **L** → M | 다음은 **L** |
| 3단계 | 스토리지, 컨트롤러, 플럭스 블록, 청크 로딩, 원작 모양 렌더링, 네트워크 색상 | 대기 |
| 4단계 | 컨트롤러 무선 충전 (GT 전기 아이템) | 대기 |
| 5단계 | GT 레시피, NEI, WAILA, 배포 | 대기 |

GUI는 **원작 GUI를 그대로 옮긴다** (ModularUI 쓰지 않음).

### 남은 2단계 배치

- **J**: 커넥터 `ItemBlock`(이름·툴팁, 네트워크 이름과 EU 설정 표시), 구성기(`ItemConfigurator`), 관리자 구성기, `GuiHandler`,
  구성기 패킷 `PacketConfiguratorSettings`(ID 17), `PacketSetConfiguratorNetwork`(ID 18),
  색상 캐시 패킷 `PacketColorRequest`(ID 7), `PacketColorCache`(ID 8), 원작 `client/FluxColorHandler`의 네트워크 이름·색 캐시 부분.
  코드에 남은 `// TODO 배치 J` 처리 (네트워크 편집 시 색상 캐시 전송 포함)
- **K**: GUI 틀. `GuiFluxCore`, 탭·팝업 구조(`GuiTabCore`, `GuiTabPages`, `GuiPopUpHost`, `GuiPopCore`), `GuiDraw`, 버튼 9종, `GuiTextField`.
  `GlStateManager` → `GL11`, `BufferBuilder` → `Tessellator`. `// TODO 배치 K`(권한·SuperAdmin 변경을 열린 GUI에 전달) 처리
- **L**: GUI 화면. 커넥터 홈(전압 티어 선택·암페어 입력, 플러그 최대 입력 티어), 선택, 생성, 설정, 멤버, 연결, 통계(`LineChart`), 팝업.
  무선 탭은 4단계라 숨긴다
- **M**: `/fluxdebug` 명령과 블록 우클릭 정보 출력 삭제(GUI로 대체), 남은 `// TODO 2단계` 정리, 데디케이티드 서버 테스트

## 작업 방식

- 배치 하나를 끝내면 `./gradlew build` 성공을 확인하고 바뀐 점을 요약한다. 커밋과 게임 안 테스트는 사용자가 한다
- 범위가 큰 배치(K, L)는 코드를 쓰기 전에 파일 목록과 순서를 계획으로 먼저 보여주고 승인을 받는다
- 배치 범위 밖 기능은 지우지 말고 `// TODO <단계 또는 배치>:` 주석으로 남긴다 (원작 동작 한 줄 요약 포함)
- 끝나면 사용자가 게임에서 확인할 절차를 짧게 적어준다

## 금지 사항 (1.12 코드가 섞여 들어오기 쉽다)

- `BlockPos`, `IBlockState`, `EnumFacing`, `ChunkPos`, `Capability`/`@CapabilityInject`, `ITickable`, JSON 모델·blockstate 금지
- `net.minecraftforge.fml.*` 금지 → `cpw.mods.fml.*`
- `it.unimi.dsi.fastutil.*`, `org.apache.commons.io.*` 금지
- Java 9 이상 API 금지 (`List.of`, `Map.of`, `String.isBlank`, `Optional.isEmpty` 등)
- `EnergyType`, RF·FE·IC2 에너지 코드 금지. GT 쪽은 `IEnergyConnected`, `GTValues`(→ `GTEnergy` 헬퍼), `GregTechAPI.causeCableUpdate`, `BaseMetaPipeEntity`만 쓴다
- `IEnergyConnected.Util.emitEnergyToNetwork` 금지 (IC2·RF로도 보내고 송신자 조건이 맞지 않음)

자주 쓰는 변환: `world/pos` → `worldObj/xCoord,yCoord,zCoord`, `update()` → `updateEntity()`,
`writeToNBT`는 void, `notifyBlockUpdate` → `markBlockForUpdate`, `ItemStack.EMPTY` → `null`,
`TextFormatting` → `EnumChatFormatting`, `mc.player` → `mc.thePlayer`, NBT UUID는 `키+"Most"`/`키+"Least"` 두 long.

## 확정된 설계 (바꾸지 않는다)

1. **내부 단위는 EU 1:1**. 원작의 FE↔EU `<< 2` 변환은 없다
2. **포인트**: 네트워크 틱 끝(`onCycleEnd`)에 출력. 요청량 = 전압 × 암페어 × `FluxConfig.pointBufferTicks` − 버퍼.
   인접 `IEnergyConnected`에 `injectEnergyUnits(반대면, V, 남은 암페어)`. 못 보낸 EU는 버퍼에 남는다.
   출력 전압이 GT 기계보다 높으면 기계가 폭발한다 (GT 규칙, 의도된 동작)
3. **플러그**: `FluxPlugHandler.injectEnergyUnits`가 암페어 단위로 받는다. 최대 입력 티어 초과 전압은 0 반환(폭발 없음). `side`가 `UNKNOWN`이어도 받는다
4. **전송 사이클**: `FluxNetworkServer.onEndServerTick`과 우선순위 정렬은 원작 알고리즘 그대로. 수정 금지
5. **타일 구조**: `TileFluxCore` → `TileFluxConnector`(GT `IEnergyConnected` 구현, 색 -1) → `TileFluxPlug` / `TileFluxPoint`
6. **EU 설정**: 플러그 `maxInputTier`(NBT `"mt"`), 포인트 `outputTier`(`"ot"`), `outputAmperage`(`"oa"`). 티어 0(ULV)~14(MAX).
   값을 바꿀 때는 반드시 setter(`setMaxInputTier`, `setOutputTier`, `setOutputAmperage`)를 쓴다 (보정과 동기화 포함)
7. **GT 케이블 갱신**: `TileFluxConnector.notifyGTCables()`가 인접 GT 케이블 좌표로 `GregTechAPI.causeCableUpdate`를 호출한다.
   첫 틱, 25틱 뒤, `invalidate` 때. GT 케이블 연결을 강제로 끊었다 잇는 방식은 쓰지 않는다
8. **등록 이름 (월드 저장에 기록됨, 변경 금지)**: 블록 `fluxplug`, `fluxpoint` / 타일 `fluxnetworks:FluxPlug`, `fluxnetworks:FluxPoint`
9. **아이템 NBT**: 장치 데이터는 `FluxUtils.FLUX_DATA` 하위 태그, 키는 `TileFluxCore.NBT_CUSTOM_NAME` 등 상수. 설정 복사는 `FluxConfigurationType.copyAll/pasteAll`
10. **SuperAdmin**: `IExtendedEntityProperties`(`SuperAdminData`). 원작처럼 죽으면 해제

## 패킷 규칙 (배치 H·I에서 확정)

- `onMessage`는 메시지 내용과 플레이어만 꺼내고 `PacketHandler.handlePacket(Runnable, MessageContext)`로 넘긴 뒤 **항상 `null` 반환**.
  권한 확인·네트워크 조회·타일 조회 등 모든 게임 상태 접근은 그 작업(메인 스레드 큐) 안에서 한다
- 응답은 작업 안에서 `PacketHandler.network.sendTo(응답, (EntityPlayerMP) player)`
- 패킷 클래스에 `net.minecraft.client.*`나 GUI 클래스를 import하지 않는다. 클라이언트 일은 `CommonProxy`(빈 기본 메서드) / `ClientProxy`(구현)로 위임
- 패킷 ID는 원작 번호 고정: 1 General, 2 NetworkUpdate, 3·4 ByteBuf, 5 Tile, 6 Feedback, 7 ColorRequest, 8 ColorCache, 9 NetworkUpdateRequest,
  10 ConnectionUpdate, 11 ConnectionUpdateRequest, 12 BatchEditing, 13 PermissionRequest, 14 GUIPermission, 15 SuperAdmin, 16 ActivateSuperAdmin,
  17 ConfiguratorSettings, 18 SetConfiguratorNetwork (7·8·17·18은 배치 J에서 등록)
- enum을 ordinal로 읽을 때 범위를 벗어나면 패킷을 무시한다
- `ITileByteBuf` ID: 1 이름, 2 우선순위, 3 한도, 4 서지 모드, 5 한도 해제, 6 티어(플러그 최대 입력·포인트 출력), 7 출력 암페어(포인트)

## 보안 규칙 (원작과 다름, 코드에 `// 보안:` 주석)

- 장치 대상 패킷: 같은 차원, 블록 중심까지 거리 제곱 64 이하(8블록), `tile.canAccess(player)`일 때만 처리
- 소유권 이전은 소유자·SuperAdmin만. 관리자는 소유자를 바꾸거나 제거할 수 없다
- 네트워크·장치 이름 24자, 비밀번호 16자 제한. 좌표 목록은 1024개 초과 시 무시
- 구성기 패킷(배치 J)도 같은 원칙: 손에 든 아이템이 구성기인지, 플레이어 권한이 맞는지 서버에서 다시 확인

## 주석 규칙

- `// TODO 2단계:`, `// TODO 3단계:` … / `// TODO 배치 J:` … → 해당 시점에 검색해서 처리
- `// TODO 확인:` → 1.7.10 메서드 이름이 불확실한 곳. 빌드로 확인하고 지운다
- `// 보안:`, `// 원작과 다름` → 의도적으로 원작과 다르게 한 곳. 지우지 않는다

## 그 밖의 기록

- 설계 배경과 결정 이력: claude.ai의 계획서 문서 "Flux Networks 1.7.10 GTNH 백포트 계획서"
- 진행·테스트 기록: 사용자가 가진 1단계·2단계 트래커 엑셀 파일
