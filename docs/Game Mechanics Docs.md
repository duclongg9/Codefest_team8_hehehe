**CODEFEST 2025** 

**GAME MECHANICS DOCUMENT**



I. GAME CONCEPT

Trở thành Hero, tham gia tấn công các đối thủ còn lại, thu thập vật phẩm để dành nhiều số điểm nhất đến khi kết thúc ván đấu thì sẽ dành chiến thắng.

Game sẽ được chia thành các step, mỗi step có độ dài 0.5s. Step là đơn vị thời gian được sử dụng trong mọi tính toán của game. Mỗi step là một chu kỳ trao đổi dữ liệu giữa game và client (sdk) và trong mỗi step hero chỉ thực hiện được một lệnh.



II. MAP

Mỗi một vòng chơi sẽ có map riêng biệt. Size map và thời gian của từng map config theo từng level:

Map to: 100\*100 (cell\*cell) → dành cho 8 đội chơi, tổng thời gian là 10p

Map trung bình: 70\*70 (cell\*cell) → dành cho 6-8 đội chơi, thời gian là 5p - 10p

Map nhỏ: 40\*40 (cell\*cell) → dành cho 1-3 đội chơi, thời gian là 5p

Map chứa hệ thống nhân vật và vật phẩm trong game

Súng sẽ được spawn liên tục trên map.

Hệ thống weapons còn lại (cận chiến, throwables, specials), healing/supporting items và armor/helmet sẽ được chứa trong các hộp vật phẩm. Hộp vật phẩm được spawn random từ đầu map với số lượng và vị trí cố định, không respawn trong quá trình cuộc đấu diễn ra. Hero tiến hành phá hủy hộp vật phẩm để nhận được vật phẩm tương ứng. Mỗi hộp vật phẩm sẽ cho ra 4 đồ bất kỳ.

Đồ gồm có:

Melees: 20%

Throwables: 30%

Special Weapons: 5%

Healing/Supporting items: 30%

Armor/Helmet: 15%

Trong thời gian diễn ra một trận đấu: Hero sẽ được spawn khi trận đấu bắt đầu, respawn khi bị chết trong lúc trận đấu diễn ra, riêng 45s cuối không respawn (Hero bị tử trận ở giây thứ 45 trước khi trận đấu kết thúc sẽ không được tiếp tục tham gia map đấu).



Chứa hai vùng chuyên biệt:

Vùng an toàn (Vùng sáng): như cái tên, trong đây thì không sao cả. Hero khi hy sinh sẽ được respawn trong khu vực an toàn.

Vùng tối: Khi hero ở ngoài vùng tối, sẽ mất 1 lượng HP\_LOSS/step.

&nbsp;

Damage của vùng tối theo thời gian của map

Nếu thời gian thi đấu còn lại của map đó >= ⅔ \* T thì HP\_LOSS = 5

Vòng bo ở sau 2/3 thời gian tổng thì cứ sau 10 giây bo sẽ đốt đau hơn 1HP cứ cộng dần thế theo cấp số cộng theo công thức sau :

HP\_LOSS = ceil( 5 + (⅓\*T - t)/10 )

Trong đó: 

Số máu bị đốt theo thời gian: HP\_LOSS

Thời gian tổng của map: T (giây)

Thời gian còn lại của map: t (giây)

ceil( ): hàm làm tròn lên 

Đơn vị tốc độ đốt: HP/s

Thông số của map:



















Map nhỏ

(vòng 1)

Map trung bình

(1 map 5p, 1 map 10p)

Map to

Size

40\*40

70\*70

100\*100

Minsize

\-

12\*12

24\*24

Số lượng hero

6 (1 bot hero + 1 bot btc + 4 bot mặc định)

6-8

8

Thời gian tổng map

5p

5-10p 

10p

Thời gian còn lại để bắt đầu cơ chế của vùng tối - vùng sáng

\-

5p: Giây thứ 270

