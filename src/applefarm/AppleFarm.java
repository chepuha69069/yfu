package applefarm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.class_124;
import net.minecraft.class_1268;
import net.minecraft.class_1269;
import net.minecraft.class_1297;
import net.minecraft.class_1657;
import net.minecraft.class_1661;
import net.minecraft.class_1713;
import net.minecraft.class_1735;
import net.minecraft.class_1792;
import net.minecraft.class_1794;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_2561;
import net.minecraft.class_2350;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_3481;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_5250;
import net.minecraft.class_9279;
import net.minecraft.class_9334;

/**
 * AppleFarm - автономная версия (без Releon Client).
 *
 * Цикл: посадить саженец -> удобрить костной мукой -> срубить выросшее дерево
 * (листва, потом брёвна) -> при необходимости починить мотыгу пузырьками опыта.
 */
public final class AppleFarm {

    public static final AppleFarm INSTANCE = new AppleFarm();
    private static final class_310 mc = class_310.method_1551();


    private static final String MINE_TARGETS_LEAVES =
        "oak_leaves spruce_leaves birch_leaves jungle_leaves acacia_leaves dark_oak_leaves cherry_leaves mangrove_leaves";
    private static final String MINE_TARGETS_LOGS =
        "oak_log spruce_log birch_log jungle_log acacia_log dark_oak_log cherry_log mangrove_log";
    private static final int OFFHAND_SLOT_ID = 45;
    private static final String REPAIR_BOTTLE_LORE_MARKER = "полностью ремонтирует";
    private static final String MODE_STATIC = "На месте";
    private static final String MODE_BARITONE = "Baritone";

    // ---------- настройки (порядок и названия как в оригинале) ----------
    private final Setting.Mode breakMode = new Setting.Mode("Режим рубки", "Движок рубки дерева",
        MODE_STATIC, MODE_STATIC, MODE_BARITONE);
    private final Setting.Flag autoStop = new Setting.Flag("Авто-стоп", "Останавливать Baritone после рубки", true)
        .visibleWhen(() -> this.breakMode.is(MODE_BARITONE));
    private final Setting.Num actionDelayMs = new Setting.Num("Задержка действий (мс)", "Задержка между действиями фермы", 1, 0, 500);
    private final Setting.Num commandDelayMs = new Setting.Num("Задержка команд (мс)", "Задержка между командами #mine", 600, 0, 2000);
    private final Setting.Num breakStepDelayMs = new Setting.Num("Задержка ломания (мс)", "Задержка между шагами локального ломания", 1, 0, 250);
    private final Setting.Num toolSwitchDelayMs = new Setting.Num("Задержка смены инструмента (мс)", "Минимальная задержка между сменой инструмента", 80, 0, 1000);
    private final Setting.Num scanRadius = new Setting.Num("Радиус сканирования", "Радиус поиска дерева", 6, 1, 10);
    private final Setting.Num scanHeight = new Setting.Num("Высота сканирования", "Высота поиска дерева", 20, 1, 40);
    private final Setting.Num maxDistance = new Setting.Num("Макс. дистанция", "Максимальная дистанция от точки фермы", 5, 1, 12);
    private final Setting.Num breakRange = new Setting.Num("Дистанция ломания", "Максимальная дистанция локального ломания", 5.5f, 1.0f, 6.0f);
    private final Setting.Num rotateStepYaw = new Setting.Num("Поворот Yaw", "Шаг поворота по Yaw за тик", 180.0f, 1.0f, 180.0f);
    private final Setting.Num rotateStepPitch = new Setting.Num("Поворот Pitch", "Шаг поворота по Pitch за тик", 180.0f, 1.0f, 180.0f);
    private final Setting.Num aimToleranceDeg = new Setting.Num("Допуск ломания", "Допуск наведения при ломании (в градусах)", 20.0f, 1.0f, 45.0f);
    private final Setting.Num interactAimToleranceDeg = new Setting.Num("Допуск взаимодействия", "Допуск наведения при установке/взаимодействии (в градусах)", 16.0f, 1.0f, 45.0f);
    private final Setting.Flag autoRepairHoe = new Setting.Flag("Авто-починка мотыги", "Чинит мотыгу ремонтным пузырем опыта при низкой прочности", true);
    private final Setting.Num repairThresholdPercent = new Setting.Num("Порог починки (%)", "Начинать починку, когда прочность мотыги ниже процента", 15.0f, 1.0f, 95.0f);
    private final Setting.Num repairUseDelayMs = new Setting.Num("Задержка починки (мс)", "Задержка между использованием ремонтного пузыря", 500, 100, 5000);

