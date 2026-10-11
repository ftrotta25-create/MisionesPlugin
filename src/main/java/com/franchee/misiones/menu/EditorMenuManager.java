package com.franchee.misiones.menu;

import com.franchee.misiones.Mision;
import com.franchee.misiones.MisionesPlugin;
import com.franchee.misiones.RecompensaService;
import com.franchee.misiones.util.ItemsUtil;
import com.franchee.misiones.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Menus del editor de misiones (/mision editor).
 *
 *  LISTA        54 slots: misiones (45 por pagina) + boton de crear
 *  MISION       45 slots: un boton por cada campo de la mision
 *  RECOMPENSAS  54 slots: lista de recompensas + botones de agregar
 *  ESPECIALES   27 slots: items de tus otros plugins
 *  CONFIRMAR    27 slots: confirmar el borrado
 */
public class EditorMenuManager {

    public static final int POR_PAGINA = 45;
    public static final int MAX_RECOMPENSAS = 45;

    private static final Map<String, Material> ICONOS_ESPECIALES = Map.of(
            "libro_florencia", Material.WRITTEN_BOOK,
            "cuerno_cabra", Material.GOAT_HORN,
            "yelmo_testarazo", Material.NETHERITE_HELMET,
            "pava", Material.IRON_HOE,
            "mate_vacio", Material.BOWL,
            "sudario_aparecido", Material.PHANTOM_MEMBRANE,
            "vestigio_aparecido", Material.NETHER_STAR
    );

    private final MisionesPlugin plugin;

