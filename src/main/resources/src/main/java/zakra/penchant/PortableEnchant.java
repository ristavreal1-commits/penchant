package zakra.penchant;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.MenuType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public final class PortableEnchant extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("PortableEnchant успешно запущен для 1.21.11!");
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

        MenuType.ENCHANTMENT.builder()
                .checkReachable(false)
                .build(player)
                .open();

        return true;
    }
}
