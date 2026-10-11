package com.franchee.misiones;

import com.franchee.misiones.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Interpreta y entrega las recompensas de una mision.
 *
 * Formatos (una linea cada uno):
 *   item:MATERIAL:CANTIDAD
 *   especial:NOMBRE[:CANTIDAD]   (items de tus otros plugins)
 *   comando:COMANDO              (consola, %jugador% = nombre)
 *
 * Los items "especiales" se piden por REFLEXION a los otros plugins en el
 * momento de entregar - asi este plugin NO depende de ellos para compilar ni
 * para arrancar, y si falta uno (o tiene una version vieja) solo falla esa
 * recompensa puntual, no todo el plugin.
 */
public class RecompensaService {

    // token -> { plugin, metodo1, metodo2, ... }  (el ultimo metodo devuelve el ItemStack)
    private static final Map<String, String[]> ESPECIALES = Map.of(
            "libro_florencia", new String[]{"LibroFlorenciaPlugin", "getItemFactory", "crearLibro"},
            "cuerno_cabra", new String[]{"JefeCabraPlugin", "getItemFactory", "crearCuernoInvocador"},
            "yelmo_testarazo", new String[]{"JefeCabraPlugin", "getItemFactory", "crearYelmoTestarazo"},
            "pava", new String[]{"MateSystemPlugin", "getItemFactory", "crearPava"},
            "mate_vacio", new String[]{"MateSystemPlugin", "getItemFactory", "crearMateVacio"},
            "sudario_aparecido", new String[]{"EventoPlugin", "getEventoManager", "getItemFactory", "crearSudario"},
            "vestigio_aparecido", new String[]{"EventoPlugin", "getEventoManager", "getItemFactory", "crearVestigioDelAparecido"}
    );

    private static final Map<String, String> NOMBRES_ESPECIALES = Map.of(
            "libro_florencia", "El Reinado Perdido",
            "cuerno_cabra", "Cuerno de Cabra Maldito",
            "yelmo_testarazo", "Yelmo del Testarazo",
            "pava", "Pava de Hierro Encantada",
            "mate_vacio", "Mate",
            "sudario_aparecido", "Sudario del Aparecido",
            "vestigio_aparecido", "Vestigio del Aparecido"
    );

    /** Los tokens de "especial:" en orden estable (para el editor). */
    public static final List<String> TOKENS = List.of(
            "libro_florencia", "cuerno_cabra", "yelmo_testarazo", "pava",
            "mate_vacio", "sudario_aparecido", "vestigio_aparecido");

    public static String nombreDe(String token) {
        return NOMBRES_ESPECIALES.getOrDefault(token.toLowerCase(Locale.ROOT), token);
    }

    private final MisionesPlugin plugin;

    public RecompensaService(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    private static String[] dividir(String linea) {
        int i = linea.indexOf(':');
        if (i < 0) return new String[]{linea.trim(), ""};
        return new String[]{linea.substring(0, i).trim(), linea.substring(i + 1).trim()};
    }

    private static int parseEntero(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }

    private ItemStack parseItem(String valor) {
        String[] p = valor.split(":");
        Material material = Material.getMaterial(p[0].trim().toUpperCase(Locale.ROOT));
        if (material == null || !material.isItem()) return null;
        int cantidad = p.length > 1 ? parseEntero(p[1], 1) : 1;
        return new ItemStack(material, Math.max(1, cantidad));
    }

    /** Pide el item especial al otro plugin por reflexion. Devuelve null si no se puede. */
    public ItemStack crearEspecial(String token, int cantidad) {
        String[] ruta = ESPECIALES.get(token.toLowerCase(Locale.ROOT));
        if (ruta == null) return null;

        Plugin otro = Bukkit.getPluginManager().getPlugin(ruta[0]);
        if (otro == null || !otro.isEnabled()) return null;

        try {
            Object actual = otro;
            for (int i = 1; i < ruta.length; i++) {
                actual = actual.getClass().getMethod(ruta[i]).invoke(actual);
                if (actual == null) return null;
            }
            if (actual instanceof ItemStack item) {
                item.setAmount(Math.max(1, cantidad));
                return item;
            }
        } catch (ReflectiveOperationException | RuntimeException ex) {
            plugin.getLogger().warning("No se pudo pedir '" + token + "' a " + ruta[0]
                    + " (¿jar viejo?): " + ex.getMessage());
        }
        return null;
    }

    /** Devuelve las recompensas que NO se podrian entregar ahora (para bloquear el reclamo antes de cobrar nada). */
    public List<String> noDisponibles(List<String> recompensas) {
        List<String> faltan = new ArrayList<>();
        for (String linea : recompensas) {
            String[] p = dividir(linea);
            String tipo = p[0].toLowerCase(Locale.ROOT);
            if (tipo.equals("especial")) {
                String token = p[1].split(":")[0];
                if (crearEspecial(token, 1) == null) faltan.add(token);
            } else if (tipo.equals("item")) {
                if (parseItem(p[1]) == null) faltan.add(p[1]);
            }
        }
        return faltan;
    }

    public void entregar(Player jugador, List<String> recompensas) {
        boolean inventarioLleno = false;

        for (String linea : recompensas) {
            String[] p = dividir(linea);
            String tipo = p[0].toLowerCase(Locale.ROOT);
            String valor = p[1];

            switch (tipo) {
                case "item" -> {
                    ItemStack item = parseItem(valor);
                    if (item != null) inventarioLleno |= darItem(jugador, item);
                }
                case "especial" -> {
                    String[] v = valor.split(":");
                    int cantidad = v.length > 1 ? parseEntero(v[1], 1) : 1;
                    ItemStack item = crearEspecial(v[0], cantidad);
                    if (item != null) inventarioLleno |= darItem(jugador, item);
                }
                case "comando" -> {
                    String comando = valor.startsWith("/") ? valor.substring(1) : valor;
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comando.replace("%jugador%", jugador.getName()));
                }
                default -> plugin.getLogger().warning("Recompensa con tipo desconocido: '" + linea + "'");
            }
        }

        if (inventarioLleno) {
            jugador.sendMessage(Texto.chat("&7Tu inventario estaba lleno: algunos items cayeron al piso."));
        }
    }

    /** Devuelve true si algo no entro y cayo al piso. */
    private boolean darItem(Player jugador, ItemStack item) {
        Map<Integer, ItemStack> sobrante = jugador.getInventory().addItem(item);
        for (ItemStack resto : sobrante.values()) {
            jugador.getWorld().dropItemNaturally(jugador.getLocation(), resto);
        }
        return !sobrante.isEmpty();
    }

    /** Texto para mostrar en el menu (los comandos no se muestran: usar textoRecompensas). */
    public List<String> describir(List<String> recompensas) {
        List<String> resultado = new ArrayList<>();
        for (String linea : recompensas) {
            String[] p = dividir(linea);
            String tipo = p[0].toLowerCase(Locale.ROOT);

            if (tipo.equals("item")) {
                ItemStack item = parseItem(p[1]);
                if (item != null) {
                    resultado.add(item.getAmount() + "x " + Texto.bonito(item.getType().name()));
                }
            } else if (tipo.equals("especial")) {
                String[] v = p[1].split(":");
                String token = v[0].toLowerCase(Locale.ROOT);
                int cantidad = v.length > 1 ? parseEntero(v[1], 1) : 1;
                resultado.add(cantidad + "x " + NOMBRES_ESPECIALES.getOrDefault(token, token));
            }
        }
        return resultado;
    }
}