    public EditorMenuManager(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------- Apertura ----------------

    public void abrirLista(Player admin) {
        abrir(admin, new EditorHolder(EditorHolder.Tipo.LISTA, null), 54, "&5Editor de misiones");
    }

    public void abrirMision(Player admin, String id) {
        if (plugin.getMisionManager().get(id) == null) {
            abrirLista(admin);
            return;
        }
        abrir(admin, new EditorHolder(EditorHolder.Tipo.MISION, id), 45, "&5Editando: &f" + id);
    }

    public void abrirRecompensas(Player admin, String id) {
        if (plugin.getMisionManager().get(id) == null) {
            abrirLista(admin);
            return;
        }
        abrir(admin, new EditorHolder(EditorHolder.Tipo.RECOMPENSAS, id), 54, "&5Recompensas: &f" + id);
    }

    public void abrirEspeciales(Player admin, String id) {
        abrir(admin, new EditorHolder(EditorHolder.Tipo.ESPECIALES, id), 27, "&5Recompensa especial");
    }

    public void abrirConfirmarBorrado(Player admin, String id) {
        if (plugin.getMisionManager().get(id) == null) {
            abrirLista(admin);
            return;
        }
        abrir(admin, new EditorHolder(EditorHolder.Tipo.CONFIRMAR_BORRADO, id), 27, "&cEliminar: &f" + id);
    }

    private void abrir(Player admin, EditorHolder holder, int tamano, String titulo) {
        Inventory inventario = Bukkit.createInventory(holder, tamano, Texto.chat(titulo));
        holder.setInventario(inventario);
        render(holder);
        admin.openInventory(inventario);
    }

    public int maxPaginaLista() {
        int total = plugin.getMisionManager().todas().size();
        return Math.max(0, (total - 1) / POR_PAGINA);
    }

    // ---------------- Dibujo ----------------

    public void render(EditorHolder h) {
        switch (h.getTipo()) {
            case LISTA -> renderLista(h);
            case MISION -> renderMision(h);
            case RECOMPENSAS -> renderRecompensas(h);
            case ESPECIALES -> renderEspeciales(h);
            case CONFIRMAR_BORRADO -> renderConfirmar(h);
        }
    }

    private void rellenar(Inventory inv, int desde, int hasta) {
        ItemStack relleno = ItemsUtil.boton(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int slot = desde; slot < hasta; slot++) {
            inv.setItem(slot, relleno);
        }
    }

    // ----- LISTA -----

    private void renderLista(EditorHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();

        List<Mision> lista = plugin.getMisionManager().lista();
        int max = maxPaginaLista();
        if (h.getPagina() > max) h.setPagina(max);
        int inicio = h.getPagina() * POR_PAGINA;

        for (int i = 0; i < POR_PAGINA && inicio + i < lista.size(); i++) {
            inv.setItem(i, itemLista(lista.get(inicio + i)));
        }
        if (lista.isEmpty()) {
            inv.setItem(22, ItemsUtil.boton(Material.BARRIER, "&cNo hay misiones",
                    "&7Creá la primera con el botón verde de abajo."));
        }

        rellenar(inv, 45, 54);
        if (h.getPagina() > 0) {
            inv.setItem(45, ItemsUtil.boton(Material.ARROW, "&e« Página anterior"));
        }
        if (h.getPagina() < max) {
            inv.setItem(53, ItemsUtil.boton(Material.ARROW, "&ePágina siguiente »"));
        }
        inv.setItem(49, ItemsUtil.boton(Material.EMERALD, "&a+ Crear nueva misión",
                "&7Te pide un ID por el chat y después",
                "&7la editás con los botones del menú."));
    }

    private ItemStack itemLista(Mision m) {
        List<String> lore = new ArrayList<>();
        lore.add("&8ID: " + m.id());
        lore.add("&7Tipo: &f" + m.tipo());
        lore.add("&7Cantidad: &f" + m.cantidad());
        lore.add("&7Recompensas: &f" + m.recompensas().size());
        lore.add("");
        lore.add("&eClick izquierdo: &fEditar");
        lore.add("&cClick derecho: &fEliminar");
        return ItemsUtil.construir(m.icono(), m.nombre(), lore, false);
    }

    // ----- MISION -----

    private void renderMision(EditorHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();

        Mision m = plugin.getMisionManager().get(h.getMisionId());
        if (m == null) return;

        rellenar(inv, 0, 45);

        inv.setItem(4, ItemsUtil.construir(m.icono(), m.nombre(), List.of("&8ID: " + m.id()), false));

        inv.setItem(10, ItemsUtil.boton(Material.NAME_TAG, "&eNombre",
                "&7Actual: &r" + m.nombre(),
                "",
                "&7Click: cambiarlo (lo escribís en el chat)",
                "&7Podés usar colores con &"));

        inv.setItem(11, ItemsUtil.boton(m.icono(), "&eÍcono",
                "&7Actual: &f" + Texto.bonito(m.icono().name()),
                "",
                "&7Sostené un item en la mano",
                "&7y hacé click acá para usarlo."));

        List<String> loreDesc = new ArrayList<>();
        if (m.descripcion().isEmpty()) {
            loreDesc.add("&8(sin descripción)");
        } else {
            for (String linea : m.descripcion()) loreDesc.add("&7" + linea);
        }
        loreDesc.add("");
        loreDesc.add("&eClick izquierdo: &fagregar una línea");
        loreDesc.add("&eClick derecho: &fquitar la última línea");
        inv.setItem(12, ItemsUtil.construir(Material.WRITABLE_BOOK, "&eDescripción", loreDesc, false));

        inv.setItem(13, ItemsUtil.boton(Material.COMPARATOR, "&eTipo: &f" + m.tipo(),
                "&7BREAK: romper bloques",
                "&7KILL: matar mobs",
                "&7DELIVER: entregar items al alcalde",
                "",
                "&eClick izquierdo: &fsiguiente tipo",
                "&eClick derecho: &ftipo anterior",
                "&cOjo: al cambiar el tipo se resetea el objetivo."));

        List<String> loreObj = new ArrayList<>();
        int mostrados = 0;
        for (String o : m.objetivos()) {
            if (mostrados++ >= 6) {
                loreObj.add("&7... y " + (m.objetivos().size() - 6) + " más");
                break;
            }
            loreObj.add("&7• &f" + Texto.bonito(o));
        }
        loreObj.add("");
        loreObj.add("&eClick izquierdo: &fescribir la lista (separada por coma)");
        if (m.tipo() == Mision.TipoMision.KILL) {
            loreObj.add("&7Para mobs, escribí el nombre (ej: ZOMBIE).");
        } else {
            loreObj.add("&eClick derecho: &fagregar el item que tenés en la mano");
        }
        inv.setItem(14, ItemsUtil.construir(Material.COMPASS, "&eObjetivo", loreObj, false));

        inv.setItem(15, ItemsUtil.boton(Material.SUNFLOWER, "&eCantidad: &f" + m.cantidad(),
                "&eClick izquierdo: &f+1",
                "&eClick derecho: &f-1",
                "&eShift + izquierdo: &f+10",
                "&eShift + derecho: &f-10"));

        List<String> loreRec = new ArrayList<>();
        List<String> descritas = plugin.getRecompensas().describir(m.recompensas());
        for (String r : descritas) loreRec.add("&7• &f" + r);
        long comandos = m.recompensas().stream().filter(l -> l.trim().toLowerCase(Locale.ROOT).startsWith("comando")).count();
        if (comandos > 0) loreRec.add("&7• &f" + comandos + " comando(s)");
        if (loreRec.isEmpty()) loreRec.add("&8(sin recompensas)");
        loreRec.add("");
        loreRec.add("&eClick: &feditar las recompensas");
        inv.setItem(16, ItemsUtil.construir(Material.CHEST, "&eRecompensas &7(" + m.recompensas().size() + ")", loreRec, false));

        inv.setItem(20, ItemsUtil.boton(Material.OAK_SIGN, "&eTexto del objetivo",
                m.textoObjetivo() != null ? "&7Actual: &f" + m.textoObjetivo() : "&8(automático)",
                "",
                "&eClick izquierdo: &fescribir un texto propio",
                "&eClick derecho: &fvolver al automático"));

        if (m.tipo() == Mision.TipoMision.KILL) {
            inv.setItem(22, ItemsUtil.boton(Material.PAPER, "&eFiltro de nombre",
                    m.filtroNombre() != null ? "&7Actual: &f" + m.filtroNombre() : "&8(ninguno)",
                    "&7Solo cuentan los mobs que tengan este",
                    "&7texto en el nombre (ej: Jefe Cabra).",
                    "",
                    "&eClick izquierdo: &fescribirlo",
                    "&eClick derecho: &fquitarlo"));
        } else {
            inv.setItem(22, ItemsUtil.boton(Material.GRAY_STAINED_GLASS_PANE, "&8Filtro de nombre",
                    "&8Solo disponible para el tipo KILL."));
        }

        List<String> loreTexto = new ArrayList<>();
        if (m.textoRecompensas().isEmpty()) {
            loreTexto.add("&8(automático)");
        } else {
            for (String l : m.textoRecompensas()) loreTexto.add("&7• &f" + l);
        }
        loreTexto.add("");
        loreTexto.add("&7Lo que ve el jugador como recompensa.");
        loreTexto.add("&7Útil si usás comandos (esos no se muestran solos).");
        loreTexto.add("");
        loreTexto.add("&eClick izquierdo: &fagregar una línea");
        loreTexto.add("&eClick derecho: &fborrar todo y volver al automático");
        inv.setItem(24, ItemsUtil.construir(Material.BOOK, "&eTexto de recompensas", loreTexto, false));

        inv.setItem(36, ItemsUtil.boton(Material.ARROW, "&e« Volver a la lista"));
        inv.setItem(40, ItemsUtil.boton(Material.TNT, "&cEliminar misión",
                "&7Te va a pedir confirmación."));
    }

    // ----- RECOMPENSAS -----

    private void renderRecompensas(EditorHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();

        Mision m = plugin.getMisionManager().get(h.getMisionId());
        if (m == null) return;

        List<String> recompensas = m.recompensas();
        for (int i = 0; i < MAX_RECOMPENSAS && i < recompensas.size(); i++) {
            inv.setItem(i, itemRecompensa(recompensas.get(i)));
        }
        if (recompensas.isEmpty()) {
            inv.setItem(22, ItemsUtil.boton(Material.BARRIER, "&cSin recompensas",
                    "&7Agregá con los botones de abajo."));
        }

        rellenar(inv, 45, 54);
        inv.setItem(45, ItemsUtil.boton(Material.ARROW, "&e« Volver a la misión"));
        inv.setItem(47, ItemsUtil.boton(Material.HOPPER, "&aAgregar el item de mi mano",
                "&7Sostené el item (y la cantidad)",
                "&7que querés dar y hacé click.",
                "&7Solo se guarda el tipo de item,",
                "&7sin nombre ni encantamientos."));
        inv.setItem(49, ItemsUtil.boton(Material.NETHER_STAR, "&dAgregar item especial",
                "&7Items de tus otros plugins:",
                "&7Libro de Florencia, Pava, etc."));
        inv.setItem(51, ItemsUtil.boton(Material.COMMAND_BLOCK, "&6Agregar comando",
                "&7Un comando de consola. Sirve para",
                "&7economía, ExecutableItems, etc.",
                "&7Usá %jugador% para el nombre."));
        inv.setItem(53, ItemsUtil.boton(Material.RED_DYE, "&cQuitar la última recompensa"));
    }

    private ItemStack itemRecompensa(String linea) {
        String[] p = dividir(linea);
        String tipo = p[0].toLowerCase(Locale.ROOT);
        String[] v = p[1].split(":");
        int cantidad = v.length > 1 ? entero(v[1], 1) : 1;

        if (tipo.equals("item")) {
            Material material = Material.getMaterial(v[0].trim().toUpperCase(Locale.ROOT));
            if (material == null || !material.isItem()) {
                return ItemsUtil.boton(Material.BARRIER, "&cItem inválido: " + p[1],
                        "&cQ (soltar): &fquitar");
            }
            ItemStack item = ItemsUtil.construir(material, "&f" + cantidad + "x " + Texto.bonito(material.name()),
                    List.of("&7Item vanilla",
                            "",
                            "&eClick izquierdo: &f+1   &eDerecho: &f-1",
                            "&eShift + izquierdo: &f+10   &eShift + derecho: &f-10",
                            "&cQ (soltar): &fquitar"), false);
            item.setAmount(Math.max(1, Math.min(cantidad, 64)));
            return item;
        }

        if (tipo.equals("especial")) {
            String token = v[0].trim().toLowerCase(Locale.ROOT);
            Material icono = ICONOS_ESPECIALES.getOrDefault(token, Material.NETHER_STAR);
            return ItemsUtil.construir(icono, "&d" + RecompensaService.nombreDe(token) + " &7x" + cantidad,
                    List.of("&7Item especial (" + token + ")",
                            "",
                            "&eClick izquierdo: &f+1   &eDerecho: &f-1",
                            "&eShift + izquierdo: &f+10   &eShift + derecho: &f-10",
                            "&cQ (soltar): &fquitar"), false);
        }

        if (tipo.equals("comando")) {
            return ItemsUtil.boton(Material.COMMAND_BLOCK, "&6Comando",
                    "&f" + p[1],
                    "",
                    "&cQ (soltar): &fquitar");
        }

        return ItemsUtil.boton(Material.BARRIER, "&cRecompensa desconocida", "&7" + linea, "", "&cQ (soltar): &fquitar");
    }

    // ----- ESPECIALES -----

    private void renderEspeciales(EditorHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();
        rellenar(inv, 0, 27);

        for (int i = 0; i < RecompensaService.TOKENS.size(); i++) {
            String token = RecompensaService.TOKENS.get(i);
            boolean disponible = plugin.getRecompensas().crearEspecial(token, 1) != null;
            Material icono = ICONOS_ESPECIALES.getOrDefault(token, Material.NETHER_STAR);

            inv.setItem(10 + i, ItemsUtil.boton(icono, "&d" + RecompensaService.nombreDe(token),
                    disponible ? "&a✔ Disponible ahora" : "&c✘ No disponible ahora",
                    disponible ? "" : "&7(el plugin no está instalado o tiene un jar viejo)",
                    "",
                    "&eClick: &fagregar como recompensa"));
        }

        inv.setItem(18, ItemsUtil.boton(Material.ARROW, "&e« Volver a las recompensas"));
    }

    // ----- CONFIRMAR BORRADO -----

    private void renderConfirmar(EditorHolder h) {
        Inventory inv = h.getInventario();
        inv.clear();
        rellenar(inv, 0, 27);

        Mision m = plugin.getMisionManager().get(h.getMisionId());
        if (m == null) return;

        inv.setItem(11, ItemsUtil.boton(Material.LIME_CONCRETE, "&a✔ Sí, eliminar",
                "&7Se borra la misión y el progreso",
                "&7que tenga cada jugador."));
        inv.setItem(13, ItemsUtil.construir(m.icono(), m.nombre(), List.of("&8ID: " + m.id()), false));
        inv.setItem(15, ItemsUtil.boton(Material.RED_CONCRETE, "&c✘ Cancelar"));
    }

    // ---------------- Helpers ----------------

    private static String[] dividir(String linea) {
        int i = linea.indexOf(':');
        if (i < 0) return new String[]{linea.trim(), ""};
        return new String[]{linea.substring(0, i).trim(), linea.substring(i + 1).trim()};
    }

    private static int entero(String texto, int porDefecto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException ex) {
            return porDefecto;
        }
    }
}