    private final List<Setting> settings = List.of(breakMode, autoStop, actionDelayMs, commandDelayMs, breakStepDelayMs,
        toolSwitchDelayMs, scanRadius, scanHeight, maxDistance, breakRange, rotateStepYaw, rotateStepPitch,
        aimToleranceDeg, interactAimToleranceDeg, autoRepairHoe, repairThresholdPercent, repairUseDelayMs);

    // ---------- таймеры ----------
    private final Stopwatch actionTimer = new Stopwatch();
    private final Stopwatch commandTimer = new Stopwatch();
    private final Stopwatch notifyTimer = new Stopwatch();
    private final Stopwatch toolSwitchTimer = new Stopwatch();
    private final Stopwatch breakTimer = new Stopwatch();
    private final Stopwatch repairTimer = new Stopwatch();

    // ---------- состояние ----------
    private class_2338 farmPos;
    private class_2338 currentBreakTarget;
    private class_2350 currentBreakSide;
    private boolean miningActive;
    private boolean baritoneAvailable;
    private boolean repairingHoe;
    private int repairHoeSourceSlotId = -1;
    private int repairHoeSourceHotbarSlot = -1;

    private boolean enabled;

    private AppleFarm() {
    }

    public List<Setting> settings() {
        return settings;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public void setEnabled(boolean value) {
        if (value == enabled) {
            return;
        }
        enabled = value;
        if (value) {
            activate();
        } else {
            deactivate();
        }
    }

    // ================= включение / выключение =================

    private void activate() {
        miningActive = false;
        currentBreakTarget = null;
        currentBreakSide = null;
        repairingHoe = false;
        repairHoeSourceSlotId = -1;
        repairHoeSourceHotbarSlot = -1;

        actionTimer.reset();
        commandTimer.setElapsed(commandDelayMs.getInt());
        notifyTimer.setElapsed(3000L);
        toolSwitchTimer.setElapsed(toolSwitchDelayMs.getInt());
        breakTimer.setElapsed(breakStepDelayMs.getInt());
        repairTimer.setElapsed(50L);

        baritoneAvailable = isBaritoneInstalled();
        if (isBaritoneMode() && !baritoneAvailable) {
            chat("Baritone not found in runtime. Put Baritone into mods folder or switch Break Mode to Static.");
            setEnabled(false);
            return;
        }

        farmPos = resolveFarmPos();
        if (farmPos == null) {
            chat("Failed to resolve farm point. Look at plantable ground and enable TreeBot again.");
            setEnabled(false);
            return;
        }

        if (isBaritoneMode()) {
            sendBaritone("#set autoTool true");
            sendBaritone("#set allowBreak true");
        }
        chat("TreeBot enabled at " + farmPos.method_23854() + " in " + breakMode.get() + " mode.");
    }

    private void deactivate() {
        if (isBaritoneMode() && baritoneAvailable && miningActive && autoStop.get()) {
            sendBaritone("#stop");
        }
        restoreHoe();
        miningActive = false;
        currentBreakTarget = null;
        currentBreakSide = null;
        farmPos = null;
    }

    // ================= главный тик =================

    /** Вызывается каждый клиентский тик (Fabric END_CLIENT_TICK). */
    public void tick() {
        if (!enabled) {
            return;
        }
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1761 == null) {
            return;
        }
        if (farmPos == null) {
            farmPos = resolveFarmPos();
            if (farmPos == null) {
                return;
            }
        }

        // слишком далеко от точки фермы
        double maxDist = maxDistance.getInt();
        if (mc.field_1724.method_5707(farmPos.method_46558()) > maxDist * maxDist) {
            if (notifyTimer.hasElapsed(2500.0)) {
                chat("TreeBot: too far from farm point, move closer.");
                notifyTimer.reset();
            }
            stopBaritoneIfNeeded();
            return;
        }

        // починка мотыги имеет приоритет
        if (tickHoeRepair()) {
            return;
        }

        // дерево выросло - рубим
        if (hasTreeNearby()) {
            if (isBaritoneMode()) {
                startBaritoneMining();
            }
            breakLocally();
            return;
        }

        currentBreakTarget = null;
        currentBreakSide = null;
        stopBaritoneIfNeeded();

        if (!actionTimer.hasElapsed(actionDelayMs.getInt())) {
            return;
        }
        actionTimer.reset();

        // нет саженца - сажаем
        if (!isSaplingAtFarm()) {
            if (!tryPlantSapling() && notifyTimer.hasElapsed(2500.0)) {
                class_1792 sapling = findSaplingItem();
                if (sapling == class_1802.field_8162) {
                    chat("TreeBot: no sapling in inventory.");
                } else {
                    chat("TreeBot: missing sapling " + sapling.method_63680().getString());
                }
                notifyTimer.reset();
            }
            return;
        }

        // саженец стоит - удобряем
        if (!applyBonemeal() && notifyTimer.hasElapsed(2500.0)) {
            chat("TreeBot: no bone meal, waiting for natural growth.");
            notifyTimer.reset();
        }
    }

