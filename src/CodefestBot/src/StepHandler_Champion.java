
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.List;

/**
 * ======================================================================
 *  CHAMPION Bot v1 – “adaptive winner”
 *  ✦ Early  (T > 2/3) :  Loot vũ khí + giáp + chest, tránh giao tranh.    [Stealth Opportunist] :contentReference[oaicite:0]{index=0}
 *  ✦ Mid    (T ∈ (1/3,2/3]) :  Giữ bo, bắn kẻ yếu, lấy thính.            [Smart Aggressive]   :contentReference[oaicite:1]{index=1}
 *  ✦ Late   (T ≤ 1/3) :  Săn kill, dồn góc map, tối đa streak 100 đ/kill. [Zone Dominator]     :contentReference[oaicite:2]{index=2}
 *  ✦ Mọi lúc: Né Bullet/NPC, tự heal, anti‑stuck, ưu tiên phá CHEST (+điểm & 4 đồ) :contentReference[oaicite:3]{index=3}
 * ----------------------------------------------------------------------
 *  Cấu trúc:
 *      0.  anti‑stuck   (nhờ BaseBotLogic)
 *      1.  survival     (heal sớm, né đạn/NPC, giữ trong bo)
 *      2.  economy      (loot súng, giáp, rương, thính)
 *      3.  combat       (bắn/melee/throw/special tuỳ trang bị & phase)
 *      4.  fallback     (moveRandom)
 * ======================================================================
 */
public class StepHandler_Champion {

    /* ===== THAM SỐ CHIẾN THUẬT ===== */
    private static final int LOW_HP          = 80;   // heal
    private static final int CRIT_HP         = 50;   // chạy lấy Item
    private static final int SAFE_MARGIN     = 2;    // cách rìa bo
    private static final int CLOSE_RANGE     = 3;    // phạm vi “nguy hiểm”
    private static final int LATE_GAME_SEC   = 60;   // 1/3 map 5p ≈ 100 s
    private static final int MID_GAME_SEC    = 180;  // 2/3 map 5p ≈ 200 s

    /* ==================== HANDLE STEP ==================== */
    public static void handleStep(GameMap map, Hero hero) throws IOException {

        if (map == null || hero == null) return;
        Player me = map.getCurrentPlayer();
        if (me == null || me.getHealth() == null || me.getHealth() <= 0) return;

        Node cur          = new Node(me.getX(), me.getY());
        Inventory inv     = hero.getInventory();
        List<Node> avoid  = BaseBotLogic.buildAvoidList(map, true);

        /* 0. ANTI‑STUCK */
        if (BaseBotLogic.isStuck(cur)) {
            BaseBotLogic.resolveStuck(hero, map, cur,
                    BaseBotLogic.getClosest(map.getObstaclesByTag("DESTRUCTIBLE"), cur),
                    avoid);
            return;
        }

        /* 1. SURVIVAL – heal & né */
        BaseBotLogic.useSupportIfLowHP(hero, inv, me.getHealth());
        if (BaseBotLogic.dodgeBulletIfTargeted(hero, map, cur)) return;
        if (BaseBotLogic.avoidEnemies(hero, map, cur)) return;

        /* 1b. GIỮ TRONG BO */
        if (!PathUtils.checkInsideSafeArea(cur,
                map.getSafeZone() - SAFE_MARGIN, map.getMapSize())) {
            Node center = new Node(map.getMapSize()/2, map.getMapSize()/2);
            BaseBotLogic.goTo(hero, map, cur, center, avoid);
            return;
        }

        /* 2. ECONOMY – luôn phá rương gần nhất */
        if (BaseBotLogic.breakChestIfNearby(hero, map, cur)) return;

        /* 2a. LOOT súng (chưa có) */
        if (BaseBotLogic.pickupGunIfNeeded(hero, map, cur)) return;

        /* 2b. LOOT Item/Weapon điểm cao */
        if (BaseBotLogic.pickupValuableItem(hero, map, cur)) return;

        /* 2c. THÍNH – lấy khi thời điểm spawn */
        if (isAirdropTime(map) && goToPredictedAirdrop(hero, map, cur, avoid)) return;

        /* 3. COMBAT – adaptive by phase */
        int remainSec = getRemainSecond(map);
        Phase phase = getPhase(remainSec);

        switch (phase) {
            case EARLY -> {
                // chỉ bắn khi kẻ địch cực gần & yếu
                if (shootWeakEnemy(hero, map, cur, inv, CLOSE_RANGE, 60)) return;
            }
            case MID -> {
                // bắn kẻ yếu trong tầm súng ngắn, không rượt quá 5 ô
                int range = Math.min(getGunRange(inv), 5);
                if (shootWeakEnemy(hero, map, cur, inv, range, 70)) return;
            }
            case LATE -> {
                // chủ động hơn – bắn bất kỳ địch trong tầm súng
                if (BaseBotLogic.shootNearby(hero, map, cur, inv)) return;

                // nếu có súng & máu khỏe, rượt địch gần
                if (inv.getGun()!=null && me.getHealth() > LOW_HP) {
                    Player tgt = BaseBotLogic.getClosest(map.getOtherPlayerInfo(), cur);
                    if (tgt != null) {
                        BaseBotLogic.goTo(hero, map, cur, tgt, avoid);
                        return;
                    }
                }
            }
        }

        /* 4. FALLBACK – đi dạo trong bo */
        BaseBotLogic.moveRandom(hero, map, cur, avoid);
    }

