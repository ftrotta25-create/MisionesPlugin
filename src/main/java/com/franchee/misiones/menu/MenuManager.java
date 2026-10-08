package com.franchee.misiones.menu;

import com.franchee.misiones.DatosJugadores;
import com.franchee.misiones.Mision;
import com.franchee.misiones.MisionesPlugin;
import com.franchee.misiones.ProgresoService;
import com.franchee.misiones.util.Texto;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Arma y dibuja los menus. Layout (54 slots):
 *   0-44  misiones (45 por pagina)
 *   45    pagina anterior        47 asignar todas (solo admin)
 *   49    info                   51 quitar todas (solo admin)
 *   53    pagina siguiente
 */
public class MenuManager {

    public static final int POR_PAGINA = 45;

    private final MisionesPlugin plugin;

    public MenuManager(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------- Apertura ----------------

    public void abrirJugador(Player jugador) {
        MenuHolder holder = new MenuHolder(MenuHolder.Tipo.JUGADOR, jugador.getUniqueId(), jugador.getName());
        abrir(jugador, holder, plugin.getConfig().getString("menu.tituloJugador", "&6Misiones"));
    }

    public void abrirAsignarGlobal(Player admin) {
        MenuHolder holder = new MenuHolder(MenuHolder.Tipo.ASIGNAR_GLOBAL, null, "TODOS");
        abrir(admin, holder, plugin.getConfig().getString("menu.tituloAsignarTodos", "&5Misiones para TODOS"));
    }

    public void abrirAsignarJugador(Player admin, OfflinePlayer objetivo) {
        String nombre = objetivo.getName() != null ? objetivo.getName() : "?";
        MenuHolder holder = new MenuHolder(MenuHolder.Tipo.ASIGNAR_JUGADOR, objetivo.getUniqueId(), nombre);
        String titulo = plugin.getConfig().getString("menu.tituloAsignarJugador", "&5Misiones para &d%jugador%")
                .replace("%jugador%", nombre);
        abrir(admin, holder, titulo);
    }

    private void abrir(Player viewer, MenuHolder holder, String titulo) {
        Inventory inventario = Bukkit.createInventory(holder, 54, Texto.chat(titulo));
        holder.setInventario(inventario);
        render(holder);
        viewer.openInventory(inventario);
    }

    // ---------------- Datos del menu ----------------

    public List<Mision> listaPara(MenuHolder h) {
        List<Mision> todas = plugin.getMisionManager().lista();
        if (h.getTipo() != MenuHolder.Tipo.JUGADOR) return todas;

        DatosJugadores datos = plugin.getDatos();
        List<Mision> asignadas = new ArrayList<>();
        for (Mision m : todas) {
            if (datos.tieneAsignada(h.getObjetivo(), m.id())) asignadas.add(m);
        }
        return asignadas;
    }

    public int maxPagina(MenuHolder h) {
        int total = listaPara(h).size();
        return Math.max(0, (total - 1) / POR_PAGINA);
    }

    // ---------------- Dibujo ----------------

    public void render(MenuHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();

        List<Mision> lista = listaPara(h);
        int maxPagina = maxPagina(h);
        if (h.getPagina() > maxPagina) h.setPagina(maxPagina);
        int inicio = h.getPagina() * POR_PAGINA;

        for (int i = 0; i < POR_PAGINA && inicio + i < lista.size(); i++) {
            Mision m = lista.get(inicio + i);
            inv.setItem(i, h.getTipo() == MenuHolder.Tipo.JUGADOR ? itemJugador(h, m) : itemAsignar(h, m));
        }

        if (lista.isEmpty()) {
            inv.setItem(22, itemVacio(h));
        }

        ItemStack relleno = boton(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = 45; slot < 54; slot++) {
            inv.setItem(slot, relleno);
        }
        if (h.getPagina() > 0) {
            inv.setItem(45, boton(Material.ARROW, "&e« Página anterior"));
        }
        if (h.getPagina() < maxPagina) {
            inv.setItem(53, boton(Material.ARROW, "&ePágina siguiente »"));
        }
        inv.setItem(49, itemInfo(h, lista, maxPagina));

        if (h.getTipo() != MenuHolder.Tipo.JUGADOR) {
            inv.setItem(47, boton(Material.LIME_DYE, "&aAsignar TODAS las misiones"));
            inv.setItem(51, boton(Material.RED_DYE, "&cQuitar TODAS las misiones"));
        }
    }

    private ItemStack itemVacio(MenuHolder h) {
        if (h.getTipo() == MenuHolder.Tipo.JUGADOR) {
            return boton(Material.BARRIER, "&cNo tenés misiones por ahora",
                    "&7Volvé más tarde, el alcalde te va a avisar.");
        }
        return boton(Material.BARRIER, "&cNo hay misiones cargadas",
                "&7Agregalas en missions.yml y usá /mision reload.");
    }

    private ItemStack itemInfo(MenuHolder h, List<Mision> lista, int maxPagina) {
        String nombre;
        List<String> lore = new ArrayList<>();

        if (h.getTipo() == MenuHolder.Tipo.ASIGNAR_GLOBAL) {
            nombre = "&dAsignando a: &fTODOS los jugadores";
            lore.add("&7Las misiones asignadas acá le aparecen");
            lore.add("&7a todos, incluso a quienes entren después.");
        } else if (h.getTipo() == MenuHolder.Tipo.ASIGNAR_JUGADOR) {
            nombre = "&dAsignando a: &f" + h.getNombreObjetivo();
            lore.add("&7Solo para este jugador.");
        } else {
            nombre = "&6Tus misiones";
            int reclamadas = 0;
            for (Mision m : lista) {
                if (plugin.getDatos().estaReclamada(h.getObjetivo(), m.id())) reclamadas++;
            }
            lore.add("&7Reclamadas: &f" + reclamadas + "/" + lista.size());
        }
        lore.add("&8Página " + (h.getPagina() + 1) + "/" + (maxPagina + 1));

        return boton(Material.BOOK, nombre, lore.toArray(new String[0]));
    }

    private ItemStack itemAsignar(MenuHolder h, Mision m) {
        DatosJugadores datos = plugin.getDatos();
        boolean global = datos.esGlobal(m.id());
        boolean asignada = h.getTipo() == MenuHolder.Tipo.ASIGNAR_GLOBAL
                ? global
                : datos.esAsignadaPersonal(h.getObjetivo(), m.id());

        List<String> lore = new ArrayList<>();
        for (String linea : m.descripcion()) lore.add("&7" + linea);
        lore.add("");
        lore.add("&eObjetivo: &f" + resumen(m));
        lore.add("&6Recompensas:");
        for (String r : textoRecompensas(m)) lore.add("&7  • &f" + r);
        lore.add("");
        if (asignada) {
            lore.add("&a✔ ASIGNADA");
            lore.add("&7Click para quitarla");
        } else {
            lore.add("&c✘ No asignada");
            lore.add("&7Click para asignarla");
        }
        if (h.getTipo() == MenuHolder.Tipo.ASIGNAR_JUGADOR && global) {
            lore.add("&d(Ya está asignada a TODOS)");
        }

        return construir(m.icono(), m.nombre(), lore, asignada);
    }

    private ItemStack itemJugador(MenuHolder h, Mision m) {
        DatosJugadores datos = plugin.getDatos();
        ProgresoService prog = plugin.getProgreso();
        UUID u = h.getObjetivo();
        Player jugador = Bukkit.getPlayer(u);

        boolean reclamada = datos.estaReclamada(u, m.id());
        int progreso = jugador != null
                ? prog.progreso(jugador, m)
                : Math.min(datos.getProgreso(u, m.id()), m.cantidad());
        boolean completa = progreso >= m.cantidad();
        boolean entrega = m.tipo() == Mision.TipoMision.DELIVER;

        List<String> lore = new ArrayList<>();
        for (String linea : m.descripcion()) lore.add("&7" + linea);
        lore.add("");
        lore.add("&eObjetivo: &f" + resumen(m));
        lore.add("&eProgreso: &f" + progreso + "/" + m.cantidad() + " " + barra(progreso, m.cantidad()));
        lore.add("");
        lore.add("&6Recompensas:");
        for (String r : textoRecompensas(m)) lore.add("&7  • &f" + r);
        lore.add("");

        boolean brillo = false;
        if (reclamada) {
            lore.add("&a✔ Recompensa reclamada");
        } else if (completa) {
            lore.add(entrega ? "&a¡Tenés todo! &eClick para ENTREGAR y reclamar"
                             : "&a¡Completada! &eClick para reclamar tu recompensa");
            brillo = true;
        } else {
            lore.add(entrega ? "&7Juntá los items y volvé a hacer click"
                             : "&7Seguí así, todavía no la completaste");
        }

        String nombre = reclamada ? "&a✔ " + m.nombre() : m.nombre();
        return construir(m.icono(), nombre, lore, brillo);
    }

    // ---------------- Helpers ----------------

    private String resumen(Mision m) {
        if (m.textoObjetivo() != null) return m.textoObjetivo();

        List<String> nombres = new ArrayList<>();
        for (String o : m.objetivos()) nombres.add(Texto.bonito(o));
        String lista;
        if (nombres.size() > 3) {
            lista = String.join(", ", nombres.subList(0, 3)) + " y " + (nombres.size() - 3) + " más";
        } else {
            lista = String.join(", ", nombres);
        }

        return switch (m.tipo()) {
            case BREAK -> "Romper " + m.cantidad() + " (" + lista + ")";
            case KILL -> "Matar " + m.cantidad() + " (" + lista + ")"
                    + (m.filtroNombre() != null ? " llamados '" + m.filtroNombre() + "'" : "");
            case DELIVER -> "Entregar " + m.cantidad() + " (" + lista + ")";
        };
    }

    private List<String> textoRecompensas(Mision m) {
        if (!m.textoRecompensas().isEmpty()) return m.textoRecompensas();
        return plugin.getRecompensas().describir(m.recompensas());
    }

    private String barra(int actual, int total) {
        int llenos = (int) Math.round(10.0 * Math.min(actual, total) / total);
        return "&a" + "█".repeat(llenos) + "&7" + "░".repeat(10 - llenos);
    }

    private ItemStack boton(Material material, String nombre, String... lore) {
        List<String> lineas = new ArrayList<>(List.of(lore));
        return construir(material, nombre, lineas, false);
    }

    private ItemStack construir(Material material, String nombre, List<String> lore, boolean brillo) {
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
}
