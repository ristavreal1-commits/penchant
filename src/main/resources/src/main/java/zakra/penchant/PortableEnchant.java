package zakra.penchant;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PortableEnchant extends JavaPlugin implements Listener {

    private final Set<UUID> portableUsers = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("PortableEnchant (30 lvl) успешно запущен для 1.21.11!");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }

        if (!player.hasPermission("portableenchant.use")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
            return true;
        }

        portableUsers.add(player.getUniqueId());
        player.openEnchanting(null, true);
        return true;
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (portableUsers.contains(event.getEnchanter().getUniqueId())) {
            // 15 книжных полок = максимальный 30-й уровень зачарования
            event.setEnchantmentBonus(15);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        portableUsers.remove(event.getPlayer().getUniqueId());
    }
}
