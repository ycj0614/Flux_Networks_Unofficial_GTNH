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
| 2단계 GUI·패킷 | H 패킷 기반·SuperAdmin·동기화 패킷 / I 명령 패킷 / J 구성기·ItemBlock·색상 캐시 / K GUI 틀 / L GUI 화면 / M `/fluxdebug` 삭제·TODO 정리 | **완료 (마일스톤)**, 데디케이티드 서버 멀티플레이 테스트 통과 |
| 3단계 | N 스토리지 / O 컨트롤러 / P 플럭스 아이템·블록 / Q 청크 로딩·일괄 편집 / R 원작 모양 렌더링·네트워크 색상 | 완료, 게임 테스트 통과 |
| 4단계 컨트롤러 무선 충전 | S 아이템 충전 헬퍼 / T 충전 핸들러 / U 무선 GUI 탭 / V 바우블 | **다음은 S** |
| 5단계 | GT 레시피, NEI, WAILA, 배포 | 대기 |

GUI는 **원작 GUI를 그대로 옮긴다** (ModularUI 쓰지 않음).

### 3단계 배치

배치마다 코드에 남은 `// TODO 3단계:` 중 해당 항목을 검색해서 처리한다. 각 배치 끝에 그 배치의 TODO가 남지 않았는지 확인한다.

- **N 스토리지 (완료)**: 원작 `TileFluxStorage`(기본·`Herculean`·`Gargantuan` 3종), `FluxStorageHandler`, `BlockFluxStorage`, `IFluxStorage`.
  - 등록 이름(월드 저장에 기록됨, 정한 뒤 변경 금지): 블록 `fluxstorage`, `herculeanfluxstorage`, `gargantuanfluxstorage` /
    타일 `fluxnetworks:FluxStorage`, `fluxnetworks:HerculeanFluxStorage`, `fluxnetworks:GargantuanFluxStorage`
  - 용량·전송량은 이미 있는 `FluxConfig.basicCapacity` 등(EU, EU/t)을 쓴다. 에너지는 GT 기계와 직접 주고받지 않는다 (원작처럼 네트워크 버퍼 전용, `IEnergyConnected` 구현 안 함)
  - 우선순위 `PRI_DIFF`·`PRI_UPPER`, 4틱마다 에너지 동기화, 부숴도 아이템 NBT(`"energy"`)에 에너지 보존
  - `ItemFluxConnector` 툴팁 에너지 표시, `FluxUtils` 아이콘의 스토리지 부분, `TileEntityHandler`·`RegistryBlocks` 등록, GUI 홈 화면의 스토리지 표시 확인
  - 원작 모양 렌더링(`TileFluxStorageRenderer` 게이지)은 R에서 한다. N에서는 단순 아이콘
  - 결과: `FluxStorageHandler`(NBT 키 상수 `NBT_ENERGY`), `TileFluxStorage`(+`Herculean`/`Gargantuan` 내부 클래스), `BlockFluxStorage`(`getMaxStorage()`),
    텍스처 `fluxstorage1~3_on/off.png`(원작 `model/tierN`, `tierN_off` 복사). `IFluxStorage`가 `IFluxPlug`·`IFluxPoint`를 상속하므로
    EU getter는 쓰이지 않는 고정값(MAX 티어, 1A)을 돌려준다 (`// 원작과 다름`). 아이템 툴팁은 "저장량 / 용량"
- **O 컨트롤러 (완료)**: 원작 `TileFluxController`, `BlockFluxController`, `FluxControllerHandler`.
  - 등록 이름: 블록 `fluxcontroller` / 타일 `fluxnetworks:FluxController`
  - 네트워크당 컨트롤러 1개 (원작 `FluxUtils`·`PacketTileHandler`의 `HAS_CONTROLLER` 피드백). 같은 검사를 구성기 붙여넣기에도 적용
  - 핸들러는 요청량 0인 껍데기만 둔다. 플레이어 충전(`chargeAllItems`, 바우블)은 `// TODO 4단계:`로 남긴다. GUI 무선 탭은 계속 숨긴다
  - 결과: `FluxControllerHandler`(4단계 TODO는 클래스 주석), `TileFluxController`(`TileFluxCore` 직접 상속, EU getter는 고정값 `// 원작과 다름`),
    `BlockFluxController`, 텍스처 `fluxcontroller_on/off.png`(원작 `model/` 복사). 구성기 붙여넣기 검사는 `FluxUtils.hasOtherController`
    (다른 컨트롤러가 있으면 네트워크만 건너뛰고 나머지 설정은 붙여넣음, 채팅으로 `hascontroller` 안내)
