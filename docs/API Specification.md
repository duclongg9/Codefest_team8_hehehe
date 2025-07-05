Tài liệu này liệt kê tổng quan về tất cả các class và method được sử dụng trong bộ SDK của Codefest 2025.



**Hero** - đại diện cho người chơi

**GameMap** - đại diện cho các thông tin trên map

**Inventory** - đại diện cho các thông tin kho đồ

**Entity** - các thực thể có trên map

**PathUtils** - các thuật toán utils hỗ trợ người chơi



### **------------------Hero---------------------**

Package: jsclub.codefest.sdk.model

Mô tả: Class chứa các method sử dụng để tạo người chơi cũng như kết nối với Server thi đấu.

1. getGameMap
GameMap getGameMap()
Mô tả: Trả về tất cả thông tin của game.
Tham số: Không có.
Trả về: gameMap - Thông tin của game.
2. getInventory
Inventory getInventory()
Mô tả: Lấy thông tin về danh sách vũ khí của người dùng
Tham số: Không có
Trả về:
Inventory: Danh sách vũ khí của người dùng
3. move
void move(String move)
Mô tả: Hàm giúp người dùng di chuyển
Tham số: move: Một dãy các chỉ dẫn "lrud..." (left - right - up - down) giúp người dùng di chuyển
Trả về: Không có
4. shoot
void shoot(String direction)
Mô tả: Hàm giúp người dùng vũ khí tầm xa (gun)
Tham số: direction: Hướng sẽ bắn ("l"/"r"/"u"/"d")
Trả về: Không có
5. attack
void attack(String direction)
Mô tả: Hàm giúp người dùng tấn công cận chiến (melee)
Tham số: direction: Hướng đánh ("l"/"r"/"u"/"d")
Trả về: Không có
6. throwItem
void throwItem(String direction)
Mô tả: Hàm giúp người chơi ném vũ khí dạng ném (throwable)
Tham số:
direction: Hướng ném vũ khí ("l"/"r"/"u"/"d")
Trả về: Không có
7. useSpecial
void useSpecial(String direction)
Mô tả: Hàm giúp người chơi dùng vũ khí dạng đặc biệt (special)
Tham số: direction: Hướng dùng vũ khí ("l"/"r"/"u"/"d")
Trả về: Không có
8. pickupItem
void pickupItem()
Mô tả: Hàm giúp người chơi nhặt đồ (nếu vị trí đồ trùng vị trí người chơi)
Tham số: Không có
Trả về: Không có
9. useItem
void useItem(String itemId)
Mô tả: Hàm giúp người chơi sử dụng vật phẩm trị thương (healingItem)
Tham số: itemId: id của item
Trả về: Không có
10. revokeItem
void revokeItem(String itemId)
Mô tả: Hàm giúp người chơi bỏ đồ ra khỏi túi
Tham số: itemId: id của item
Trả về: Không có
11. getEffects
List<Effect> getEffects()
Mô tả: Lấy thông tin về danh sách hiệu ứng áp dụng lên người dùng
Tham số: Không có
Trả về:
List<Effect>: Danh sách hiệu ứng của người dùng



### **-----------------GameMap-------------------------**



Package: jsclub.codefest.sdk.model

Mô tả: Class chứa các method sử dụng để người chơi lấy, cập nhật thông tin của map.

