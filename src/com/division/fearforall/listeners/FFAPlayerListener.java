package com.division.fearforall.listeners;

import static com.division.common.utils.LocationTools.toVector;
import com.division.fearforall.core.FearForAll;
import com.division.fearforall.events.*;
import com.division.fearforall.events.PlayerDeathInArenaEvent.DeathCause;
import com.division.fearforall.regions.HealRegion;
import com.division.fearforall.regions.Region;
import com.division.fearforall.regions.Selection;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 *
 * @author Evan
 */
public class FFAPlayerListener implements Listener {

    FearForAll FFA;

    public FFAPlayerListener(FearForAll instance) {
        this.FFA = instance;
        FFA.getServer().getPluginManager().registerEvents(this, FFA);

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent evt) {
        if (evt.getPlayer().getItemInHand().getTypeId() != 289) {
            return;
        }
        if (evt.getPlayer().hasPermission("fearforall.selection")) {
            if (evt.hasBlock()) {
                Block evtBlock = evt.getClickedBlock();
                ItemStack iih = evt.getItem();
                final int mat = Material.STICK.getId();
                if (iih != null) {
                    if (iih.getTypeId() == mat) {
                        if (evt.getAction() == Action.RIGHT_CLICK_BLOCK) {
                            Selection.setP2(toVector(evtBlock));
                            evt.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Set Point 1: " + toVector(evtBlock));
                            evt.setCancelled(true);
                        }
                        if (evt.getAction() == Action.LEFT_CLICK_BLOCK) {
                            Selection.setP1(toVector(evtBlock));
                            evt.getPlayer().sendMessage(ChatColor.LIGHT_PURPLE + "Set Point 2: " + toVector(evtBlock));
                            evt.setCancelled(true);
                        }
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMoveEvent(PlayerMoveEvent evt) {
        if (evt.isCancelled()) {
            return;
        }
        Location from = evt.getFrom();
        Location to = evt.getTo();
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }
        Player evtPlayer = evt.getPlayer();
        if (FFA.isInTeleportQueue(evtPlayer)) {
            FFA.removePlayerFromTeleportQueue(evtPlayer);
            evtPlayer.sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " Teleport has been cancelled.");
            return;
        }
        Region region = FFA.getRegion();
        HealRegion healRegion = FFA.getHealRegion();
        Vector pt = toVector(to);
        Vector pf = toVector(from);
        World world = evtPlayer.getWorld();
        if (region.contains(world, pt)) {
            if (healRegion != null) {
                if (healRegion.contains(world, pt) && !healRegion.contains(world, pf)) {
                    evtPlayer.sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " You have entered the heal region.");
                }
                if (!healRegion.contains(world, pt) && healRegion.contains(world, pf)) {
                    evtPlayer.sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " You have left the heal region.");
                }
            }
        }
        if (region.contains(world, pt) && !region.contains(world, pf)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerEnteredArenaEvent(evtPlayer, pf, pt, MoveMethod.MOVED));
        } else if (!region.contains(world, pt) && region.contains(world, pf)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerLeftArenaEvent(evtPlayer, pf, pt, MoveMethod.MOVED));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreProcess(PlayerCommandPreprocessEvent evt) {
        if (evt.getPlayer().hasPermission("fearforall.bypass") || evt.getMessage().subSequence(1, 4).equals("ffa")) {
            return;
        }
        World world = evt.getPlayer().getWorld();
        Vector pt = toVector(evt.getPlayer().getLocation());
        Region region = FFA.getRegion();
        if (region.contains(world, pt)) {
            evt.setCancelled(true);
            evt.getPlayer().sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " You cannot use commands in the arena.");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent evt) {
        Player p = evt.getEntity();
        World world = p.getWorld();
        Vector pt = toVector(p.getLocation());
        Region region = FFA.getRegion();
        if (region.contains(world, pt)) {
            evt.getDrops().clear();
            EntityDamageEvent ede = p.getLastDamageCause();
            if (ede instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent edee = (EntityDamageByEntityEvent) ede;
                if (edee.getDamager() instanceof Player) {
                    FFA.getServer().getPluginManager().callEvent(new PlayerKilledPlayerInArenaEvent(p, (Player) edee.getDamager()));
                } else {
                    FFA.getServer().getPluginManager().callEvent(new PlayerDeathInArenaEvent(p, DeathCause.ENVIRONMENT));
                }
            } else {
                FFA.getServer().getPluginManager().callEvent(new PlayerDeathInArenaEvent(p, DeathCause.ENVIRONMENT));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerDrop(PlayerDropItemEvent evt) {
        World world = evt.getPlayer().getWorld();
        Vector pt = toVector(evt.getPlayer().getLocation());
        Region region = FFA.getRegion();
        if (region.contains(world, pt)) {
            evt.setCancelled(true);
            evt.getPlayer().sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " You are not allowed to drop items in this area.");
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerBreak(BlockBreakEvent evt) {
        if (evt.getPlayer().hasPermission("fearforall.selection")) {
            if (evt.getPlayer().getItemInHand().getType() == Material.STICK) {
                evt.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent evt) {
        Player evtPlayer = evt.getPlayer();
        World world = evtPlayer.getWorld();
        Location loc = evt.getRespawnLocation();
        Vector rt = toVector(loc);
        Region region = FFA.getRegion();
        if (!region.contains(world, rt)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerLeftArenaEvent(evtPlayer, null, rt, MoveMethod.RESPAWNED));
        } else {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerEnteredArenaEvent(evtPlayer, null, rt, MoveMethod.RESPAWNED));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerTeleport(PlayerTeleportEvent evt) {
        if (!evt.isCancelled()) {
            Player evtPlayer = evt.getPlayer();
            World world = evtPlayer.getWorld();
            Region region = FFA.getRegion();
            Vector tTo = toVector(evt.getTo());
            Vector tFrom = toVector(evt.getPlayer().getLocation());
            if (region.contains(world, tFrom)) {
                evt.setCancelled(true);
                return;
            }
            if (region.contains(world, tTo)) {
                FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerEnteredArenaEvent(evtPlayer, null, tTo, MoveMethod.TELEPORTED));
            } else if(region.contains(world, tFrom)) {
                FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerLeftArenaEvent(evtPlayer, null, tTo, MoveMethod.TELEPORTED));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerKick(PlayerKickEvent evt) {
        Player evtPlayer = evt.getPlayer();
        World world = evtPlayer.getWorld();
        Location loc = evtPlayer.getLocation();
        Vector pt = toVector(loc);
        Region region = FFA.getRegion();
        if (region.contains(world, pt)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerQuitInArenaEvent(evtPlayer));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerQuit(PlayerQuitEvent evt) {
        Player evtPlayer = evt.getPlayer();
        World world = evtPlayer.getWorld();
        Location loc = evtPlayer.getLocation();
        Vector pt = toVector(loc);
        Region region = FFA.getRegion();
        if (FFA.isInTeleportQueue(evtPlayer)) {
            FFA.removePlayerFromTeleportQueue(evtPlayer);
            evtPlayer.sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " Teleport has been cancelled.");
        }
        if (region.contains(world, pt)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerQuitInArenaEvent(evtPlayer));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent evt) {
        if (evt.isCancelled()) {
            return;
        }
        if (!(evt.getEntity() instanceof Player)) {
            return;
        }
        Player evtPlayer = (Player) evt.getEntity();
        if (FFA.isInTeleportQueue(evtPlayer)) {
            FFA.removePlayerFromTeleportQueue(evtPlayer);
            evtPlayer.sendMessage(ChatColor.YELLOW + "[FearForAll]" + ChatColor.RED + " Teleport has been cancelled.");
            return;
        }
        Location loc = evtPlayer.getLocation();
        World world = evtPlayer.getWorld();
        Vector pt = toVector(loc);
        Region region = FFA.getRegion();
        if (region.contains(world, pt)) {
            if (evt instanceof EntityDamageByEntityEvent) {
                EntityDamageByEntityEvent edee = (EntityDamageByEntityEvent) evt;
                FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerDamageInArenaEvent(evtPlayer, edee.getDamager(), evt.getCause(), evt));
            } else {
                FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerDamageInArenaEvent(evtPlayer, null, evt.getCause(), evt));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent evt) {
        if (FearForAll.getInstance().isUsingLeaderBoards()) {
            FFA.getDataInterface().createPlayerAccount(evt.getPlayer().getName());
        }
        Player evtPlayer = evt.getPlayer();
        World world = evtPlayer.getWorld();
        Vector pt = toVector(evtPlayer.getLocation());
        Region region = FFA.getRegion();
        if (!region.contains(world, pt)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerPreCheckEvent(evtPlayer));
        }
        if (region.contains(world, pt)) {
            FearForAll.getInstance().getServer().getPluginManager().callEvent(new PlayerEnteredArenaEvent(evtPlayer, null, pt, MoveMethod.JOINED));
        }
    }
}