10p: Giây thứ 570

10p: Giây thứ 570







III. CƠ CHẾ TÍNH ĐIỂM

Tiêu diệt 1 mạng: (có Streak, Hero sẽ biết mình đang Streak bao nhiêu)

Điểm nhận được khi kill 1 hero = 100 + 20\*( y-1 + y-2 + ... + 0). Trong đó: y là chuỗi kill hero liên tiếp mà mình không bị kill

Khi bị giết: -100đ

Nhặt được 1 Weapon: cộng điểm (config theo từng vật phẩm)

Sử dụng 1 item: cộng điểm (config theo từng vật phẩm)

Điểm nhận được khi vũ khí đánh trúng 1 mục tiêu → đánh trúng x mục tiêu trong cùng 1 thời điểm thì sẽ là:      (config theo từng vật phẩm)

x\*on-hit points (điểm)

Những Hero còn sống sót khi kết thúc game (0s cuối cùng) được cộng 200đ

Hệ số phụ: Số lần kill - Số lần bị kill

Nếu hai bot có cùng điểm khi kết thúc trận đấu, sẽ xem số lần kill, kill nhiều hơn sẽ chiến thắng.

Nếu hai bot có cùng điểm và số lần kill, thì bot nào bị giết ít hơn sẽ thắng.



IV. CÁC HIỆU ỨNG: \[CODEFEST 2025] - ELEMENTS GDD

Logic cộng dồn hiệu ứng sát thương theo thời gian:

Các loại sát thương theo thời gian (Poison, Bleed…) nếu xảy ra ở trên cùng một mục tiêu sẽ gây sát thương độc lập với nhau. Ví dụ mục tiêu bị trúng cả Poison lẫn Bleed thì 2 hiệu ứng này gây sát thương đồng thời trên người mục tiêu.

Nếu một mục tiêu đang bị trúng 1 loại hiệu ứng theo thời gian, rồi tiếp tục bị trúng cùng loại hiệu ứng đó nhưng từ nguồn khác thì thời gian tồn tại của hiệu ứng sẽ = thời gian duy trì mới > thời gian còn lại cũ ? thời gian duy trì mới : thời gian còn lại cũ.



V. HỆ THỐNG NHÂN VẬT VÀ VẬT PHẨM

Chi tiết: \[CODEFEST 2025] - ELEMENTS GDD

1\. Nhân vật:

a. Hero

Di chuyển:

Có thể di chuyển theo 4 hướng (trên/dưới/phải/trái)

Không thể đi qua được các obstacles không có tag “Can go through”.

Chỉ số của Hero:

Tốc độ: 1 cell/step

Kích thước: 1 cell

HP = 100

Điểm = 0

Cơ chế inventory:

Mỗi hero được sử dụng một vũ khí trong một lần.

Được mang theo tối đa mỗi loại một vũ khí (tối đa 4 vũ khí)

Số vật phẩm tiêu thụ tối đa được mang: 4

Số armor tối đa được mang: 1

Số helmet tối đa được mang: 1

Hero được drop đồ nếu thấy đồ có lợi hơn cho mình xuất hiện trên map, tuy nhiên đồ drop sẽ biến mất hoàn toàn khỏi map và không được respawn trong quá trình trận đấu diễn ra.

b. Enemy: Kẻ Thù

Tốc độ di chuyển: 1 cell / step

Quỹ đạo di chuyển: thẳng

Nếu gặp phải Hero trong quá trình di chuyển sẽ gây một sát thương (có thể có Effect) lên người Hero, không bị Hero tấn công.

c. Ally: Đồng Minh

Tốc độ di chuyển: 1 cell / step

Quỹ đạo di chuyển: thẳng

Nếu gặp phải Hero trong quá trình di chuyển sẽ hồi một lượng Hp cho Hero, không bị Hero tấn công.



2\. Hệ thống vật phẩm:

a. Obstacles

