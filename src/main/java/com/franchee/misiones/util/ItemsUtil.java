package com.franchee.misiones.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/** Helpers para armar items de menu con nombre/lore en colores. */
public final class ItemsUtil {

    private ItemsUtil() {
    }

    public static ItemStack construir(Material material, String nombre, List<String> lore, boolean brillo) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Texto.color(nombre));

        List<Component> componentes = new ArrayList<>();
        for (String linea : lore) componentes.add(Texto.color(linea));
        meta.lore(componentes);

        if (brillo) meta.addEnchant(Enchantment.DURABILITY, 1, true);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack boton(Material material, String nombre, String... lore) {
        return construir(material, nombre, new ArrayList<>(List.of(lore)), false);
    }
}