1. getElementByIndex
Element getElementByIndex(int x, int y)
Mô tả: Lấy Element ở vị trí x, y được truyền vào.
Tham số:
x: vị trí x
y: vị trí y
Trả về:
Element: Element
2. getMapSize
int getMapSize()
Mô tả: Lấy độ rộng của map
Tham số: Không có
Trả về:
int: độ rộng của map
3. getSafeZone
int getSafeZone()
Mô tả: Lấy bán kính vùng an toàn (tính từ tâm của map)
Tham số: Không có
Trả về:
int: bán kính vùng an toàn của map
4. getListWeapons
List<Weapon> getListWeapons()
Mô tả: Lấy thông tin tất cả vũ khí trên bản đồ.
Tham số: Không có
Trả về:
List<Weapon>: danh sách vũ khí
5. getAllGun / getAllMelee / getAllThrowable/ getAllSpecial
List<Weapon> getAllGun()
List<Weapon> getAllMelee()
List<Weapon> getAllThrowable()
List<Weapon> getAllSpecial()
Mô tả: Lấy thông tin tất cả vũ khí loại Gun / Melee / Throwable / Special tương ứng trên bản đồ.
Tham số: Không có
Trả về:
List<Weapon>: danh sách vũ khí loại Gun / Melee / Throwable / Special tương ứng
6. getListEnemies
List<Enemy> getListEnemies()
Mô tả: Lấy thông tin tất cả Enemy(kẻ thù gây sát thương) trên bản đồ
Tham số: Không có
Trả về:
List<Enemy>: danh sách kẻ thù
7. getListAllies
List<Ally> getListAllies()
Mô tả: Lấy thông tin tất cả Ally(NPC đồng minh) trên bản đồ
Tham số: Không có
Trả về:
List<Ally>: danh sách đồng minh
8. getListObstacles
List<Obstacle> getListObstacles()
Mô tả: Lấy thông tin tất cả vật cản trên bản đồ.
Tham số: Không có
Trả về:
List<Obstacle>: danh sách obstacle
9. getListIndestructibles
List<Obstacle> getListIndestructibles()
Mô tả: Lấy thông tin tất cả vật cản theo loại Indestructible trên bản đồ
Tham số: Không có
Trả về:
List<Obstacle>: danh sách obstacle
10. getObstaclesByTag
List<Obstacle> getObstaclesByTag(String tag)
Mô tả: Lấy thông tin về tất cả Obstacle trên bản đồ theo mỗi loại thuộc tính của nó.
Tham số: tag: Thẻ thuộc tính:
"DESTRUCTIBLE"
"TRAP"
"CAN_GO_THROUGH"
"CAN_SHOOT_THROUGH"
"PULLABLE_ROPE"
"HERO_HIT_BY_BAT_WILL_BE_STUNNED".
Trả về:
List<Obstacle>: danh sách obstacle theo thuộc tính tương ứng.
11. getListSupportItems
List<SupportItem> getListSupportItems()
Mô tả: Lấy thông tin tất cả vật phẩm hỗ trợ trên bản đồ.
Tham số: Không có
Trả về:
List<SupportItem>: danh sách vật phẩm hỗ trợ.
12. getListArmors
List<Armor> getListArmors()
Mô tả: Lấy thông tin tất cả trang bị trên bản đồ (cả armor và helmet).
Tham số: Không có
Trả về:
List<Armor>: danh sách trang bị
13. getListBullets
List<Bullet> getListBullets()
Mô tả: Lấy thông tin tất cả đạn trên bản đồ.
Tham số: Không có
Trả về:
List<Bullet>: danh sách đạn
14. getOtherPlayerInfo
List<Player> getOtherPlayerInfo()
Mô tả: Lấy thông tin tất cả các chơi khác(trừ mình) trên bản đồ.
Tham số: Không có
Trả về:
List<Player>: danh sách người chơi
15. getCurrentPlayer
Player getCurrentPlayer()
Mô tả: Lấy thông tin của người chơi mình điều khiển.
Tham số: Không có
Trả về:
Player: thông tin người chơi
16. getStepNumber
int getStepNumber()
Mô tả: Lấy ra step hiện tại của trò chơi(tính từ khi bắt đầu game).



### **-----------------Inventory------------------------**



Package: jsclub.codefest.sdk.model

Mô tả: Class chứa các method sử dụng để người chơi lấy, cập nhật thông tin của map.

