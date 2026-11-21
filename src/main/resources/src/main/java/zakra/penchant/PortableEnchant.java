package zakra.penchant;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class PortableEnchant extends JavaPlugin implements Listener {

    private final Set<UUID> portableUsers = new HashSet<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("enchanttable") != null) {
            getCommand("enchanttable").setExecutor(this);
        }
        getLogger().info("PortableEnchant (Spigot 1.21.11, 30 lvl) успешно запущен!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Эту команду может использовать только игрок.");
            return true;
        }

        if (!player.hasPermission("portableenchant.use")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использование этой команды.");
            return true;
        }

        portableUsers.add(player.getUniqueId());

        // В Spigot 1.21.11 title передаётся обычной строкой String
        MenuType.ENCHANTMENT.builder()
                .title("Зачарование")
                .checkReachable(false)
                .build(player)
                .open();

        return true;
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        if (!portableUsers.contains(event.getEnchanter().getUniqueId())) {
            return;
        }

        ItemStack item = event.getItem();
        List<Enchantment> validEnchants = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            if (ench.canEnchantItem(item)) {
                validEnchants.add(ench);
            }
        }

        if (validEnchants.isEmpty()) {
            return;
        }

        EnchantmentOffer[] offers = event.getOffers();
        int[] costs = {
                6 + random.nextInt(5),   // 1 слот: 6-10 lvl
                15 + random.nextInt(6),  // 2 слот: 15-20 lvl
                30                       // 3 слот: всегда 30 lvl
        };

        for (int i = 0; i < offers.length; i++) {
            Collections.shuffle(validEnchants, random);
            Enchantment chosen = validEnchants.get(0);
            int maxLvl = chosen.getMaxLevel();
            int level = (i == 2) ? maxLvl : Math.max(1, Math.min(maxLvl, (i + 1) * maxLvl / 3 + random.nextInt(2)));
            offers[i] = new EnchantmentOffer(chosen, level, costs[i]);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        portableUsers.remove(event.getPlayer().getUniqueId());
    }
}