- **P 플럭스 아이템·블록 (완료)**: 원작 `ItemFlux`(Flux), `ItemCore`(FluxCore), `BlockCore`(FluxBlock, 바위 재질), `EntityFireItem`,
  `CommonProxy`의 흑요석 좌클릭 제작(`enableFluxRecipe`)과 `onEntyAdded`(`enableOldRecipe`).
  - 등록 이름: 아이템 `flux`, `fluxcore` / 블록 `fluxblock`, 엔티티 `"Flux"`(ID 0)
  - 일반 제작대 레시피와 GT 레시피는 5단계. 이 배치는 원작의 월드 안 제작 방식만
  - 결과: `ItemCore`, `ItemFlux`(툴팁), `BlockCore`(`Material.rock`, 원작처럼 경도 0), `EntityFireItem`(1.7.10은 불 면역이면 불 피해 자체가 안 오므로
    면역을 끄고 `attackEntityFrom`에서 불 피해를 막으며 `inFire` 때 변환, `// 원작과 다름`). `CommonProxy.onPlayerInteract`(`LEFT_CLICK_BLOCK`, Flux를 최대 스택으로 나눠 떨어뜨림)와
    `onEntityAdded`(청크 로드 중 CME를 피하려고 `scheduleServerTask`로 다음 틱에 교체, `// 원작과 다름`)
- **Q 청크 로딩·일괄 편집 (완료)**: 원작 `FluxChunkManager`, `ForgeChunkManager` 콜백 등록(`CommonProxy`), `FluxNetworkData`의 청크 목록 저장,
  `PacketTileHandler`의 `forceChunk/releaseChunk`, `TileFluxCore.invalidate`의 해제, `PacketBatchEditing` `editions[5]`, 구성기 홈의 청크 로딩 스위치.
  - `ChunkPos` 대신 `ChunkCoordIntPair`. `FluxConfig.enableChunkLoading`이 꺼져 있으면 모두 거부
  - 같은 배치에서 연결 탭의 티어·암페어 일괄 편집(`GuiPopConnectionEdit`, `PacketBatchEditing`, `FluxLiteConnector`에 EU 설정 추가. 원작에 없는 기능)
  - 결과: `FluxChunkManager`(`common/data`, 장치 청크 걸기·풀기는 `setChunkLoading(tile, load)`. 티켓 null 처리, 콜백 `put`, `WorldEvent.Unload` 때 티켓 캐시 제거 `// 원작과 다름`).
    단일 패킷은 스토리지를 서버에서도 거부하고 이미 로딩 중이면 그대로 둔다(`// 원작과 다름`). 구성기 설정 `FluxConfigurationType.CHUNK_LOADING`(키 `"chunk"`, 스위치 y=114, 스토리지는 복사·붙여넣기 안 함, 로더가 이미 있으면 조용히 꺼진 채).
    `PacketBatchEditing` editions 9개(7 티어, 8 출력 암페어, 태그 `"tier"`·`"oa"`). `FluxLiteConnector`에 `tier`·`amperage`(NBT `"mt"`/`"ot"`/`"oa"`, `getEUTier`/`getEUAmperage`).
    티어 선택 칸은 `GuiDraw.drawTierSelector`/`cycleTier`/`formatTier` 공용. 번역 키 `flux.tier`
- **R 원작 모양 렌더링·네트워크 색상**: ISBRH(`ISimpleBlockRenderingHandler`)로 원작 모양(플러그 0.25~0.75, 포인트 0.375~0.625, 컨트롤러·스토리지)과
  경계 상자, 원작 `BlockSidedConnection`의 면 연결 표시, 스토리지 TESR 에너지 게이지와 아이템 렌더러(`IItemRenderer`),
  네트워크 색 입히기(블록 `colorMultiplier`, 구성기 2레이어 아이콘. 원작 `FluxColorHandler`의 색 부분, `ClientProxy` TODO).
  - 범위가 크다. 코드를 쓰기 전에 파일 목록과 순서를 보여주고 승인을 받는다
  - 결과 (완료): `client/render/`에 `FluxModelRenderer`(JSON 요소를 Tessellator로, 면별 UV, Y축 45°, submodel 방향 회전, 월드는 면 음영·아이템은 법선),
    `FluxBlockModels`(원작 JSON 5개를 상수로), `BlockFluxRenderer`(ISBRH, `connections[i] == 1`인 면에 연결 팔), `TileFluxStorageRenderer`(원작 TESR),
    `ItemFluxDeviceRenderer`(장치 아이템 6개 `IItemRenderer`. 플러그·포인트는 여섯 면 팔, 스토리지는 NBT `"energy"` 게이지).
    블록은 텍스처 칸 `BlockFluxCore.TEX_0/TEX_1/TEX_TOP/TEX_COLOUR`마다 켜짐·꺼짐 아이콘을 정한다(`setModelTexture`). `TEX_COLOUR`가 원작 tintindex 1.
    렌더 ID는 `BlockFluxCore.renderId`(ClientProxy가 넣음). 텍스처는 `textures/blocks/model/`(원작 `model/` 그대로), N·O의 단순 아이콘은 삭제.
    색은 `FluxColorHandler.getBlockColor/getItemColor/getConfiguratorColor`. 구성기는 2패스(`layer0`, `layer1`에 색), 관리자 구성기는 1패스.
    GUI 표시 아이템(`FluxUtils.getDisplayStack`)에 `GUI_COLOR` 태그(원작 `FluxGuiStack`). `TileFluxCore.onDataPacket`은 켜짐·색·면 연결이 바뀔 때만 다시 그림(`// 원작과 다름`)
  - 손에 든 아이템은 `EQUIPPED_BLOCK` 헬퍼로 Forge가 -0.5 이동한다 (게임 테스트 후 수정)

