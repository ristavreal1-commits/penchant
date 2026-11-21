package zakra.penchant;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class PortableEnchant extends JavaPlugin implements Listener {

    private final Map<UUID, EnchantmentOffer[]> activeOffers = new HashMap<>();
    private final Random random = new Random();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        if (getCommand("enchanttable") != null) {
            getCommand("enchanttable").setExecutor(this);
        }
        getLogger().info("PortableEnchant (Spigot 1.21.11, 30 lvl + Lapis) успешно запущен!");
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

        activeOffers.put(player.getUniqueId(), new EnchantmentOffer[3]);

        InventoryView view = MenuType.ENCHANTMENT.builder()
                .title("Зачарование")
                .checkReachable(false)
                .build(player);
        player.openInventory(view);

        return true;
    }

    private boolean isTableEnchantable(Enchantment ench, ItemStack item) {
        // Исключаем сокровища (Починка, Ледоход и т.д.) и проклятия, как в ванильном столе
        if (ench.isTreasure() || ench.isCursed()) {
            return false;
        }
        if (item.getType() == Material.BOOK) {
            return true;
        }
        return ench.canEnchantItem(item);
    }

    @EventHandler
    public void onPrepareEnchant(PrepareItemEnchantEvent event) {
        UUID uuid = event.getEnchanter().getUniqueId();
        if (!activeOffers.containsKey(uuid)) {
            return;
        }

        ItemStack item = event.getItem();
        List<Enchantment> validEnchants = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            if (isTableEnchantable(ench, item)) {
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
                30                       // 3 слот: 30 lvl
        };

        EnchantmentOffer[] saved = new EnchantmentOffer[3];
        for (int i = 0; i < offers.length; i++) {
            Collections.shuffle(validEnchants, random);
            Enchantment chosen = validEnchants.get(0);
            int maxLvl = chosen.getMaxLevel();
            int level = (i == 2) ? maxLvl : Math.max(1, Math.min(maxLvl, (i + 1) * maxLvl / 3 + random.nextInt(2)));
            EnchantmentOffer offer = new EnchantmentOffer(chosen, level, costs[i]);
            offers[i] = offer;
            saved[i] = offer;
        }
        activeOffers.put(uuid, saved);
    }

    @EventHandler
    public void onEnchantItem(EnchantItemEvent event) {
        Player player = event.getEnchanter();
        UUID uuid = player.getUniqueId();
        if (!activeOffers.containsKey(uuid)) {
            return;
        }

        int button = event.whichButton(); // 0, 1 или 2
        int lapisRequired = button + 1;   // 1, 2 или 3 лазурита
        EnchantmentOffer[] saved = activeOffers.get(uuid);
        EnchantmentOffer chosenOffer = (saved != null && button >= 0 && button < saved.length) ? saved[button] : null;

        if (chosenOffer == null) {
            event.setCancelled(true);
            return;
        }

        int requiredLevel = chosenOffer.getCost();

        // Проверка уровня и лазурита (если игрок не в креативе)
        if (player.getGameMode() != GameMode.CREATIVE) {
            if (player.getLevel() < requiredLevel) {
                event.setCancelled(true);
                return;
            }

            if (event.getInventory() instanceof EnchantingInventory inv) {
                ItemStack lapis = inv.getSecondary();
                if (lapis == null || lapis.getType() != Material.LAPIS_LAZULI || lapis.getAmount() < lapisRequired) {
                    event.setCancelled(true);
                    return;
                }
                // Списываем лазурит
                if (lapis.getAmount() == lapisRequired) {
                    inv.setSecondary(null);
                } else {
                    lapis.setAmount(lapis.getAmount() - lapisRequired);
                }
            }
            // Списываем уровни опыта (как в ваниле: 1, 2 или 3 уровня)
            player.setLevel(player.getLevel() - lapisRequired);
        }

        // Отменяем кривое ванильное зачарование 1-го уровня и применяем правильное
        event.setCancelled(true);

        ItemStack item = event.getItem();
        if (item.getType() == Material.BOOK) {
            item.setType(Material.ENCHANTED_BOOK);
            if (item.getItemMeta() instanceof EnchantmentStorageMeta meta) {
                meta.addStoredEnchant(chosenOffer.getEnchantment(), chosenOffer.getEnchantmentLevel(), true);
                addBonusEnchants(meta, item, chosenOffer.getEnchantment(), button);
                item.setItemMeta(meta);
            }
        } else {
            item.addUnsafeEnchantment(chosenOffer.getEnchantment(), chosenOffer.getEnchantmentLevel());
            addBonusEnchantsToItem(item, chosenOffer.getEnchantment(), button);
        }

        if (event.getInventory() instanceof EnchantingInventory inv) {
            inv.setItem(item);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 1.0f);
        activeOffers.put(uuid, new EnchantmentOffer[3]);
    }

    private void addBonusEnchantsToItem(ItemStack item, Enchantment primary, int button) {
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            if (ench != primary && !ench.conflictsWith(primary) && isTableEnchantable(ench, item)) {
                pool.add(ench);
            }
        }
        Collections.shuffle(pool, random);

        // На 3-м слоте (30 lvl) даём шанс на 1-2 дополнительных чара, как в ваниле
        int bonusCount = (button == 2 && random.nextInt(100) < 60) ? (random.nextBoolean() ? 1 : 2) : 0;
        for (int i = 0; i < Math.min(bonusCount, pool.size()); i++) {
            Enchantment bonus = pool.get(i);
            boolean conflict = false;
            for (Enchantment existing : item.getEnchantments().keySet()) {
                if (bonus.conflictsWith(existing)) {
                    conflict = true;
                    break;
                }
            }
            if (!conflict) {
                int lvl = Math.max(1, random.nextInt(bonus.getMaxLevel()) + 1);
                item.addUnsafeEnchantment(bonus, lvl);
            }
        }
    }

    private void addBonusEnchants(EnchantmentStorageMeta meta, ItemStack book, Enchantment primary, int button) {
        List<Enchantment> pool = new ArrayList<>();
        for (Enchantment ench : Enchantment.values()) {
            if (ench != primary && !ench.conflictsWith(primary) && isTableEnchantable(ench, book)) {
                pool.add(ench);
            }
        }
        Collections.shuffle(pool, random);

        int bonusCount = (button == 2 && random.nextInt(100) < 50) ? 1 : 0;
        for (int i = 0; i < Math.min(bonusCount, pool.size()); i++) {
            Enchantment bonus = pool.get(i);
            int lvl = Math.max(1, random.nextInt(bonus.getMaxLevel()) + 1);
            meta.addStoredEnchant(bonus, lvl, true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeOffers.remove(event.getPlayer().getUniqueId());
    }
}
