package com.division.fearforall.engines;

import com.division.common.utils.ItemArmor;
import com.division.fearforall.core.FearForAll;
import com.division.fearforall.core.PlayerStorage;
import com.division.fearforall.events.PlayerDamageInArenaEvent;
import com.division.fearforall.events.PlayerEnteredArenaEvent;
import com.division.fearforall.events.PlayerQuitInArenaEvent;
import com.tetra.combatprotector.CombatProtector;
import com.tetra.combatprotector.handlers.MarkHandler;
import java.util.ArrayList;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

/**
 *
 * @author Evan
 *
 */
@EngineInfo(author = "mastershake71",
version = "0.2.4EB",
depends = {"OfflineStorage"})
public class StorageEngine extends Engine {

    protected int aM;

    @Override
    public String getName() {
        return ("Storage");
    }
    private static ArrayList<PlayerStorage> massStorage = new ArrayList<PlayerStorage>();
    //public ArrayList<Player> playersInArena = new ArrayList<Player>();
    private OfflineStorageEngine OSE = null;

    public StorageEngine() {
    }

    @Override
    public void runStartupChecks() throws EngineException {
        Engine eng = FearForAll.getInstance().getEngineManger().getEngine("OfflineStorage");
        if (eng != null) {
            if (eng instanceof OfflineStorageEngine) {
                this.OSE = (OfflineStorageEngine) eng;
            }
        }
        if (OSE == null) {
            throw new EngineException("Missing Dependency.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDamagedInArena(PlayerDamageInArenaEvent evt) {
        Player victim = evt.getVictim();
        EntityDamageEvent ede = evt.getDamageEvent();
        EntityDamageByEntityEvent edee;
        if (ede instanceof EntityDamageByEntityEvent) {
            edee = (EntityDamageByEntityEvent) ede;
            Player attacker;
            if (edee.getDamager() instanceof Player) {
                attacker = (Player) edee.getDamager();
                getStorage(victim.getName()).setLastHit(attacker.getName());
                if (!evt.getDamageEvent().isCancelled()) {
                    int damage = getArmorRedox(attacker, evt.getDamageEvent().getDamage());
                    victim.damage(damage);
                    MarkHandler handler = CombatProtector.instance.markHandlers.get(victim);
                    if(handler.checkTagged(victim)){
                        handler.refreshTimer(victim);
                    } else{
                        handler.safeOff(victim);
                    }
                    CombatProtector.instance.getCombatLogger().AddEntry(victim, attacker, damage, CombatProtector.instance.cpel.checkWeapon(attacker));
                    for(ItemStack item: victim.getInventory().getArmorContents()){
                        item.setDurability((short)(item.getDurability()-3));
                    }
                    evt.getDamageEvent().setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuitInArena(PlayerQuitInArenaEvent evt) {
        Player evtPlayer = evt.getPlayer();
        if (hasStorage(evt.getPlayer())) {
            PlayerStorage pStorage = getStorage(evtPlayer.getName());
            if (evtPlayer.hasPotionEffect(PotionEffectType.SPEED)) {
                evtPlayer.removePotionEffect(PotionEffectType.SPEED);
            }
            if (!OSE.hasOfflineStorage(pStorage.getKey())) {
                OSE.covertPlayerStorage(pStorage);
            }
            //playersInArena.remove(evtPlayer);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEnteredArena(PlayerEnteredArenaEvent evt) {
        // if (checkAllowed(evt.getPlayer())) {
        if (!hasStorage(evt.getPlayer())) {
            addStorage(evt.getPlayer());
        } else {
            evt.setCancelled(true);
        }
        //  playersInArena.add(evt.getPlayer());
        //  } else {
        //     evt.getPlayer().sendMessage(ChatColor.YELLOW + "[FearForAll] " + ChatColor.RED + "There are already 2 instances of your ip in the arena.");
        //      evt.setCancelled(true);
        //  }
    }

    public void addStorage(Player key) {
        PlayerStorage pStorage = new PlayerStorage(key.getName(), key.getInventory());
        massStorage.add(pStorage);
    }

    public PlayerStorage getStorage(String rKey) {
        for (PlayerStorage ps : massStorage) {
            if (ps.getKey().equals(rKey)) {
                return ps;
            }
        }
        return null;
    }

    public void safeRestore(Player p) {
        if (!p.isDead()) {
            p.teleport(p.getWorld().getSpawnLocation());
        }
    }

    public boolean hasStorage(Player rkey) {
        if (getStorage(rkey.getName()) != null) {
            return true;
        }
        return false;
    }

    public void removeStorage(PlayerStorage pStorage) {
        massStorage.remove(pStorage);
    }

    public void saveAll() {
        for (PlayerStorage ps : massStorage) {
            createOfflineStorage(ps);
        }
    }

    public void createOfflineStorage(PlayerStorage pStorage) {
        if (pStorage != null) {
            if (!hasOfflineStorage(pStorage.getKey())) {
                System.out.println("[FearForAll] converting storage key: " + pStorage.getKey() + " to Offline Storage.");
                if (OSE.covertPlayerStorage(pStorage)) {
                    System.out.println("[FearForAll] storage key: " + pStorage.getKey() + " has been successfully converted.");
                } else {
                    System.out.println("[FearForAll] An error occured when converting storage key: " + pStorage.getKey());
                }
            }
        }
    }

//    public boolean checkAllowed(Player p) {
//        if (playersInArena.isEmpty()) {
//            return true;
//        }
//
//        int ipCount = 0;
//        String addr = p.getAddress().getHostName();
//        Player[] players = playersInArena.toArray(new Player[0]);
//        for (Player player : players) {
//            if (player == p) {
//                continue;
//            }
//            String addr2 = player.getAddress().getHostName();
//            if (addr.equals(addr2)) {
//                ipCount++;
//                continue;
//            }
//        }
//        if (ipCount >= 2) {
//            return false;
//        } else {
//            return true;
//        }
//    }
    public boolean hasOfflineStorage(String key) {
        return OSE.hasOfflineStorage(key);
    }

    public int aO(Player p) {
        return this.l(p);
    }

    public int l(Player p) {
        int i = 0;
        ItemStack[] aitemstack = p.getInventory().getArmorContents();
        int j = aitemstack.length;

        for (int k = 0; k < j; ++k) {
            ItemStack itemstack = aitemstack[k];

            int l = ItemArmor.valueOf(itemstack.getType().name()).b();

            i += l;
        }
        return i;
    }

    public int getArmorRedox(Player p, int i) {
        int j = 25 - this.aO(p);
        int k = i * j;

        this.k(p, i);
        i = k / 25;

        return i;
    }

    public void k(Player p, int i) {
        i /= 4;
        if (i < 1) {
            i = 1;
        }
        ItemStack[] armor = p.getInventory().getArmorContents();

        for (int j = 0; j < armor.length; ++j) {
            if (armor[j] != null) {
                armor[j].setDurability((short) (armor[j].getDurability() - i));
                if (armor[j].getDurability() == 0) {
                    armor[j] = null;
                }
            }
        }
    }
}