Chỉ số của obstacles:

Tag: Mỗi tag đại diện cho 1 thuộc tính của Obstacles (Destructible, Trap, Can go through, Can shoot through, Pullable rope, Hero hit by bat will be stunned)

Ví dụ: CHEST có 3 tag là Destructible, Pullable Rope, Hero hit by bat will be stunned => CHEST có thể bị phá huỷ, có thể dùng Dây Thừng lên CHEST, Hero bị đánh va vào CHEST sẽ bị choáng.

b. Healing Items

Healing/Supporting Items (Đồ trị thương/ Đồ hỗ trợ), khi sử dụng đồ sẽ có hiệu ứng đi kèm; gồm các chỉ số sau:

Usage time (s): thời gian cần để sử dụng item. Trong thời gian này, Hero không thể di chuyển hay tấn công.

HP hồi.

Effect: Hiệu ứng tác động lên Hero.

Duration(s): Thời gian hiệu ứng duy trì.

c. Armors

Armor/Helmet (Giáp/Mũ), gồm các chỉ số:

HP: chỉ số chịu đựng của Armor/Helmet. Khi HP = 0 thì Armor/Helmet sẽ biến mất.

Ví dụ: Giáp giảm 20% dame Hero nhận. Nếu Hero bị dính đòn đánh 80 Dame thì Hero nhận 64 Dame. Giáp nhận 16 Dame.

Damage reduction: Độ giảm sát thương của giáp (so với damage của vật tác dụng)

Ví dụ: Khi sử dụng Giáp Gai (damage reduction = 20%) mà chịu sự tấn công của vũ khí có damage là 20. Hero sẽ mất 80%\*20 HP.



d. Weapons

Gồm có vũ khí cận chiến, xạ chiến, vũ khí có thể ném được và vũ khí đặc biệt:

Cận chiến (Melees)

Xạ chiến (Guns)

Có thể ném được (Throwables)

Đặc biệt (Specials)



VI. CƠ CHẾ "THÍNH"

Sẽ có 1 con rồng lửa bay qua phía trên map, theo đường bay đã định sẵn (đường bay sẽ random mỗi khi đến thời gian spawn thính). Ở trong game thính sẽ ở dưới dạng 1 quả trứng rồng sặc sỡ. Thính sẽ luôn được spawn ở trong vùng sáng. Khi có con rồng bay qua map sẽ có tiếng đập cánh là dấu hiệu nhận biết thính sắp được spawn. (Không có hiệu ứng trứng rồng rơi từ trên xuống)

Đối với trận đấu 5 phút : 

Map 70x70 : Có tổng 2 thính sẽ được spawn. Bắt đầu spawn cái đầu tiên khi trận đấu còn 200s, spawn cái thứ 2 khi trận đấu còn 100s.

Đối với trận đấu 10 phút : 

Map 70x70 : Có tổng 4 thính sẽ được spawn. Bắt đầu spawn cái đầu tiên khi trận đấu còn 450s, spawn cái thứ 2 khi trận đấu còn 330s, spawn cái thứ 3 khi trận đấu còn 210s, spawn cái thứ 4 khi trận đấu còn 90s.

Map 100x100 : Có tổng 5 thính sẽ được spawn. Bắt đầu spawn cái đầu tiên khi trận đấu còn 520s, spawn cái thứ 2 khi trận đấu còn 440s, spawn cái thứ 3 khi trận đấu còn 360s, spawn cái thứ 4 khi trận đấu còn 240s, spawn cái thứ 5 khi trận đấu còn 120s. 

CHÚ Ý: Khi thính spawn ra thì những con bot của người chơi hay các vật phẩm, rương,... sẽ biến mất tại ô xuất hiện quả trứng rồng ấy. Không thông báo cho người chơi biết vị trí của quả trứng sẽ rơi ở đâu, chỉ để con rồng bay qua cho người xem biết là sắp có trứng rồng.

