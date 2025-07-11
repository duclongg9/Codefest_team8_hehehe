package jsclub.codefest.sdk.socket.event_handler;

import com.google.gson.Gson;
import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.factory.SupportItemFactory;
import jsclub.codefest.sdk.factory.WeaponFactory;
import jsclub.codefest.sdk.model.ElementType;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.socket.data.receive_data.Item;
import jsclub.codefest.sdk.util.MsgPackUtil;

import java.io.IOException;

public class onPlayerInventoryClear implements Emitter.Listener {
    private final Inventory inventory;
    Gson gson = new Gson();

    public onPlayerInventoryClear(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public void call(Object... args) {
        try {
            String message = MsgPackUtil.decode(args[0]);
            Item itemData = gson.fromJson(message, Item.class);
            ElementType type = itemData.type;
            String id = itemData.ID;
            System.out.println("Item cleared: " + id);
            
            switch (type) {
                case GUN:
                    inventory.setGun(null);
                    break;
                case MELEE:
                    inventory.setMelee(WeaponFactory.getWeaponById("HAND"));
                    break;
                case THROWABLE:
                    inventory.setThrowable(null);
                    break;
                case SPECIAL:
                    inventory.setSpecial(null);
                    break;
                case ARMOR:
                    inventory.setArmor(null);
                    break;
                case HELMET:
                    inventory.setHelmet(null);
                    break;
                case SUPPORT_ITEM:
                    inventory.getListSupportItem().remove(SupportItemFactory.getSupportItemById(id));
                    break;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
