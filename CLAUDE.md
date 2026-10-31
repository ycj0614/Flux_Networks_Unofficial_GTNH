# Flux Networks GTNH (1.7.10 백포트)

Flux Networks(SonarSonic, 1.12.2-refactored 브랜치, MIT, Copyright (c) 2019 BloCamLimb)를
Minecraft 1.7.10 / Forge 10.13.4.1614로 옮기는 프로젝트다. **GTNH 전용**이며 에너지는
**GregTech 5 Unofficial(GT5U)의 EU만** 다룬다. RF·FE·IC2 에너지는 지원하지 않는다.

- 패키지 `com.ycj0614.fluxnetworks` (원작 `sonar.fluxnetworks`의 하위 구조 유지), 모드 ID `fluxnetworks`
- 의존성: `com.github.GTNewHorizons:GT5-Unofficial:5.09.54.264:dev` (GTNH 2.9.0 정식, 배치 Z에서 205 → 264)
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
git clone --depth 1 --filter=blob:none --sparse --branch 5.09.54.264 https://github.com/GTNewHorizons/GT5-Unofficial.git ../reference/GT5-Unofficial
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
| 4단계 컨트롤러 무선 충전 | S 아이템 충전 헬퍼 / T 충전 핸들러 / U 무선 GUI 탭 / V 바우블 | 완료, 게임 테스트 통과 |
| 5단계 마무리·연동 | W 레시피 / X NEI / Y WAILA / Z 배포 | W·X·Y 완료·게임 테스트 통과, Z 완료(게임 테스트 대기) |

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
코드에 남았던 `// TODO 배치 V:`(`FluxControllerHandler` 클래스 주석, `GuiTabWireless` 바우블 버튼, `WirelessType.BAUBLES`)는 V에서 모두 처리했다.
이미 있는 것: `WirelessType`, `NETWORK_WIRELESS` 설정과 저장(`wirelessMode`), `PacketGeneralType.CHANGE_WIRELESS`(편집 권한 확인), 번역 키 `tab.wireless`·`flux.wireless`·`slot.*`, `InventoryButton`.

**IC2 아이템 API 예외 (S에서 결정, 승인됨)**: GTNH의 전기 아이템(GT 배터리·도구·전기 갑옷, IC2·GT++ 아이템)은 모두
IC2 `IElectricItem` 기반이고, GT5U도 `GTModHandler.chargeElectricItem` → `ic2.api.item.ElectricItem.manager.charge`로 충전한다.
EU이므로 "IC2 에너지 금지"의 예외로 **아이템 충전에 한해** `ic2.api.item.ElectricItem.manager.charge/getCharge`와 `IElectricItem`을
`common/core/ItemEnergy` 한 곳에서만 직접 쓴다 (GT5U 의존성에 IC2 API가 딸려 옴). `GTModHandler.chargeElectricItem`은 반환값 보정과 `int` 때문에 쓰지 않는다.

- **S 아이템 충전 헬퍼**: 원작 `IItemEnergyHandler`/`ItemEnergyHandler` 대신 `common/core/ItemEnergy`(가칭) 하나.
  `canCharge(stack)`, `charge(stack, long eu, boolean simulate)` → 실제로 넣은 EU. `int` 범위로 잘라 부르고, 아이템 티어 제한은 무시하되(`Integer.MAX_VALUE`)
  아이템 전송 한도는 지킨다. 원작은 4틱마다 충전하므로 한 번에 "한도 × 4"까지 넣는 보정을 검토한다(배치에서 결정해 기록).
  `isElectricItem`이 아닌 아이템(RF 아이템 등)은 무시
  - 결과 (완료): `ItemEnergy.canCharge(stack)`(전기 아이템이고 가득 차지 않음), `ItemEnergy.charge(stack, eu, simulate)` → 넣은 EU(`long`, 소수점 버림).
    한 번에 `getTransferLimit × CHARGE_INTERVAL(4)`까지 넣는다(틱당 전송 한도, `// 원작과 다름`). `manager.charge(stack, 양, Integer.MAX_VALUE, true, simulate)`로
    티어는 무시하고 한도는 직접 계산. `ElectricItem.manager`가 `ISpecialElectricItem`을 처리하는 게이트웨이라 따로 나누지 않는다
