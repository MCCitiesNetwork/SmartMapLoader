package io.github.md5sha256.SmartMapLoader;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import java.util.Arrays;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerShowEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapView;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartMapLoader extends JavaPlugin implements Listener {
   public void onEnable() {
      this.getServer().getPluginManager().registerEvents(this, this);
   }

   public void onDisable() {
   }

   @EventHandler
   public void onChunkSend(PlayerChunkLoadEvent event) {
      Entity[] entities = event.getChunk().getEntities();
      Player player = event.getPlayer();
      Arrays.stream(entities)
            .filter(ItemFrame.class::isInstance)
            .map(ItemFrame.class::cast)
            .forEach(frame -> {
               ItemStack item = frame.getItem();
               if (item != null) {
                  this.showMapItem(item, player);
               }
            });
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

   private void showMapItem(ItemStack item, Player player) {
      if (item == null || item.getType() != Material.FILLED_MAP || !item.hasItemMeta()) {
         return;
      }
      
      ItemMeta meta = item.getItemMeta();
      if (!(meta instanceof MapMeta mapMeta)) {
         return;
      }
      
      MapView mapView = mapMeta.getMapView();
      if (mapView != null) {
         player.sendMap(mapView);
      }
   }
}