    private boolean isBaritoneMode() {
        return breakMode.is(MODE_BARITONE);
    }

    private void stopBaritoneIfNeeded() {
        if (isBaritoneMode() && miningActive && autoStop.get()) {
            sendBaritone("#stop");
            miningActive = false;
        }
    }

    // ================= рубка =================

    private void startBaritoneMining() {
        if (!commandTimer.hasElapsed(commandDelayMs.getInt())) {
            return;
        }
        sendBaritone("#mine 1 " + (hasLeavesNearby() ? MINE_TARGETS_LEAVES : MINE_TARGETS_LOGS));
        miningActive = true;
        commandTimer.reset();
    }

    private void breakLocally() {
        if (!breakTimer.hasElapsed(breakStepDelayMs.getInt())) {
            return;
        }
        if (currentBreakTarget == null || !canBreak(currentBreakTarget)) {
            BreakTarget target = findBreakTarget();
            if (target == null) {
                currentBreakTarget = null;
                currentBreakSide = null;
                return;
            }
            currentBreakTarget = target.pos;
            currentBreakSide = target.side;
        }
        if (currentBreakTarget == null || currentBreakSide == null) {
            return;
        }
        if (!aimAtSide(currentBreakTarget, currentBreakSide)) {
            return;
        }
        selectBestTool(currentBreakTarget);
        mc.field_1761.method_2902(currentBreakTarget, currentBreakSide);
        mc.field_1724.method_6104(class_1268.field_5808);
        breakTimer.reset();
    }

    private boolean isLog(class_2680 state) {
        return state.method_26164(class_3481.field_15475);
    }

    private boolean isLeaves(class_2680 state) {
        return state.method_26164(class_3481.field_15503);
    }

    private boolean canBreak(class_2338 pos) {
        if (pos == null) {
            return false;
        }
        class_2680 state = mc.field_1687.method_8320(pos);
        if (!isLog(state) && !isLeaves(state)) {
            return false;
        }
        class_2350 side = findBreakSide(pos);
        if (side == null) {
            return false;
        }
        currentBreakSide = side;
        double range = breakRange.getFloat();
        return mc.field_1724.method_5707(pos.method_46558()) <= range * range;
    }