- **T 충전 핸들러**: `FluxControllerHandler`를 원작 알고리즘대로 옮긴다 (`onCycleStart` 타이머·`demand`, `sendToConsumers`, `onCycleEnd` `timer = ++timer & 0x1f`, `reset`).
  - 원작 `sendToConsumers(energy, simulate)`가 현재 `BasicPointHandler.sendToConsumers(long)` 구조와 다르다. 전송 사이클(확정 설계 4)은 바꾸지 말고 핸들러 안에서 맞춘다
  - 정적 `CHARGING_PLAYERS`는 `HashSet`(fastutil 금지). `EntityPlayerMP` 대신 UUID로 두거나, 로그아웃·서버 종료 때 비운다(원작은 죽은 플레이어 참조가 남을 수 있음, `// 원작과 다름`)
  - 플레이어 조회는 `MinecraftServer.getServer().getConfigurationManager()`에서 UUID로 찾는다 (`playerEntityList` 순회)
  - 슬롯: 오른손 = `inventory.getCurrentItem()`, 핫바 0~8(든 아이템 제외), 갑옷 `armorInventory`. **1.7.10에는 왼손(오프핸드)이 없다** → `LEFT_HAND`는 무시
  - 충전한 아이템은 `inventoryContainer.detectAndSendChanges()`가 동기화하는지 게임에서 확인
  - 확인 방법: `/fluxdebug`가 없으므로 GUI 통계 탭의 컨트롤러 전송량과 아이템 충전 표시로 본다
  - 결과 (완료): `FluxControllerHandler`가 원작 타이머 그대로 동작한다(`timer == 0` 대상 갱신, `& 3 == 2` 시뮬레이션 demand, `& 3 == 0` 실제 충전).
    `sendToConsumers`에서 `min(버퍼, getLogicLimit())`로 자른다(원작은 `BasicPointHandler`에서 자름). `WirelessHandler` 대신 슬롯 그룹별 루프(오른손 → 핫바(든 아이템 제외) → 갑옷).
    `CHARGING_PLAYERS`는 `HashSet<UUID>`(`getGameProfile().getId()`), 핸들러는 `EntityPlayerMP` 목록을 32틱마다 다시 만들고 `reset` 때 비운다.
    `CommonProxy.onServerStopped`에서 `clearChargingPlayers()`. 바우블은 `// TODO 배치 V:`. U 전에는 무선을 켤 GUI가 없어 게임 확인은 U와 함께 한다
- **U 무선 GUI 탭**: 원작 `GuiTabWireless`를 그대로 옮기고 `GuiTabCore`의 숨김을 푼다. `InventoryButton`에 탭의 `settings` 배열을 넘긴다.
  왼손 버튼은 1.7.10에 없으므로 숨기거나 누를 수 없게 하고 `// 원작과 다름`. 적용 비트 계산과 `CHANGE_WIRELESS` 패킷은 원작 그대로.
  바우블 버튼은 V 전까지 바우블 모드가 없으면 누를 수 없게 둔다
  - 결과 (완료): `client/gui/tab/GuiTabWireless`(원작 배치·적용 비트·`CHANGE_WIRELESS` 그대로). `GuiTabCore.setDefaultTabs`는 원작처럼 모든 탭.
    왼손 칸은 넣지 않는다(`// 원작과 다름`, 비트는 서버 값을 그대로 돌려보냄). 바우블 칸은 `clickable = false`(`// TODO 배치 V:`).
    `InventoryButton`은 생성자로 탭의 `settings` 배열을 받고, 누를 수 없는 칸은 회색(0x606060)으로 그린다