### 4단계 배치

컨트롤러가 네트워크 에너지로 멤버 플레이어의 전기 아이템을 충전한다. 원작 `FluxControllerHandler`, `GuiTabWireless`, `WirelessType`.
코드에 남은 `// TODO 4단계:` (`FluxControllerHandler` 클래스 주석, `GuiTabCore` 2곳, `InventoryButton`)를 배치마다 처리한다.
이미 있는 것: `WirelessType`, `NETWORK_WIRELESS` 설정과 저장(`wirelessMode`), `PacketGeneralType.CHANGE_WIRELESS`(편집 권한 확인), 번역 키 `tab.wireless`·`flux.wireless`·`slot.*`, `InventoryButton`.

**S를 시작하기 전에 사용자 승인을 받을 것 (금지 사항과 충돌)**: GTNH의 전기 아이템(GT 배터리·도구·전기 갑옷, IC2·GT++ 아이템)은 모두
IC2 `IElectricItem` 기반이고, GT5U도 `GTModHandler.chargeElectricItem` → `ic2.api.item.ElectricItem.manager.charge`로 충전한다.
EU이므로 "IC2 에너지 금지"의 예외로 **아이템 충전에 한해** 다음 중 하나를 허용할지 정한다.
- (권장) `ic2.api.item.ElectricItem.manager.charge/getCharge`와 `IElectricItem`을 헬퍼 한 곳에서만 직접 쓴다. GT5U 의존성에 IC2 API가 이미 딸려 온다
- `GTModHandler.chargeElectricItem`만 쓴다. 반환값에 이상한 보정(`rCharge * 4 > aTier ? aTier : 0`)이 있고 `int`라 큰 값을 나눠 불러야 한다

- **S 아이템 충전 헬퍼**: 원작 `IItemEnergyHandler`/`ItemEnergyHandler` 대신 `common/core/ItemEnergy`(가칭) 하나.
  `canCharge(stack)`, `charge(stack, long eu, boolean simulate)` → 실제로 넣은 EU. `int` 범위로 잘라 부르고, 아이템 티어 제한은 무시하되(`Integer.MAX_VALUE`)
  아이템 전송 한도는 지킨다. 원작은 4틱마다 충전하므로 한 번에 "한도 × 4"까지 넣는 보정을 검토한다(배치에서 결정해 기록).
  `isElectricItem`이 아닌 아이템(RF 아이템 등)은 무시
- **T 충전 핸들러**: `FluxControllerHandler`를 원작 알고리즘대로 옮긴다 (`onCycleStart` 타이머·`demand`, `sendToConsumers`, `onCycleEnd` `timer = ++timer & 0x1f`, `reset`).
  - 원작 `sendToConsumers(energy, simulate)`가 현재 `BasicPointHandler.sendToConsumers(long)` 구조와 다르다. 전송 사이클(확정 설계 4)은 바꾸지 말고 핸들러 안에서 맞춘다
  - 정적 `CHARGING_PLAYERS`는 `HashSet`(fastutil 금지). `EntityPlayerMP` 대신 UUID로 두거나, 로그아웃·서버 종료 때 비운다(원작은 죽은 플레이어 참조가 남을 수 있음, `// 원작과 다름`)
  - 플레이어 조회는 `MinecraftServer.getServer().getConfigurationManager()`에서 UUID로 찾는다 (`playerEntityList` 순회)
  - 슬롯: 오른손 = `inventory.getCurrentItem()`, 핫바 0~8(든 아이템 제외), 갑옷 `armorInventory`. **1.7.10에는 왼손(오프핸드)이 없다** → `LEFT_HAND`는 무시
  - 충전한 아이템은 `inventoryContainer.detectAndSendChanges()`가 동기화하는지 게임에서 확인
  - 확인 방법: `/fluxdebug`가 없으므로 GUI 통계 탭의 컨트롤러 전송량과 아이템 충전 표시로 본다