    /** Ближайший блок для ломания: сначала листва, если её нет - брёвна. */
    private BreakTarget findBreakTarget() {
        List<BreakTarget> logs = new ArrayList<>();
        List<BreakTarget> leaves = new ArrayList<>();
        int radius = scanRadius.getInt();
        int height = scanHeight.getInt();
        double range = breakRange.getFloat();

        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                for (int dy = 0; dy <= height; ++dy) {
                    class_2338 pos = farmPos.method_10069(dx, dy, dz);
                    double distSq = mc.field_1724.method_5707(pos.method_46558());
                    if (distSq > range * range) {
                        continue;
                    }
                    class_2350 side = findBreakSide(pos);
                    if (side == null) {
                        continue;
                    }
                    class_2680 state = mc.field_1687.method_8320(pos);
                    BreakTarget target = new BreakTarget(pos, side, distSq);
                    if (isLog(state)) {
                        logs.add(target);
                    } else if (isLeaves(state)) {
                        leaves.add(target);
                    }
                }
            }
        }

        Comparator<BreakTarget> byDistance = Comparator.comparingDouble(BreakTarget::distanceSq);
        BreakTarget nearestLeaves = leaves.stream().min(byDistance).orElse(null);
        if (nearestLeaves != null) {
            return nearestLeaves;
        }
        return logs.stream().min(byDistance).orElse(null);
    }

    private boolean hasLeavesNearby() {
        int radius = scanRadius.getInt();
        int height = scanHeight.getInt();
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                for (int dy = 0; dy <= height; ++dy) {
                    if (isLeaves(mc.field_1687.method_8320(farmPos.method_10069(dx, dy, dz)))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean hasTreeNearby() {
        int radius = scanRadius.getInt();
        int height = scanHeight.getInt();
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dz = -radius; dz <= radius; ++dz) {
                for (int dy = 0; dy <= height; ++dy) {
                    class_2680 state = mc.field_1687.method_8320(farmPos.method_10069(dx, dy, dz));
                    if (isLog(state) || isLeaves(state)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Грань блока, видимая из глаз игрока (проверка лучом). */
    private class_2350 findBreakSide(class_2338 pos) {
        class_243 eyes = mc.field_1724.method_33571();
        class_243 center = pos.method_46558();
        for (class_2350 dir : class_2350.values()) {
            class_243 point = center.method_1031(
                dir.method_10148() * 0.499, dir.method_10164() * 0.499, dir.method_10165() * 0.499);
            class_3965 hit = mc.field_1687.method_17742(new class_3959(
                eyes, point, class_3959.class_3960.field_17559, class_3959.class_242.field_1348, (class_1297) mc.field_1724));
            if (hit == null || hit.method_17783() != class_239.class_240.field_1332 || !hit.method_17777().equals(pos)) {
                continue;
            }
            return hit.method_17780();
        }
        return null;
    }

    // ================= наведение =================

    private boolean aimAtSide(class_2338 pos, class_2350 side) {
        class_243 target = pos.method_46558().method_1031(
            side.method_10148() * 0.5, side.method_10164() * 0.5, side.method_10165() * 0.5);
        return aimAt(target, aimToleranceDeg.getFloat());
    }

    /** Поворачивает голову к точке (не больше шага за тик). true - если уже в пределах допуска. */
    private boolean aimAt(class_243 target, float toleranceDeg) {
        class_243 eyes = mc.field_1724.method_33571();
        class_243 diff = target.method_1020(eyes);
        double horizontal = Math.sqrt(diff.field_1352 * diff.field_1352 + diff.field_1350 * diff.field_1350);

        float wantedYaw = (float) (Math.toDegrees(Math.atan2(diff.field_1350, diff.field_1352)) - 90.0);
        float wantedPitch = (float) (-Math.toDegrees(Math.atan2(diff.field_1351, horizontal)));

        float yawDelta = class_3532.method_15393(wantedYaw - mc.field_1724.method_36454());
        float pitchDelta = class_3532.method_15393(wantedPitch - mc.field_1724.method_36455());

        float newYaw = mc.field_1724.method_36454()
            + class_3532.method_15363(yawDelta, -rotateStepYaw.getFloat(), rotateStepYaw.getFloat());
        float newPitch = mc.field_1724.method_36455()
            + class_3532.method_15363(pitchDelta, -rotateStepPitch.getFloat(), rotateStepPitch.getFloat());
        newPitch = class_3532.method_15363(newPitch, -90.0f, 90.0f);

        mc.field_1724.method_36456(newYaw);
        mc.field_1724.method_36457(newPitch);
        mc.field_1724.method_5847(newYaw);
        mc.field_1724.method_5636(newYaw);

        return Math.abs(yawDelta) <= toleranceDeg && Math.abs(pitchDelta) <= toleranceDeg;
    }

    private void selectBestTool(class_2338 pos) {
        if (!toolSwitchTimer.hasElapsed(toolSwitchDelayMs.getInt())) {
            return;
        }
        class_2680 state = mc.field_1687.method_8320(pos);
        int bestSlot = -1;
        float bestSpeed = 1.0f;
        for (int i = 0; i < 9; ++i) {
            class_1799 stack = mc.field_1724.method_31548().method_5438(i);
            float speed = stack.method_7924(state);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        if (bestSlot != -1 && bestSlot != mc.field_1724.method_31548().field_7545) {
            selectHotbar(bestSlot);
            toolSwitchTimer.reset();
        }
    }

    // ================= посадка и костная мука =================

    private boolean isSaplingAtFarm() {
        return mc.field_1687.method_8320(farmPos).method_26164(class_3481.field_15462);
    }

    private boolean tryPlantSapling() {
        class_1792 sapling = findSaplingItem();
        if (sapling == class_1802.field_8162) {
            return false;
        }
        if (isSaplingAtFarm()) {
            return true;
        }
        if (!canPlantAt(farmPos)) {
            return false;
        }
        class_2338 ground = farmPos.method_10074();
        class_3965 hit = new class_3965(ground.method_46558(), class_2350.field_11036, ground, false);
        return interactWithItemOnBlock(sapling, hit);
    }

    private boolean applyBonemeal() {
        int previousSlot = mc.field_1724.method_31548().field_7545;
        int boneMealSlot = findHotbarSlot(class_1802.field_8324);

        if (boneMealSlot == -1) {
            int inventorySlot = findInventorySlot(class_1802.field_8324);
            if (inventorySlot == -1) {
                return false;
            }
            int freeHotbar = findFreeHotbarSlot();
            if (freeHotbar == -1) {
                return false;
            }
            mc.field_1761.method_2906(mc.field_1724.field_7498.field_7763, inventorySlot, freeHotbar,
                class_1713.field_7791, (class_1657) mc.field_1724);
            boneMealSlot = freeHotbar;
        }

        selectHotbar(boneMealSlot);
        class_3965 hit = new class_3965(farmPos.method_46558(), class_2350.field_11036, farmPos, false);
        if (!aimAt(hit.method_17784(), interactAimToleranceDeg.getFloat())) {
            if (previousSlot != boneMealSlot) {
                selectHotbar(previousSlot);
            }
            return false;
        }
        class_1269 result = mc.field_1761.method_2896(mc.field_1724, class_1268.field_5808, hit);
        mc.field_1724.method_6104(class_1268.field_5808);
        if (previousSlot != boneMealSlot) {
            selectHotbar(previousSlot);
        }
        return result.method_23665();
    }

    private boolean interactWithItemOnBlock(class_1792 item, class_3965 hit) {
        if (!aimAt(hit.method_17784(), interactAimToleranceDeg.getFloat())) {
            return false;
        }
        int itemSlot = findHotbarSlot(item);
        if (itemSlot == -1) {
            return false;
        }
        int previousSlot = mc.field_1724.method_31548().field_7545;
        if (previousSlot != itemSlot) {
            selectHotbar(itemSlot);
        }
        class_1269 result = mc.field_1761.method_2896(mc.field_1724, class_1268.field_5808, hit);
        mc.field_1724.method_6104(class_1268.field_5808);
        if (previousSlot != itemSlot) {
            selectHotbar(previousSlot);
        }
        return result.method_23665();
    }

    /** Точка фермы: блок над прицелом, сам блок прицела или блок перед игроком. */
    private class_2338 resolveFarmPos() {
        class_239 target = mc.field_1765;
        if (target instanceof class_3965 blockHit && blockHit.method_17783() == class_239.class_240.field_1332) {
            class_2338 above = blockHit.method_17777().method_10084();
            if (canPlantAt(above)) {
                return above;
            }
            class_2338 same = blockHit.method_17777();
            if (canPlantAt(same)) {
                return same;
            }
        }
        class_2338 ahead = mc.field_1724.method_24515().method_10093(mc.field_1724.method_5735());
        return canPlantAt(ahead) ? ahead : null;
    }

    private boolean canPlantAt(class_2338 pos) {
        class_2680 state = mc.field_1687.method_8320(pos);
        if (!state.method_26215() && !state.method_45474()) {
            return false;
        }
        class_2680 below = mc.field_1687.method_8320(pos.method_10074());
        return below.method_26164(class_3481.field_29822)
            || below.method_27852(class_2246.field_10219)
            || below.method_27852(class_2246.field_10362)
            || below.method_27852(class_2246.field_37576);
    }

    private List<class_1792> saplingItems() {
        return List.of(class_1802.field_17535, class_1802.field_17536, class_1802.field_17537, class_1802.field_17538,
            class_1802.field_17539, class_1802.field_17540, class_1802.field_42688, class_1802.field_37508);
    }

    private class_1792 findSaplingItem() {
        List<class_1792> saplings = saplingItems();
        class_1735 slot = findSlot(s -> saplings.contains(s.method_7677().method_7909()));
        return slot != null ? slot.method_7677().method_7909() : class_1802.field_8162;
    }

    // ================= починка мотыги =================

    /** true - тик занят починкой, остальную работу пропускаем. */
    private boolean tickHoeRepair() {
        if (!autoRepairHoe.get()) {
            restoreHoe();
            return false;
        }
        if (repairingHoe) {
            return repairTick();
        }

        class_1735 damagedHoe = findDamagedHoe();
        if (damagedHoe == null) {
            return false;
        }
        stopMining();

        if (!hasUsableBottle()) {
            if (notifyTimer.hasElapsed(2500.0)) {
                chat("TreeBot: no experience bottle for hoe auto-repair.");
                notifyTimer.reset();
            }
            return true;
        }
        if (!moveHoeToOffhand(damagedHoe)) {
            if (notifyTimer.hasElapsed(2500.0)) {
                chat("TreeBot: failed to move hoe to offhand for repair.");
                notifyTimer.reset();
            }
            return true;
        }
        repairingHoe = true;
        repairTimer.setElapsed(50L);
        return repairTick();
    }

    private boolean repairTick() {
        class_1799 offhand = mc.field_1724.method_6079();
        if (!isHoe(offhand)) {
            resetHoeTracking();
            return false;
        }
        if (!isRepairThresholdReached(offhand)) {
            restoreHoe();
            return true;
        }
        stopMining();
        if (!repairTimer.hasElapsed(50.0)) {
            return true;
        }
        Bottle bottle = findRepairBottle();
        if (bottle.slot == -1) {
            if (notifyTimer.hasElapsed(2500.0)) {
                chat(bottle.message);
                notifyTimer.reset();
            }
            repairTimer.reset();
            return true;
        }
        useBottle(bottle.slot);
        repairTimer.reset();
        return true;
    }

    private boolean moveHoeToOffhand(class_1735 hoeSlot) {
        repairHoeSourceSlotId = -1;
        repairHoeSourceHotbarSlot = -1;
        if (hoeSlot.field_7874 == OFFHAND_SLOT_ID) {
            return true;
        }
        repairHoeSourceSlotId = hoeSlot.field_7874;
        if (hoeSlot.field_7874 >= 36 && hoeSlot.field_7874 <= 44) {
            repairHoeSourceHotbarSlot = hoeSlot.field_7874 - 36;
        }
        swapWithOffhand(hoeSlot);
        return isHoe(mc.field_1724.method_6079());
    }

    private void restoreHoe() {
        if (!repairingHoe || mc.field_1724 == null || mc.field_1761 == null) {
            resetHoeTracking();
            return;
        }
        if (repairHoeSourceSlotId != -1 && isHoe(mc.field_1724.method_6079())) {
            class_1735 source = findSlotById(repairHoeSourceSlotId);
            if (source != null) {
                swapWithOffhand(source);
            }
        }
        resetHoeTracking();
    }

    private void resetHoeTracking() {
        repairingHoe = false;
        repairHoeSourceSlotId = -1;
        repairHoeSourceHotbarSlot = -1;
    }

    private Bottle findRepairBottle() {
        Predicate<class_1799> repairBottle = s -> isUsableBottle(s) && isRepairBottle(s);
        Predicate<class_1799> anyBottle = this::isUsableBottle;

        int hotbar = findHotbarSlot(repairBottle);
        if (hotbar == -1) {
            hotbar = findHotbarSlot(anyBottle);
        }
        if (hotbar != -1) {
            return new Bottle(hotbar, "");
        }

        int inventory = findInventorySlot(repairBottle);
        if (inventory == -1) {
            inventory = findInventorySlot(anyBottle);
        }
        if (inventory == -1) {
            String message = hasUsableBottle()
                ? "TreeBot: experience bottle is on cooldown."
                : "TreeBot: no experience bottle for hoe auto-repair.";
            return new Bottle(-1, message);
        }

        int safeHotbar = findSafeHotbarSlot();
        if (safeHotbar == -1) {
            return new Bottle(-1, "TreeBot: no safe hotbar slot for repair bottle.");
        }
        mc.field_1761.method_2906(mc.field_1724.field_7498.field_7763, inventory, safeHotbar,
            class_1713.field_7791, (class_1657) mc.field_1724);
        return new Bottle(safeHotbar, "");
    }

    private void useBottle(int hotbarSlot) {
        int previousSlot = mc.field_1724.method_31548().field_7545;
        selectHotbar(hotbarSlot);
        faceDown();
        class_1269 result = mc.field_1761.method_2919((class_1657) mc.field_1724, class_1268.field_5808);
        mc.field_1724.method_6104(class_1268.field_5808);
        if (previousSlot != hotbarSlot) {
            selectHotbar(previousSlot);
        }
        if (!result.method_23665() && notifyTimer.hasElapsed(2500.0)) {
            chat("TreeBot: failed to use experience bottle.");
            notifyTimer.reset();
        }
    }

    /** Смотрим строго вниз, чтобы пузырь опыта падал под ноги. */
    private void faceDown() {
        float yaw = mc.field_1724.method_36454();
        mc.field_1724.method_36456(yaw);
        mc.field_1724.method_36457(90.0f);
        mc.field_1724.method_5847(yaw);
        mc.field_1724.method_5636(yaw);
    }

    private void stopMining() {
        currentBreakTarget = null;
        currentBreakSide = null;
        if (isBaritoneMode() && baritoneAvailable && miningActive) {
            sendBaritone("#stop");
            miningActive = false;
        }
    }

    private class_1735 findDamagedHoe() {
        class_1735 offhand = findSlotById(OFFHAND_SLOT_ID);
        if (offhand != null && isRepairThresholdReached(offhand.method_7677())) {
            return offhand;
        }
        return slots()
            .filter(s -> s.field_7874 != OFFHAND_SLOT_ID)
            .filter(s -> isRepairThresholdReached(s.method_7677()))
            .findFirst()
            .orElse(null);
    }

    private boolean isRepairThresholdReached(class_1799 stack) {
        return isHoe(stack) && durabilityPercent(stack) < repairThresholdPercent.getFloat();
    }

    private boolean isHoe(class_1799 stack) {
        return !stack.method_7960() && stack.method_7963() && stack.method_7909() instanceof class_1794;
    }

    private float durabilityPercent(class_1799 stack) {
        if (!stack.method_7963() || stack.method_7936() <= 0) {
            return 100.0f;
        }
        return (float) (stack.method_7936() - stack.method_7919()) * 100.0f / (float) stack.method_7936();
    }

    // ================= пузырьки опыта =================

    private boolean hasUsableBottle() {
        if (isUsableBottle(mc.field_1724.method_6079())) {
            return true;
        }
        for (int i = 0; i < 36; ++i) {
            if (isUsableBottle(mc.field_1724.method_31548().method_5438(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isUsableBottle(class_1799 stack) {
        return isBottle(stack) && !mc.field_1724.method_7357().method_7904(stack);
    }

    private boolean isBottle(class_1799 stack) {
        return !stack.method_7960() && stack.method_7909() == class_1802.field_8287;
    }

    private boolean isRepairBottle(class_1799 stack) {
        if (!isBottle(stack)) {
            return false;
        }
        class_9279 lore = (class_9279) stack.method_57824(class_9334.field_49628);
        return lore != null && lore.toString().contains(REPAIR_BOTTLE_LORE_MARKER);
    }

    // ================= слоты инвентаря =================

    private int findSafeHotbarSlot() {
        for (int i = 0; i < 9; ++i) {
            if (i == repairHoeSourceHotbarSlot) {
                continue;
            }
            class_1792 item = mc.field_1724.method_31548().method_5438(i).method_7909();
            if (item == class_1802.field_8162 || !isProtectedItem(item)) {
                return i;
            }
        }
        return -1;
    }

    private int findFreeHotbarSlot() {
        for (int i = 0; i < 9; ++i) {
            class_1792 item = mc.field_1724.method_31548().method_5438(i).method_7909();
            if (item == class_1802.field_8162 || !isProtectedItem(item)) {
                return i;
            }
        }
        return -1;
    }

    /** Предметы, которые нельзя выталкивать из хотбара (инструменты, кость, саженцы, пузыри). */
    private boolean isProtectedItem(class_1792 item) {
        return item == class_1802.field_8556 || item == class_1802.field_22025
            || item == class_1802.field_8527 || item == class_1802.field_22026
            || item == class_1802.field_8324 || item == class_1802.field_8287
            || saplingItems().contains(item);
    }

    private class_1735 findSlotById(int id) {
        return slots().filter(s -> s.field_7874 == id).findFirst().orElse(null);
    }

    private int findHotbarSlot(class_1792 item) {
        for (int i = 0; i < 9; ++i) {
            if (mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findHotbarSlot(Predicate<class_1799> predicate) {
        for (int i = 0; i < 9; ++i) {
            if (predicate.test(mc.field_1724.method_31548().method_5438(i))) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(class_1792 item) {
        for (int i = 9; i < 36; ++i) {
            if (mc.field_1724.method_31548().method_5438(i).method_7909() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findInventorySlot(Predicate<class_1799> predicate) {
        for (int i = 9; i < 36; ++i) {
            if (predicate.test(mc.field_1724.method_31548().method_5438(i))) {
                return i;
            }
        }
        return -1;
    }

    // ================= вспомогательные методы (замена утилит Releon) =================

    private static java.util.stream.Stream<class_1735> slots() {
        return mc.field_1724.field_7512.field_7761.stream();
    }

    private static class_1735 findSlot(Predicate<class_1735> predicate) {
        return slots().filter(predicate).findFirst().orElse(null);
    }

    private static void selectHotbar(int slot) {
        if (mc.field_1724 == null || mc.method_1562() == null) {
            return;
        }
        if (mc.field_1724.method_31548().field_7545 != slot) {
            mc.field_1724.method_31548().field_7545 = slot;
        }
    }

    /** Меняет местами слот и вторую руку (клик SWAP с кнопкой 40). */
    private static void swapWithOffhand(class_1735 slot) {
        if (slot == null || slot.field_7874 == -1 || !(slot.field_7871 instanceof class_1661)) {
            return;
        }
        mc.field_1761.method_2906(mc.field_1724.field_7512.field_7763, slot.field_7874, 40,
            class_1713.field_7791, (class_1657) mc.field_1724);
    }

    private static void chat(String message) {
        if (mc.field_1724 == null) {
            return;
        }
        class_5250 prefix = class_2561.method_43470("AppleFarm -> ");
        prefix.method_10862(prefix.method_10866().method_10977(class_124.field_1080));
        class_5250 text = class_2561.method_43470(message);
        text.method_10862(text.method_10866().method_10977(class_124.field_1080));
        prefix.method_10852(text);
        mc.field_1705.method_1743().method_1812(prefix);
    }

    // ================= Baritone =================

    private void sendBaritone(String command) {
        if (mc.field_1724 != null && mc.field_1724.field_3944 != null) {
            mc.field_1724.field_3944.method_45729(command);
        }
    }

    private boolean isBaritoneInstalled() {
        try {
            Class.forName("baritone.api.BaritoneAPI");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ================= вспомогательные типы =================

    private static final class BreakTarget {
        private final class_2338 pos;
        private final class_2350 side;
        private final double distanceSq;

        private BreakTarget(class_2338 pos, class_2350 side, double distanceSq) {
            this.pos = pos;
            this.side = side;
            this.distanceSq = distanceSq;
        }

        private double distanceSq() {
            return distanceSq;
        }
    }

    private static final class Bottle {
        private final int slot;
        private final String message;

        private Bottle(int slot, String message) {
            this.slot = slot;
            this.message = message;
        }
    }
}