- **V 바우블**: GTNH의 Baubles(Baubles-Expanded) API를 `compileOnly`로 추가하고(GTNH 2.9.0 매니페스트의 버전 사용), 모드가 있을 때만
  `BaublesApi.getBaubles(player)`(`IInventory`) 슬롯을 충전한다. 모드 확인은 `Loader.isModLoaded("Baubles")`로 하고 바우블 클래스는 별도 클래스에서만 import한다(없을 때 `NoClassDefFoundError` 방지)
  - 결과 (완료): `compileOnly("com.github.GTNewHorizons:Baubles-Expanded:2.2.27-GTNH:dev")`(GTNH 2.9.0 매니페스트. GT5U를 통해 런타임에도 딸려 옴).
    Baubles-Expanded의 modid도 `"Baubles"`(`"Baubles|Expanded"`는 보조 모드). `CommonProxy.baublesLoaded`(preInit, 원작과 같은 자리)를 서버 핸들러와 GUI가 같이 쓴다.
    `common/integration/BaublesIntegration.getBaubles(player)`만 `baubles.*`를 import하고 `null`일 수 있다(API가 리플렉션 실패 시 null).
    `FluxControllerHandler`는 갑옷 다음에 바우블 슬롯을 충전하고, 스택 NBT만 고친다(`setInventorySlotContents`는 장착 해제·장착 이벤트를 불러서 안 씀).
    그래서 바우블 창을 열기 전에는 클라이언트 충전 표시가 늦을 수 있다. GUI 바우블 칸은 `clickable = baublesLoaded`(`// 원작과 다름`)

### 5단계 배치

범위는 확정됐다 (승인됨). 순서는 W → X → Y → Z (X가 W의 레시피를 확인한다). 배치마다 따로 사전 승인을 받지 않고, 끝나면 결과를 이 절에 적는다.
코드에 남은 `// TODO 5단계:`를 검색해 해당 배치에서 처리한다.

- 다른 모드 연동은 모두 선택 의존성이다. `Loader.isModLoaded`로 확인하고, 그 모드의 클래스는 `common/integration/`(클라이언트 전용이면 `client/integration/`)의
  별도 클래스에서만 import한다. **NEI(2.8.155-GTNH)와 WAILA(1.19.34)는 GT5U를 통해 이미 compileClasspath에 있으므로 `compileOnly`를 추가하지 않는다**
  (2.9.0 매니페스트 `https://raw.githubusercontent.com/GTNewHorizons/DreamAssemblerXXL/master/releases/manifests/2.9.0.json`은 NEI 2.8.171, WAILA 1.19.34)
- **W 레시피**: `common/registry/RegistryRecipes`. 제작대와 GT 레시피를 **둘 다** 넣고 설정으로 고른다.
  - 기본은 원작 제작대 레시피 그대로 (코어 4개, 구성기, 플럭스 블록, 컨트롤러, 포인트, 플러그, 스토리지 3종 승급, 장치 단독 재조합으로 NBT 초기화).
    원작 `FluxShapedRecipe`·`FluxShapelessRecipe`·`FluxStorageRecipe`(`ShapedOreRecipe`/`ShapelessOreRecipe` 하위)를 쓰고, 1.7.10은 `RecipeSorter.register`가 필요하다.
    스토리지 승급은 원작처럼 재료의 에너지 합(`FluxStorageHandler.NBT_ENERGY`)과 첫 네트워크 ID를 이어받는다
  - `FluxConfig` 토글(이름은 배치에서 정함)을 켜면 제작대 레시피 대신 GT 기계 레시피(GT5U `GTValues.RA` 빌더, 조립기 등)를 등록한다. 재료·티어는 배치 시작 때 표로 제안한다
  - 원작 `enableFluxRecipe`·`enableOldRecipe`(P에서 구현)는 그대로 둔다
  - 결과 (완료): `RegistryRecipes.register()`를 `CommonProxy.postInit`에서 부른다. 설정 `FluxConfig.enableGTRecipes`("Enable GT Recipes", GENERAL, 기본 true).
    GT 모드(조립기): 코어×4 = 플럭스 4·흑요석 판 4·엔더 눈(MV 10초), 플럭스 블록 = 플럭스 4·코어 4(MV 5초), 구성기 = 플럭스 2·흑요석 막대 2·엔더 진주·MV 회로(MV 10초),
    플러그 = HV 외피·코어 4·HV 센서·HV 회로 2(HV 20초), 포인트 = HV 외피·코어 4·HV 발신기·HV 회로 2(HV 20초),
    컨트롤러 = EV 외피·플럭스 블록 2·코어·HV 역장 발생기·EV 회로 2(EV 30초). 스토리지는 두 모드 모두 제작대(`FluxStorageRecipe`)이고
    GT 모드에서는 가운데 줄이 `circuitAdvanced`/`circuitData`/`circuitElite`(HV/EV/IV), 제작대 모드는 원작 `paneGlassColorless`.
    장치 단독 재조합(NBT 초기화)은 두 모드 공통. 레시피 클래스 3종은 `RecipeSorter.register`(`fluxnetworks:shaped`/`storage`/`shapeless`)