    /* ==================== HỖ TRỢ ==================== */

    /** Pha trận: EARLY / MID / LATE dựa trên giây còn lại của map nhỏ 5p */
    private enum Phase { EARLY, MID, LATE }
    private static Phase getPhase(int remain) {
        if (remain > MID_GAME_SEC) return Phase.EARLY;
        if (remain > LATE_GAME_SEC) return Phase.MID;
        return Phase.LATE;
    }

    private static int getRemainSecond(GameMap m) {
        int total = m.getMapSize() >= 70 ? (m.getMapSize()==100 ? 600 : 300) : 300;
        int currentStep = m.getStepNumber();
        return total - (int)(currentStep*0.5);
    }

    private static int getGunRange(Inventory inv) {
        Weapon g = inv.getGun();
        return (g!=null && g.getRange()!=null) ? g.getRange()[0] : 0;
    }

    /** Bắn kẻ yếu (HP ≤ weakHP) trong range */
    private static boolean shootWeakEnemy(Hero hero, GameMap map, Node cur,
                                          Inventory inv, int range, int weakHP) throws IOException {
        Weapon gun = inv.getGun();
        if (gun == null) return false;

        Player tgt = map.getOtherPlayerInfo().stream()
                .filter(p->p.getHealth()!=null && p.getHealth() <= weakHP)
                .filter(p->PathUtils.distance(cur,p)<=range)
                .min((a,b)->Float.compare(a.getHealth(), b.getHealth()))
                .orElse(null);
        if (tgt != null) {
            hero.shoot(BaseBotLogic.getDirection(cur, tgt));
            return true;
        }
        return false;
    }

    /** true nếu hiện tại khớp mốc spawn airdrop theo Game Mechanics 2025 */
    private static boolean isAirdropTime(GameMap m) {
        int remain = getRemainSecond(m);
        int[] marks = m.getMapSize()==70 ? new int[]{200,100,450,330,210,90}
                : new int[]{520,440,360,240,120};
        for (int mk : marks) if (remain == mk) return true;
        return false;
    }

    /** Ước đoán vị trí thính (center±safeZone/2) rồi di chuyển */
    private static boolean goToPredictedAirdrop(Hero h, GameMap m, Node cur,
                                                List<Node> avoid) throws IOException {
        int safe = m.getSafeZone();
        int map  = m.getMapSize();
        Node center = new Node(map/2, map/2);
        Node guess  = new Node(center.x + safe/2, center.y); // ước lượng một cạnh
        return BaseBotLogic.goTo(h, m, cur, guess, avoid);
    }
}
