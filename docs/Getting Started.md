### **----------------Getting Started-------------**







Tài liệu này mô tả các bước cài đặt game \& chạy SDK để kết nối.



Bước 1: Set up phần mềm

Download Intellij Community: Download Intellij

Screenshot 2024-08-23 112420



Download file Codefest.jar từ bản release version mới nhất (là một file java chứa các thư viện có sẵn để hỗ trợ cho người chơi code) : Download Jar

Bước 2: Tạo Project

Mở Intellij vừa dược tải về, chọn New Project

image



Đặt tên cho Project và bấm nút Create, lưu ý hãy chọn phiên bản JDK từ 20 trở lên

image



Khi này bạn sẽ được giao diện của Project

image



Bước 3: Import file jar

Ở góc trên bên trái màn hình chọn Menu sau đó chọn mục Project Structure

Screenshot 2024-08-23 115123



Nếu không thấy bạn có thể chọn Setting ở góc trên phải màn hình và chọn Project Structure

Screenshot 2024-08-23 120831



Cửa sổ mới sẽ mở ra, sau đó bạn chọn mục Modules -> Libraries

image



Chọn dấu + và chọn Java

image



Chọn file CodeFest.jar bạn vừa tải về trong folder Downloads sau đó chọn OK

image



Sau đó bạn sẽ thêm được file jar vào trong project, bạn chọn Apply sau đó OK

image



Khi này bạn đã thêm file jar thành công

image



Bước 4: Connect server

Copy đoạn code sau paste vào file Main.java trong Project của bạn

import io.socket.emitter.Emitter;

import jsclub.codefest.sdk.model.GameMap;

import jsclub.codefest.sdk.Hero;



import java.io.IOException;



public class Main {

&nbsp;   private static final String SERVER\_URL = "https://cf25-server.jsclub.dev";

&nbsp;   private static final String GAME\_ID = "";

&nbsp;   private static final String PLAYER\_NAME = "";

&nbsp;   private static final String SECRET\_KEY = "\*secret-key được BTC cung cấp\*";





&nbsp;   public static void main(String\[] args) throws IOException {

&nbsp;       Hero hero = new Hero(GAME\_ID, PLAYER\_NAME, SECRET\_KEY);



&nbsp;       Emitter.Listener onMapUpdate = new Emitter.Listener() {

&nbsp;           @Override

&nbsp;           public void call(Object... args) {



&nbsp;               GameMap gameMap = hero.getGameMap();

&nbsp;               gameMap.updateOnUpdateMap(args\[0]);

&nbsp;               

&nbsp;           }

&nbsp;       };



&nbsp;       hero.setOnMapUpdate(onMapUpdate);

&nbsp;       hero.start(SERVER\_URL);

&nbsp;   }

}

Sau đó mở game lên, giao diện của game sẽ như này

image



Lúc này trên màn hình sẽ có game ID, lúc này là 189926 Sau đó bạn sẽ nhập game id vào code trong file main của bạn

private static final String GAME\_ID = "189926"; 

Bộ phận kĩ thuật sẽ cung cấp cho các đội chơi SECRET\_KEY cho từng con bot, bạn nhập vào phần tương ứng trong code

&nbsp;   private static final String SECRET\_KEY = "key\_duoc\_cap";

Sau đó bạn run Project

image



Project sẽ log ra connected to server, lúc này bạn đã thành công connect to server



Lúc này trong game sẽ hiện lên người chơi với tên tương ứng trong code



image



Bước 5: Chạy thử bot

Để chắc chắn con bot của bạn có thể thực hiện hành động trong game, mình sẽ thử cho nó đi lấy súng

Sử dụng if-else để nó sẽ là mỗi condition(step) thực hiện một action

Lúc đấy toàn bộ code của bạn sẽ như này

import io.socket.emitter.Emitter;

import jsclub.codefest.sdk.Hero;

import jsclub.codefest.sdk.algorithm.PathUtils;

import jsclub.codefest.sdk.base.Node;

import jsclub.codefest.sdk.model.GameMap;

import jsclub.codefest.sdk.model.players.Player;

import jsclub.codefest.sdk.model.weapon.Weapon;



import java.io.IOException;

import java.util.ArrayList;

import java.util.List;