- **X NEI**: `client/integration/nei/`에 `IConfigureNEI` 구현과 `TemplateRecipeHandler`. 원작 JEI `FluxCraftingCategory`(흑요석 위 레드스톤 → 좌클릭 → 플럭스)를 옮기고
  텍스처 `gui_creating_flux_recipe.png`를 원작에서 복사한다. W의 레시피(`ShapedOreRecipe` 하위)가 NEI 기본 핸들러에 보이는지 확인
  - 결과 (완료): `NEIFluxNetworksConfig`(NEI가 이름으로 찾아 불러옴, `enableFluxRecipe`일 때만 핸들러·카탈리스트(플럭스) 등록)와
    `FluxCraftingHandler`(핸들러 ID `"flux"`, 원작 좌표를 너비 166 가운데로 19px 이동, 높이 80). 크기·탭 아이콘은 플러그인 로딩보다 먼저 처리돼야 해서
    `ClientProxy.init`이 `Loader.isModLoaded("NotEnoughItems")`를 확인하고 IMC `registerHandlerInfo`로 보낸다(`sendHandlerInfo`).
    애니메이션은 블록 `renderBlockAsItem`, 아이템 `renderItemIn2D`, 타이머는 시스템 시간 3초 주기(`// 원작과 다름`). 애니메이션 영역은 전송 영역(누르면 전체 레시피)이기도 하다.
    텍스처·번역 키(`info.fluxnetworks.jei.*`)는 이미 있었다. W의 레시피 클래스는 `ShapedOreRecipe`/`ShapelessOreRecipe` 하위라 NEI 기본 핸들러가 `instanceof`로 잡는다(게임에서 확인)
- **Y WAILA**: 원작 `TOPIntegration`을 `common/integration/`의 WAILA 프로바이더로 옮긴다 (IMC `"Waila"`, `"register"`).
  - 데이터는 서버 `getNBTData`로 보낸다 (멀티플레이 정확도). 표시: 네트워크 이름, 전송량, 버퍼(스토리지는 저장량), 한도, 우선순위·서지, 청크 로딩,
    EU 티어·암페어(원작에 없음, `// 원작과 다름`). 머리 줄은 장치 표시 이름(원작 TOP의 display override)
  - 원작 `enableOneProbe*` 설정 대신 WAILA 자체 설정(`IWailaRegistrar.addConfig`)을 쓴다 (`// 원작과 다름`)
  - `ConnectionTransfer`의 `// TODO 5단계:`(인접 장치 표시용 ItemStack)는 추가하지 않는다. 원작 GUI도 쓰지 않았으므로 TODO만 지운다
  - 결과 (완료): `common/integration/WailaIntegration`(head·body·NBT 프로바이더, 대상 `BlockFluxCore`). `CommonProxy.init`이 `Loader.isModLoaded("Waila")`일 때
    IMC로 `...WailaIntegration.register`를 문자열로 보낸다. 서버 `getNBTData`가 하위 태그 `"fluxnetworks"`에 이름·네트워크·종류·전송량·버퍼·한도·우선순위·서지·청크 로딩·티어·암페어를 담는다.
    스토리지가 `IFluxPlug`·`IFluxPoint`를 둘 다 구현하므로 EU 값은 연결 종류로 고른다. 머리 줄은 사용자 지정 이름이 있을 때만 바꾼다(`// 원작과 다름`).
    설정 `fluxnetworks.basic`/`advanced`/`sneaking`(기본 모두 true, 원작 TOP 설정 3개와 같은 의미, 문구 `option.fluxnetworks.*`는 en_US만).
    웅크리면 버퍼를 전체 숫자로 표시. 티어 표시는 `GuiDraw.formatTier`와 같은 형식을 따로 둔다(GuiDraw는 클라이언트 전용). `ConnectionTransfer` TODO 삭제
