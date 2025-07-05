import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.*;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.*;

public class MapUpdateListener implements Emitter.Listener {

    /* ---------------- PARAM ---------------- */
    private static final int  HEAL_THRESHOLD_HP = 70;   // HP ≤ 70 thì heal
    private static final int  MAX_WAIT_TURNS_FOR_PICKUP = 5;
    private static final String[] DIRS = {"r","d","u","l"};

    /* ---------------- RUNTIME ---------------- */
    private final Hero hero;

    private Node  targetChest = null;
    private int   waitCounter = 0;
    private boolean waitingLoot = false;

    private boolean hasWeaponPhase1   = false;
    private boolean doneFirstChest    = false;

    public MapUpdateListener(Hero hero){ this.hero = hero; }

    /* ===================================================== */
    @Override public void call(Object... args) {

        try {
            GameMap map = hero.getGameMap();
            map.updateOnUpdateMap(args[0]);

            Player me = map.getCurrentPlayer();
            if (me == null || me.getHealth() == null || me.getHealth() <= 0) return;

            Inventory inv = hero.getInventory();
            Node myPos    = new Node(me.getX(), me.getY());
            List<Node> avoid = getAvoidList(map);

            /* ---------- 0. AUTO-HEAL ---------- */
            if (me.getHealth() <= HEAL_THRESHOLD_HP && tryHeal(inv)) return;

            /* ---------- 1. GIAI ĐOẠN 1: TÌM VŨ KHÍ ---------- */
            if (!hasWeaponPhase1) {
                if (hasGoodWeapon(inv)) { hasWeaponPhase1 = true; }
                else if (findAndMoveToBestWeapon(map, me, avoid)) { return; }
                else hero.move("r");                                  // fallback
                return;
            }

            /* ---------- 2. GIAI ĐOẠN 2: PHÁ RƯƠNG ĐẦU ---------- */
            if (!doneFirstChest) {
                if (waitingLoot && handleLootAroundChest(map, myPos, avoid)) return;

                Node chest = nearestChest(map, me);
                if (chest != null && canBreakChest(inv)) {
                    breakOrMoveChest(map, me, chest, avoid);
                    return;
                }
                doneFirstChest = true;          // không có / không phá được
            }

            /* ---------- 3. GIAI ĐOẠN 3: COMBAT + LOOT ---------- */
            // a. bắn enemy nếu đủ điều kiện
            Player enemy = nearestEnemy(map, me);
            if (enemy != null) {
                attackEnemy(inv, map, me, enemy, avoid);
                return;
            }

            // b. nâng cấp súng hoặc loot item giá trị
            if (findAndMoveToBestWeapon(map, me, avoid)) return;
            if (lootValuableItem(map, me, avoid)) return;

            // c. phá thêm rương
            Node moreChest = nearestChest(map, me);
            if (moreChest != null && canBreakChest(inv)) {
                breakOrMoveChest(map, me, moreChest, avoid);
                return;
            }

            // d. fallback di chuyển nhẹ
            hero.move("r");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /* =====================================================
                H E A L
       ===================================================== */
    private boolean tryHeal(Inventory inv) throws IOException {
        List<SupportItem> list = inv.getListSupportItem();
        if (list == null || list.isEmpty()) return false;
        SupportItem best = list.stream()
                .max(Comparator.comparingInt(SupportItem::getHealingHP)).orElse(null);
        hero.useItem(best.getId());
        return true;
    }

    /* =====================================================
                P H A S E   1   –  W E A P O N
       ===================================================== */
    private boolean hasGoodWeapon(Inventory inv){
        if (inv.getGun()!=null && !"RUBBER_GUN".equals(inv.getGun().getId())
                && inv.getGun().getUseCount()>0) return true;
        if (inv.getMelee()!=null && !"HAND".equals(inv.getMelee().getId())) return true;
        if (inv.getSpecial()!=null && inv.getSpecial().getUseCount()>0) return true;
        return false;
    }
    private boolean findAndMoveToBestWeapon(GameMap m, Player p, List<Node> avoid) throws IOException{
        Weapon best = chooseBestWeapon(m,p);
        if (best == null) return false;
        moveOrPickup(best,new Node(p.getX(),p.getY()),m,avoid);
        return true;
    }
    private Weapon chooseBestWeapon(GameMap m, Player p){
        int bestScore=-1, bestDist=Integer.MAX_VALUE; Weapon best=null;
        Node me = new Node(p.getX(),p.getY());

        List<Weapon> all = new ArrayList<>();
        all.addAll(m.getAllGun()); all.addAll(m.getAllMelee()); all.addAll(m.getAllSpecial());

        for (Weapon w: all){
            if ("RUBBER_GUN".equals(w.getId())) continue;
            int score = weaponScore(w);
            int dist  = PathUtils.distance(me,new Node(w.getX(),w.getY()));
            if (score>bestScore || (score==bestScore && dist<bestDist)){
                best=w; bestScore=score; bestDist=dist;
            }
        }
        return best;
    }
    private int weaponScore(Weapon w){
        String id=w.getId(); String t=w.getType().toString();
        if (t.contains("GUN")){
            return switch(id){case"SHOTGUN"->100;case"CROSSBOW"->90;case"PISTOL"->80;default->50;};
        }
        if (t.contains("MELEE")){
            return switch(id){case"AXE"->100;case"SCEPTER"->90;case"BONE"->80;case"SAHUR_BAT"->70;default->50;};
        }
        if (t.contains("SPECIAL")) return "MACE".equals(id)?100:80;
        return 50;
    }

    /* =====================================================
                C H E S T
       ===================================================== */
    private Node nearestChest(GameMap m, Player p){
        Node me=new Node(p.getX(),p.getY()); Node res=null; int dmin=Integer.MAX_VALUE;
        for (Obstacle o:m.getObstaclesByTag("DESTRUCTIBLE"))
            if (o.getId()!=null && o.getId().contains("CHEST")){
                Node n=new Node(o.getX(),o.getY());
                int d=PathUtils.distance(me,n);
                if (d<dmin){dmin=d;res=n;}
            }
        return res;
    }
    private boolean canBreakChest(Inventory inv){
        return  (inv.getGun()!=null && inv.getGun().getUseCount()>0) ||
                (inv.getMelee()!=null && !"HAND".equals(inv.getMelee().getId())) ||
                (inv.getSpecial()!=null && inv.getSpecial().getUseCount()>0) ||
                (inv.getThrowable()!=null && inv.getThrowable().getUseCount()>0);
    }
    private void breakOrMoveChest(GameMap map, Player p, Node chest, List<Node> avoid) throws IOException{
        Node me = new Node(p.getX(),p.getY());
        int d = PathUtils.distance(me,chest);
        if (d>1){                           // di chuyển lại gần
            String path = PathUtils.getShortestPath(map, avoid, me, chest,false);
            if (path!=null&&!path.isEmpty()) hero.move(""+path.charAt(0));
            return;
        }
        if (d==1){                          // đập rương
            String dir = direction(me,chest);
            Inventory inv = hero.getInventory();
            if (inv.getGun()!=null && inv.getGun().getUseCount()>0) hero.shoot(dir);
            else if (inv.getMelee()!=null && !"HAND".equals(inv.getMelee().getId())) hero.attack(dir);
            else if (inv.getSpecial()!=null && inv.getSpecial().getUseCount()>0) hero.useSpecial(dir);
            else if (inv.getThrowable()!=null && inv.getThrowable().getUseCount()>0) hero.throwItem(dir);
            targetChest = new Node(chest.getX(),chest.getY());
            waitingLoot = true; waitCounter=0;
        }
    }
    private boolean handleLootAroundChest(GameMap m, Node me, List<Node> avoid) throws IOException{
        if (targetChest==null){ waitingLoot=false; return false; }

        if (me.equals(targetChest)){ hero.pickupItem(); return true; }

        // thử 4 hướng xung quanh
        for (String d:DIRS){
            Node nxt = moveDir(targetChest,d);
            if (PathUtils.distance(me,nxt)==0){
                hero.pickupItem(); return true;
            }
            if (PathUtils.distance(me,nxt)==1){
                hero.move(direction(me,nxt)); return true;
            }
            String path = PathUtils.getShortestPath(m,avoid,me,nxt,false);
            if (path!=null&&!path.isEmpty()){ hero.move(""+path.charAt(0)); return true; }
        }
        // đợi 5 turn rồi bỏ
        if (++waitCounter>=MAX_WAIT_TURNS_FOR_PICKUP){ waitingLoot=false; targetChest=null; }
        return false;
    }

    /* =====================================================
                C O M B A T
       ===================================================== */
    private Player nearestEnemy(GameMap m, Player p){
        Player res=null; int dmin=Integer.MAX_VALUE;
        for (Player e:m.getOtherPlayerInfo())
            if (e.getHealth()!=null && e.getHealth()>0){
                int d=PathUtils.distance(p,e);
                if (d<dmin){dmin=d;res=e;}
            }
        return res;
    }
    private void attackEnemy(Inventory inv, GameMap m, Player me, Player enemy, List<Node> avoid) throws IOException{
        Node my=new Node(me.getX(),me.getY()), en=new Node(enemy.getX(),enemy.getY());
        int d = PathUtils.distance(my,en);

        if (inv.getGun()!=null && inv.getGun().getUseCount()>0){
            int R = inv.getGun().getRange()[0];
            if (d<=R){
                hero.shoot(direction(my,en)); return;
            } else { moveOrPickup(en,my,m,avoid); return; }
        }
        if (d==1){
            if (inv.getMelee()!=null && !"HAND".equals(inv.getMelee().getId())) { hero.attack(direction(my,en)); return; }
            if (inv.getSpecial()!=null && inv.getSpecial().getUseCount()>0){ hero.useSpecial(direction(my,en)); return; }
        }
        moveOrPickup(en,my,m,avoid);
    }

    /* =====================================================
                L O O T   I T E M
       ===================================================== */
    private boolean lootValuableItem(GameMap m, Player p, List<Node> avoid) throws IOException{
        Node me = new Node(p.getX(),p.getY());
        Element best=null; int bestScore=-1;

        for (Weapon w:m.getListWeapons())
            if (w.getPickupPoints()>bestScore){ best=w; bestScore=w.getPickupPoints();}
        for (SupportItem s:m.getListSupportItems())
            if (s.getPoint()>bestScore){ best=s; bestScore=s.getPoint();}

        if (best==null) return false;
        moveOrPickup(best,new Node(best.getX(),best.getY()),m,avoid);
        return true;
    }

    /* =====================================================
                H E L P E R S
       ===================================================== */
    private List<Node> getAvoidList(GameMap m){
        List<Node> a=new ArrayList<>(m.getListIndestructibles());
        a.addAll(m.getListEnemies()); a.addAll(m.getOtherPlayerInfo());
        return a;
    }
    private void moveOrPickup(Node target, Node me, GameMap m, List<Node> avoid) throws IOException{
        if (me.equals(target)){ hero.pickupItem(); return; }
        String path = PathUtils.getShortestPath(m,avoid,me,target,false);
        if (path!=null && !path.isEmpty()) hero.move(""+path.charAt(0));
    }
    private static Node moveDir(Node n,String d){
        return switch(d){
            case"r"->new Node(n.x+1,n.y);
            case"l"->new Node(n.x-1,n.y);
            case"u"->new Node(n.x,n.y+1);
            default ->new Node(n.x,n.y-1);
        };
    }
    private static String direction(Node a, Node b){      // 4-hướng
        int dx=b.x-a.x, dy=b.y-a.y;
        return Math.abs(dx)>Math.abs(dy) ? (dx>0?"r":"l") : (dy>0?"u":"d");
    }
}