- **U 무선 GUI 탭**: 원작 `GuiTabWireless`를 그대로 옮기고 `GuiTabCore`의 숨김을 푼다. `InventoryButton`에 탭의 `settings` 배열을 넘긴다.
  왼손 버튼은 1.7.10에 없으므로 숨기거나 누를 수 없게 하고 `// 원작과 다름`. 적용 비트 계산과 `CHANGE_WIRELESS` 패킷은 원작 그대로.
  바우블 버튼은 V 전까지 바우블 모드가 없으면 누를 수 없게 둔다
- **V 바우블**: GTNH의 Baubles(Baubles-Expanded) API를 `compileOnly`로 추가하고(GTNH 2.9.0 매니페스트의 버전 사용), 모드가 있을 때만
  `BaublesApi.getBaubles(player)`(`IInventory`) 슬롯을 충전한다. 모드 확인은 `Loader.isModLoaded("Baubles")`로 하고 바우블 클래스는 별도 클래스에서만 import한다(없을 때 `NoClassDefFoundError` 방지)

## 작업 방식

- 배치 하나를 끝내면 `./gradlew build` 성공을 확인하고 바뀐 점을 요약한다. 커밋과 게임 안 테스트는 사용자가 한다
- 범위가 큰 배치(K, L, N, R)는 코드를 쓰기 전에 파일 목록과 순서를 계획으로 먼저 보여주고 승인을 받는다
- 배치 범위 밖 기능은 지우지 말고 `// TODO <단계 또는 배치>:` 주석으로 남긴다 (원작 동작 한 줄 요약 포함)
- 끝나면 사용자가 게임에서 확인할 절차를 짧게 적어준다
- 사용자가 따로 말하지 않고 다음 배치를 진행하라고 하면, 이전 배치는 게임 테스트를 통과한 것으로 보고 진행 상태 표에 그렇게 적는다

## 금지 사항 (1.12 코드가 섞여 들어오기 쉽다)

- `BlockPos`, `IBlockState`, `EnumFacing`, `ChunkPos`, `Capability`/`@CapabilityInject`, `ITickable`, JSON 모델·blockstate 금지
- `net.minecraftforge.fml.*` 금지 → `cpw.mods.fml.*`
- `it.unimi.dsi.fastutil.*`, `org.apache.commons.io.*` 금지
- Java 9 이상 API 금지 (`List.of`, `Map.of`, `String.isBlank`, `Optional.isEmpty` 등)
- `EnergyType`, RF·FE·IC2 에너지 코드 금지 (4단계 아이템 충전의 예외는 S 시작 때 결정). GT 쪽은 `IEnergyConnected`, `GTValues`(→ `GTEnergy` 헬퍼), `GregTechAPI.causeCableUpdate`, `BaseMetaPipeEntity`만 쓴다
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
5. **타일 구조**: `TileFluxCore` → `TileFluxConnector`(GT `IEnergyConnected` 구현, 색 -1) → `TileFluxPlug` / `TileFluxPoint`.
   `TileFluxStorage`, `TileFluxController`는 `TileFluxCore`를 바로 상속한다 (GT와 연결하지 않음)
6. **EU 설정**: 플러그 `maxInputTier`(NBT `"mt"`), 포인트 `outputTier`(`"ot"`), `outputAmperage`(`"oa"`). 티어 0(ULV)~14(MAX).
   값을 바꿀 때는 반드시 setter(`setMaxInputTier`, `setOutputTier`, `setOutputAmperage`)를 쓴다 (보정과 동기화 포함)
7. **GT 케이블 갱신**: `TileFluxConnector.notifyGTCables()`가 인접 GT 케이블 좌표로 `GregTechAPI.causeCableUpdate`를 호출한다.
   첫 틱, 25틱 뒤, `invalidate` 때. GT 케이블 연결을 강제로 끊었다 잇는 방식은 쓰지 않는다
8. **등록 이름 (월드 저장에 기록됨, 변경 금지)**: 블록 `fluxblock`, `fluxplug`, `fluxpoint`, `fluxstorage`, `herculeanfluxstorage`, `gargantuanfluxstorage`, `fluxcontroller` /
   타일 `fluxnetworks:FluxPlug`, `fluxnetworks:FluxPoint`, `fluxnetworks:FluxStorage`, `fluxnetworks:HerculeanFluxStorage`, `fluxnetworks:GargantuanFluxStorage`, `fluxnetworks:FluxController` /
   아이템 `flux`, `fluxcore`, `fluxconfigurator`, `adminconfigurator` / 엔티티 `"Flux"`(ID 0)
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
- `PacketBatchEditing` editions: 0 이름, 1 우선순위, 2 한도, 3 서지, 4 한도 해제, 5 청크 로딩, 6 연결 해제, 7 티어, 8 출력 암페어 (7·8은 배치 Q에서 추가)

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