- **Z 배포**: 버전·`mcmod.info`(현재 템플릿 예제 문구 그대로, 원작자 크레딧)·README·LICENSE(원작 MIT 고지, `LICENSE-template` 대체).
  GT5U를 2.9.0 매니페스트의 **5.09.54.264**로 올리고 빌드·게임 테스트를 다시 한다. 배포 jar를 바우블 없는 환경에서도 실행해 확인 (V의 회색 바우블 칸 확인 포함)
  - `GuiTabMembers`의 `// TODO 5단계:`(이름으로 멤버 추가)는 **넣지 않는다**. 원작에서도 주석 처리된 기능이라는 설명 주석으로 바꾼다
  - `SuperAdminData`의 `// TODO 확인:`(ops.json 플레이어별 레벨)은 SRG 이름을 빌드로 확인해 원작처럼 바꿀지 정하고 TODO를 지운다
  - 결과 (완료): GT5U 5.09.54.264로 올려도 코드 수정 없이 빌드됨. **바우블 없는 환경은 만들 수 없다**: 런타임에서 Baubles-Expanded를 빼고 `runServer`를 돌리면
    GT5U(`ItemPowerGoggles`가 `baubles.api.IBauble` 사용)와 ModularUI2가 preInit에서 `NoClassDefFoundError`로 멈춘다(Flux Networks는 gregtech 오류로 건너뜀).
    그래서 GTNH에는 항상 바우블이 있고, V의 회색 바우블 칸·`baublesLoaded == false` 경로는 방어 코드로만 남긴다. 테스트 스위치는 넣지 않았다.
    버전은 GTNH 빌드 스크립트가 **git 태그**로 정한다(`mcmod.info`의 `${modVersion}`, `Tags.VERSION`). 첫 배포 태그는 `1.0.0`(사용자가 커밋 후 붙임).
    `mcmod.info`(설명·저장소 URL·작성자 ycj0614·원작 크레딧·`requiredMods` gregtech), README(한국어 소개·연동·설정·크레딧), `LICENSE-template` → `LICENSE`(원작 고지 아래 포트 저작권 줄),
    템플릿의 `CODEOWNERS`(@GTNewHorizons/admin) 삭제. `SuperAdminData.canActivateSuperAdmin`은 원작처럼 ops.json의 플레이어별 level 비교
    (`func_152603_m().func_152683_b(profile)` → `UserListOpsEntry.func_152644_a()`, OP가 아니면 0). `GuiTabMembers` TODO는 설명 주석으로 바꿈
- 원작의 `MekanismIntegration`, `oc`(OpenComputers), `energy`(RF·FE 등)는 옮기지 않는다 (EU 전용 원칙)

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
- `EnergyType`, RF·FE·IC2 에너지 코드 금지 (예외: 아이템 충전용 IC2 아이템 API는 `ItemEnergy`에서만). GT 쪽은 `IEnergyConnected`, `GTValues`(→ `GTEnergy` 헬퍼), `GregTechAPI.causeCableUpdate`, `BaseMetaPipeEntity`만 쓴다
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
