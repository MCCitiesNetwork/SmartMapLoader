package io.github.md5sha256.SmartMapLoader;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartMapLoader extends JavaPlugin implements Listener {
   private static final long MAP_SEND_DEDUPE_MS = 300L;
   private final Map<UUID, Map<Integer, Long>> lastMapSendByPlayer = new HashMap<>();

   public void onEnable() {
      this.getServer().getPluginManager().registerEvents(this, this);
   }

   public void onDisable() {
   }

   @EventHandler
   public void onChunkSend(PlayerChunkLoadEvent event) {
      Entity[] entities = event.getChunk().getEntities();
      Player player = event.getPlayer();
      for (Entity entity : entities) {
         if (!(entity instanceof ItemFrame frame)) {
            continue;
         }

         ItemStack item = frame.getItem();
         if (item != null) {
            this.showMapItem(item, player);
         }
      }
   }

   @EventHandler
   public void onItemFrameShow(PlayerShowEntityEvent event) {
      Entity entity = event.getEntity();
      if (entity instanceof ItemFrame itemFrame) {
         ItemStack item = itemFrame.getItem();
         if (item != null) {
            this.showMapItem(item, event.getPlayer());
         }
      }
   }

   @EventHandler
   public void onMapPlacedInItemFrame(PlayerInteractEntityEvent event) {
      if (!(event.getRightClicked() instanceof ItemFrame itemFrame)) {
         return;
      }

      ItemStack heldItem = event.getPlayer().getInventory().getItem(event.getHand());
      if (heldItem == null || heldItem.getType() != Material.FILLED_MAP) {
         return;
      }

      this.getServer().getScheduler().runTask(this, () -> {
         ItemStack frameItem = itemFrame.getItem();
         if (frameItem == null || frameItem.getType() != Material.FILLED_MAP) {
            return;
         }

         for (Player viewer : itemFrame.getTrackedBy()) {
            this.showMapItem(frameItem, viewer);
         }
      });
   }

   @EventHandler
   public void onPlayerQuit(PlayerQuitEvent event) {
      this.lastMapSendByPlayer.remove(event.getPlayer().getUniqueId());
   }

   private void showMapItem(ItemStack item, Player player) {
      if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) {
         return;
      }
      
      ItemMeta meta = item.getItemMeta();
      if (!(meta instanceof MapMeta mapMeta)) {
         return;
      }
      
      MapView mapView = mapMeta.getMapView();
      if (mapView != null && this.shouldSendMap(player, mapView.getId())) {
         player.sendMap(mapView);
      }
   }

   private boolean shouldSendMap(Player player, int mapId) {
      long now = System.currentTimeMillis();
      UUID playerId = player.getUniqueId();
      Map<Integer, Long> lastSentByMap = this.lastMapSendByPlayer.computeIfAbsent(playerId, key -> new HashMap<>());
      Long lastSentAt = lastSentByMap.get(mapId);
      if (lastSentAt != null && now - lastSentAt < MAP_SEND_DEDUPE_MS) {
         return false;
      }

      lastSentByMap.put(mapId, now);
      return true;
   }
}