public class Main {

&nbsp;   private static final String SERVER\_URL = "https://cf25-server.jsclub.dev";

&nbsp;   private static final String GAME\_ID = "";

&nbsp;   private static final String PLAYER\_NAME = "";

&nbsp;   private static final String SECRET\_KEY = "\*secret-key được BTC cung cấp\*";





&nbsp;   public static void main(String\[] args) throws IOException {

&nbsp;       Hero hero = new Hero(GAME\_ID, PLAYER\_NAME, SECRET\_KEY);

&nbsp;       Emitter.Listener onMapUpdate = new MapUpdateListener(hero);



&nbsp;       hero.setOnMapUpdate(onMapUpdate);

&nbsp;       hero.start(SERVER\_URL);

&nbsp;   }

}



class MapUpdateListener implements Emitter.Listener {

&nbsp;   private final Hero hero;



&nbsp;   public MapUpdateListener(Hero hero) {

&nbsp;       this.hero = hero;

&nbsp;   }



&nbsp;   @Override

&nbsp;   public void call(Object... args) {

&nbsp;       try {

&nbsp;           if (args == null || args.length == 0) return;



&nbsp;           GameMap gameMap = hero.getGameMap();

&nbsp;           gameMap.updateOnUpdateMap(args\[0]);

&nbsp;           Player player = gameMap.getCurrentPlayer();



&nbsp;           if (player == null || player.getHealth() == 0) {

&nbsp;               System.out.println("Player is dead or data is not available.");

&nbsp;               return;

&nbsp;           }



&nbsp;           List<Node> nodesToAvoid = getNodesToAvoid(gameMap);



&nbsp;           if (hero.getInventory().getGun() == null) {

&nbsp;               handleSearchForGun(gameMap, player, nodesToAvoid);

&nbsp;           }





&nbsp;       } catch (Exception e) {

&nbsp;           System.err.println("Critical error in call method: " + e.getMessage());

&nbsp;           e.printStackTrace();

&nbsp;       }

&nbsp;   }



&nbsp;   private void handleSearchForGun(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {

&nbsp;       System.out.println("No gun found. Searching for a gun.");

&nbsp;       String pathToGun = findPathToGun(gameMap, nodesToAvoid, player);



&nbsp;       if (pathToGun != null) {

&nbsp;           if (pathToGun.isEmpty()) {

&nbsp;               hero.pickupItem();

&nbsp;           } else {

&nbsp;               hero.move(pathToGun);

&nbsp;           }

&nbsp;       }

&nbsp;   }



&nbsp;   private List<Node> getNodesToAvoid(GameMap gameMap) {

&nbsp;       List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());



&nbsp;       nodes.removeAll(gameMap.getObstaclesByTag("CAN\_GO\_THROUGH"));

&nbsp;       nodes.addAll(gameMap.getOtherPlayerInfo());

&nbsp;       return nodes;

&nbsp;   }



&nbsp;   private String findPathToGun(GameMap gameMap, List<Node> nodesToAvoid, Player player) {

&nbsp;       Weapon nearestGun = getNearestGun(gameMap, player);

&nbsp;       if (nearestGun == null) return null;

&nbsp;       return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearestGun, false);

&nbsp;   }



&nbsp;   private Weapon getNearestGun(GameMap gameMap, Player player) {

&nbsp;       List<Weapon> guns = gameMap.getAllGun();

&nbsp;       Weapon nearestGun = null;

&nbsp;       double minDistance = Double.MAX\_VALUE;



&nbsp;       for (Weapon gun : guns) {

&nbsp;           double distance = PathUtils.distance(player, gun);

&nbsp;           if (distance < minDistance) {

&nbsp;               minDistance = distance;

&nbsp;               nearestGun = gun;

&nbsp;           }

&nbsp;       }

&nbsp;       return nearestGun;

&nbsp;   }



}

Do mình vừa sửa code nên mình phải chạy lại code



Để nhanh hơn thì mình tắt đi bật lại game, sau đó thao tác lại bước 4



Do cần phải có nhiều hơn 1 người mới vào chơi được nên bạn chọn Add player sau đó chọn Play



Như vậy là mình đã hoàn thành xong các hướng dẫn cần thiết để có thể chơi game