1. Constructor
Inventory()
Mô tả: Inventory(): Khởi tạo kho đồ với vũ khí cận chiến mặc định là HAND.
Tham số: không có
Trả về: không có
Inventory(List<ItemData> items)
Mô tả: Inventory(): Khởi tạo kho đồ từ danh sách vật phẩm ban đầu. Tự động phân loại và thêm các vật phẩm tương ứng.
Tham số: items – danh sách các vật phẩm kiểu ItemData
Trả về: không có
2. getGun
Weapon getGun()
Mô tả: Lấy thông tin vũ khí súng hiện tại của người chơi.
Tham số: không có
Trả về: không có
3. getMelee
Weapon getMelee()
Mô tả: Lấy thông tin vũ khí cận chiến hiện tại.
Tham số: không có
Trả về: không có
4. getThrowable
Weapon getThrowable()
Mô tả: Lấy thông tin vũ khí dạng ném throwable trong kho đồ.
Tham số: không có
Trả về: không có
5. getSpecial
Weapon getSpecial()
Mô tả: Lấy thông tin vũ khí dạng đặc biệt special trong kho đồ.
Tham số: không có
Trả về: không có
6. getListSupportItem
List<SupportItem> getListSupportItem()
Mô tả: Lấy danh sách thông tin vật phẩm trị thương healingItem trong kho đồ.
Tham số: không có
Trả về: không có
7. getHelmet
Armor getHelmet()
Mô tả: Lấy thông tin trang bị helmet trong kho đồ.
Tham số: không có
Trả về: không có
8. getArmor
Armor getArmor()
Mô tả: Lấy thông tin trang bị armor trong kho đồ.
Tham số: không có
Trả về: không có

### **------------------Entity--------------------------**

Inventory
Mô tả: Kho đồ của người chơi, chứa thông tin các vũ khí như súng, cận chiến, vũ khí ném, vũ khí đặc biệt; thông tin trang bị và vật phẩm trị thương.

1. getGun
Weapon getGun()
Mô tả: Lấy thông tin vũ khí tầm xa trong kho đồ.
2. getMelee
Weapon getMelee()
Mô tả: Lấy thông tin vũ khí cận chiến trong kho đồ.
3. getThrowable
Weapon getThrowable()
Mô tả: Lấy thông tin vũ khí ném trong kho đồ.
4. getSpecial
Weapon getSpecial()
Mô tả: Lấy thông tin vũ khí đặc biệt trong kho đồ.
5. getListSupportItem
List<SupportItem> getListSupportItem()
Mô tả: Lấy danh sách thông tin vật phẩm hỗ trợ trong kho đồ.
6. getArmor
Armor getArmor()
Mô tả: Lấy danh sách thông tin giáp trong kho đồ.
7. getHelmet
Armor getHelmet()
Mô tả: Lấy danh sách thông tin mũ trong kho đồ.
Element
Mô tả: Bao gồm các thành phần trong trò chơi, bao gồm player, npc, weapon, obstacle,...

1. getId
String getId()
Mô tả: Lấy id của thành phần.
2. getType
ElementType getType()
Mô tả: Lấy phân loại của thành phần.
3. getX / getY
int getX()
int getY()
Mô tả: Lấy vị trí x, y của thành phần.
Weapon
Mô tả: Thông tin vũ khí, có 4 loại vũ khí là gun, melee, throwable, special

1. getPickupPoints
int getPickupPoints()
Mô tả: Lấy ra điểm số nhận được khi nhặt vũ khí.
2. getHitPoints
int getHitPoins()
Mô tả: Lấy ra số điểm nhận được khi vũ khí đánh trúng 1 mục tiêu.
3. getCooldown
double getCooldown()
Mô tả: Lấy ra thời gian hồi giữa mỗi lần sử dụng.
4. getDamage
int getDamage()
Mô tả: Lấy ra sát thương của vũ khí lên người chơi khác.
5. getRange
int[] getRange()
Mô tả: Lấy ra một mảng 2 phần tử mô tả phạm vi sử dụng vũ khí. Có dạng {a, b}.
Ví dụ: range {3, 1}. image
6. getExplodeRange
int getExplodeRange()
Mô tả: Lấy ra phạm vi nổ của vũ khí (chỉ áp dụng vũ khí ném).
7. getSpeed
int getSpeed()
Mô tả: Lấy ra tốc độ đạn(cell/s). (áp dụng với súng, vũ khí ném, vũ khí đặc biệt)
8. getUseCount
int getUseCount()
Mô tả: Lấy ra số lần sử dụng tối đa của vũ khí.
Bullet
1. getDamage
float getDamage()
Mô tả: Lấy ra sát thương của đạn.
2. getSpeed
int getSpeed()
Mô tả: Lấy ra tốc độ đạn bắn (cell/step).
3. getDestinationX
int getDestinationX()
Mô tả: Lấy ra tọa độ x ước tính mà đạn sẽ biến mất.
4. getDestinationY
int getDestinationY()
Mô tả: Lấy ra tọa độ y ước tính mà đạn sẽ biến mất.
Player
Mô tả: Thông tin người chơi.

1. getID
String getID()
Mô tả: Lấy ra ID của người chơi.
2. getScore
int getScore()
Mô tả: Lấy ra số điểm hiện tại của người chơi.
3. getHealth
Float getHealth()
Mô tả: Lấy ra số máu hiện tại của người chơi.
Enemy
1. getDamage
int getDamage()
Mô tả: Lấy ra sát thương của kẻ thù.
Ally
1. getHealingHP
int getHealingHP()
Mô tả: Lấy ra lượng HP hồi phục cho Hero của đồng minh.
Armor
1. getHealthPoint
double getHealthPoint()
Mô tả: Lấy ra số HP của trang bị.
2. getDamageReduce
int getDamageReduce()
Mô tả: Lấy ra phần trăm giảm sát thương của trang bị.
SupportItem
1. getHealingHP
int getHealingHP()
Mô tả: Lấy ra số máu được hồi trong 1 khoảng thời gian.
2. getUsageTime
double getUsageTime()
Mô tả: Lấy ra thời gian cần để dùng vật phẩm.
3. getPoint
int getPoint()
Mô tả: Lấy ra số điểm nhận được khi sử dụng vật phẩm.
Obstacle
1. getHp
int getHp()
Mô tả: Lấy ra máu ban đầu của vật cản.
2. getCurrentHp
int getCurrentHp()
Mô tả: Lấy ra máu hiện tại của vật cản.
3. getTags
List<ObstacleTag> getTags()
Mô tả: Lấy ra danh sách các thẻ thuộc tính của Obstacle(chi tiết các thẻ thuộc tính xem ở hàm getObstaclesByTag trong gameMap).
Effect
1. getDuration
int getDuration()
Mô tả: Lấy ra thời gian hiệu lực của Effect.
2. getAffectedAt
int getAffectedAt()
Mô tả: Lấy ra step mà effect được áp dụng.
3. getEstimatedEndAt
int getEstimatedEndAt()
Mô tả: Lấy ra step mà effect được xóa bỏ.
### **---------------------PathUtils---------------------------**

Package: jsclub.codefest.sdk.algorithm

Mô tả: Class chứa các method hỗ trợ người chơi.

1. getShortestPath
String getShortestPath(GameMap gameMap, List<Node> restrictedNodes, Node current, Node target, boolean skipDarkArea)
Mô tả: Hàm trả về 1 dãy chỉ dẫn là đường đi ngắn nhất để đi từ node current đến node target
Tham số:
gameMap: Thông tin về game mà người dùng cần truyền vào
restrictedNodes: Danh sách những node mà người dùng cần truyền vào để giúp bot né. (Bình xăng, trap...)
current: Vị trí hiện tại của người dùng
target: Vị trí của người dùng muốn đến
skipDarkArea: Nếu truyền vào true, bot sẽ không đi ra khỏi bo, và ngược lại.
Trả về:
String: Một dãy các chỉ dẫn liên tiếp để đi từ vị trí current đến vị trí target
Nếu người dùng hiện tại ở vị trí (x, y), sẽ có 4 hướng:
l: bot sẽ rẽ trái (x - 1, y)
r: bot sẽ rẽ phải (x + 1, y)
u: bot sẽ đi lên trên (x, y + 1)
d: bot sẽ đi xuống dưới (x, y - 1)
2. checkInsideSafeArea
boolean checkInsideSafeArea(Node x, int safeZone, int mapSize)
Mô tả: Hàm kiểm tra xem 1 vị trí node x có ở trong vùng an toàn hay không
Tham số:
x: vị trí mà bạn muốn kiểm tra
safeZone: Kích thước của vùng an toàn.
mapSize: Kích thước của bản đồ
Trả về:
true: Nếu node x ở trong vùng an toàn.
false: Nếu node x ở ngoài vùng an toàn.
3. distance
int distance(Node x, Node y)
Mô tả: Hàm tính khoảng cách Manhattan giữa 2 node trên bản đồ, dùng để xác định tương đối về khoảng cách phải di chuyển(distance = |x1-x2| + |y1-y2|)
Tham số:
x: vị trí node đầu tiên
y: vị trí node thứ hai.
Trả về:
int: Khoảng cách Manhattan giữa 2 node x và y.
