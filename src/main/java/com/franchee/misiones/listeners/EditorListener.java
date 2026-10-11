package com.franchee.misiones.listeners;

import com.franchee.misiones.Mision;
import com.franchee.misiones.MisionManager;
import com.franchee.misiones.MisionesPlugin;
import com.franchee.misiones.RecompensaService;
import com.franchee.misiones.menu.EditorHolder;
import com.franchee.misiones.menu.EditorMenuManager;
import com.franchee.misiones.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Clicks de los menus del editor, y los "prompts" donde se escribe un texto por el chat. */
public class EditorListener implements Listener {

    private record Prompt(Consumer<String> alRecibir, Runnable alCancelar) {
    }

    private final MisionesPlugin plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public EditorListener(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------- Prompts por chat ----------------

    private void pedirTexto(Player admin, String instruccion, Consumer<String> alRecibir, Runnable alCancelar) {
        prompts.put(admin.getUniqueId(), new Prompt(alRecibir, alCancelar));
        // cerramos el menu en el proximo tick (no en medio del evento de click)
        Bukkit.getScheduler().runTask(plugin, () -> admin.closeInventory());
        admin.sendMessage(Texto.chat("&6[Editor] &e" + instruccion));
        admin.sendMessage(Texto.chat("&7Escribí &fcancelar &7para cancelar."));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    @SuppressWarnings("deprecation")
    public void onChat(AsyncPlayerChatEvent e) {
        Prompt prompt = prompts.remove(e.getPlayer().getUniqueId());
        if (prompt == null) return;

        e.setCancelled(true);
        Player jugador = e.getPlayer();
        String texto = e.getMessage();

        // el evento de chat es asincrono: todo lo demas se hace en el hilo principal
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (texto.trim().equalsIgnoreCase("cancelar")) {
                jugador.sendMessage(Texto.chat("&7Cancelado."));
                prompt.alCancelar().run();
            } else {
                prompt.alRecibir().accept(texto);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        prompts.remove(e.getPlayer().getUniqueId());
    }

    private void msg(Player jugador, String texto) {
        jugador.sendMessage(Texto.chat(texto));
    }

    // ---------------- Clicks ----------------

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof EditorHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof EditorHolder holder)) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player admin)) return;
        if (!admin.hasPermission("misiones.admin")) {
            admin.closeInventory();
            return;
        }
        if (e.getClickedInventory() != top) return;

        int slot = e.getSlot();
        ClickType click = e.getClick();

        switch (holder.getTipo()) {
            case LISTA -> clickLista(admin, holder, slot, click);
            case MISION -> clickMision(admin, holder, slot, click);
            case RECOMPENSAS -> clickRecompensas(admin, holder, slot, click);
            case ESPECIALES -> clickEspeciales(admin, holder, slot);
            case CONFIRMAR_BORRADO -> clickConfirmar(admin, holder, slot);
        }
    }

    private void sonido(Player admin) {
        admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    // ----- LISTA -----

    private void clickLista(Player admin, EditorHolder h, int slot, ClickType click) {
        EditorMenuManager menus = plugin.getEditor();
        MisionManager mm = plugin.getMisionManager();

        if (slot == 45 && h.getPagina() > 0) {
            h.setPagina(h.getPagina() - 1);
            menus.render(h);
            sonido(admin);
            return;
        }
        if (slot == 53 && h.getPagina() < menus.maxPaginaLista()) {
            h.setPagina(h.getPagina() + 1);
            menus.render(h);
            sonido(admin);
            return;
        }

        if (slot == 49) {
            pedirTexto(admin,
                    "Escribí el ID de la nueva misión (solo letras minúsculas, números y _ ; sin espacios).",
                    texto -> {
                        String id = texto.trim().toLowerCase(Locale.ROOT);
                        if (!id.matches("[a-z0-9_]{1,32}")) {
                            msg(admin, "&cID inválido: usá solo letras minúsculas, números y _ (máximo 32).");
                            menus.abrirLista(admin);
                            return;
                        }
                        if (!mm.crear(id)) {
                            msg(admin, "&cYa existe una misión con el ID '" + id + "'.");
                            menus.abrirLista(admin);
                            return;
                        }
                        msg(admin, "&aMisión '" + id + "' creada. Ahora editala con los botones.");
                        menus.abrirMision(admin, id);
                    },
                    () -> menus.abrirLista(admin));
            return;
        }

        if (slot >= EditorMenuManager.POR_PAGINA) return;

        List<Mision> lista = mm.lista();
        int indice = h.getPagina() * EditorMenuManager.POR_PAGINA + slot;
        if (indice >= lista.size()) return;
        Mision m = lista.get(indice);

        if (click.isRightClick()) {
            menus.abrirConfirmarBorrado(admin, m.id());
        } else if (click.isLeftClick()) {
            menus.abrirMision(admin, m.id());
        }
    }

    // ----- MISION -----

    private void clickMision(Player admin, EditorHolder h, int slot, ClickType click) {
        EditorMenuManager menus = plugin.getEditor();
        MisionManager mm = plugin.getMisionManager();
        String id = h.getMisionId();
        Mision m = mm.get(id);
        if (m == null) {
            menus.abrirLista(admin);
            return;
        }
        Runnable reabrir = () -> menus.abrirMision(admin, id);

        switch (slot) {
            case 10 -> pedirTexto(admin,
                    "Escribí el nuevo NOMBRE de la misión (podés usar colores con &, ej: &aMi Misión).",
                    texto -> {
                        mm.setNombre(id, texto.trim());
                        reabrir.run();
                    }, reabrir);

            case 11 -> {
                ItemStack mano = admin.getInventory().getItemInMainHand();
                if (mano.getType().isAir() || !mano.getType().isItem()) {
                    msg(admin, "&cSostené un item en la mano y volvé a hacer click.");
                } else {
                    mm.setIcono(id, mano.getType());
                    menus.render(h);
                    sonido(admin);
                }
            }

            case 12 -> {
                if (click.isRightClick()) {
                    List<String> lineas = new ArrayList<>(m.descripcion());
                    if (!lineas.isEmpty()) {
                        lineas.remove(lineas.size() - 1);
                        mm.setDescripcion(id, lineas);
                    }
                    menus.render(h);
                    sonido(admin);
                } else {
                    pedirTexto(admin,
                            "Escribí la línea de descripción a AGREGAR (podés usar colores con &).",
                            texto -> {
                                Mision actual = mm.get(id);
                                if (actual == null) return;
                                List<String> lineas = new ArrayList<>(actual.descripcion());
                                lineas.add(texto.trim());
                                mm.setDescripcion(id, lineas);
                                reabrir.run();
                            }, reabrir);
                }
            }

            case 13 -> {
                Mision.TipoMision[] tipos = Mision.TipoMision.values();
                int salto = click.isRightClick() ? tipos.length - 1 : 1;
                Mision.TipoMision nuevo = tipos[(m.tipo().ordinal() + salto) % tipos.length];
                mm.setTipo(id, nuevo);
                msg(admin, "&eTipo cambiado a &f" + nuevo + "&e. El objetivo se reseteó: editalo para que tenga sentido.");
                menus.render(h);
                sonido(admin);
            }

            case 14 -> {
                if (click.isRightClick()) {
                    if (m.tipo() == Mision.TipoMision.KILL) {
                        msg(admin, "&cPara mobs escribí el nombre con click izquierdo (ej: ZOMBIE).");
                        return;
                    }
                    ItemStack mano = admin.getInventory().getItemInMainHand();
                    if (mano.getType().isAir()) {
                        msg(admin, "&cSostené en la mano el item/bloque que querés agregar.");
                        return;
                    }
                    List<String> invalidos = new ArrayList<>();
                    Set<String> validos = mm.validarObjetivos(m.tipo(), mano.getType().name(), invalidos);
                    if (validos.isEmpty()) {
                        msg(admin, "&c" + mano.getType().name() + " no sirve como objetivo de tipo " + m.tipo()
                                + (m.tipo() == Mision.TipoMision.BREAK ? " (tiene que ser un bloque)." : "."));
                        return;
                    }
                    Set<String> nuevos = new LinkedHashSet<>(m.objetivos());
                    nuevos.addAll(validos);
                    mm.setObjetivo(id, String.join(",", nuevos));
                    menus.render(h);
                    sonido(admin);
                } else {
                    String ayuda = m.tipo() == Mision.TipoMision.KILL
                            ? "Escribí los MOBS separados por coma (ej: ZOMBIE,SKELETON). Reemplaza la lista actual."
                            : "Escribí los materiales separados por coma (ej: OAK_LOG,BIRCH_LOG). Reemplaza la lista actual.";
                    pedirTexto(admin, ayuda, texto -> {
                        Mision actual = mm.get(id);
                        if (actual == null) return;
                        List<String> invalidos = new ArrayList<>();
                        Set<String> validos = mm.validarObjetivos(actual.tipo(), texto, invalidos);
                        if (!invalidos.isEmpty() || validos.isEmpty()) {
                            msg(admin, "&cNo se cambió nada. No sirven: &f"
                                    + (invalidos.isEmpty() ? "(lista vacía)" : String.join(", ", invalidos)));
                        } else {
                            mm.setObjetivo(id, String.join(",", validos));
                        }
                        reabrir.run();
                    }, reabrir);
                }
            }

            case 15 -> {
                int delta = switch (click) {
                    case LEFT -> 1;
                    case RIGHT -> -1;
                    case SHIFT_LEFT -> 10;
                    case SHIFT_RIGHT -> -10;
                    default -> 0;
                };
                if (delta != 0) {
                    mm.setCantidad(id, Math.max(1, Math.min(100000, m.cantidad() + delta)));
                    menus.render(h);
                    sonido(admin);
                }
            }

            case 16 -> menus.abrirRecompensas(admin, id);

            case 20 -> {
                if (click.isRightClick()) {
                    mm.setTextoObjetivo(id, null);
                    menus.render(h);
                    sonido(admin);
                } else {
                    pedirTexto(admin,
                            "Escribí el texto que va a ver el jugador como objetivo (ej: Romper 16 troncos de cualquier tipo).",
                            texto -> {
                                mm.setTextoObjetivo(id, texto.trim());
                                reabrir.run();
                            }, reabrir);
                }
            }

            case 22 -> {
                if (m.tipo() != Mision.TipoMision.KILL) return;
                if (click.isRightClick()) {
                    mm.setFiltroNombre(id, null);
                    menus.render(h);
                    sonido(admin);
                } else {
                    pedirTexto(admin,
                            "Escribí el texto que tiene que tener el nombre del mob (ej: Jefe Cabra).",
                            texto -> {
                                mm.setFiltroNombre(id, texto.trim());
                                reabrir.run();
                            }, reabrir);
                }
            }

            case 24 -> {
                if (click.isRightClick()) {
                    mm.setTextoRecompensas(id, new ArrayList<>());
                    menus.render(h);
                    sonido(admin);
                } else {
                    pedirTexto(admin,
                            "Escribí la línea de texto de recompensa a AGREGAR (ej: 100 monedas). Podés usar colores con &.",
                            texto -> {
                                Mision actual = mm.get(id);
                                if (actual == null) return;
                                List<String> lineas = new ArrayList<>(actual.textoRecompensas());
                                lineas.add(texto.trim());
                                mm.setTextoRecompensas(id, lineas);
                                reabrir.run();
                            }, reabrir);
                }
            }

            case 36 -> menus.abrirLista(admin);

            case 40 -> menus.abrirConfirmarBorrado(admin, id);

            default -> {
            }
        }
    }

    // ----- RECOMPENSAS -----

    private void clickRecompensas(Player admin, EditorHolder h, int slot, ClickType click) {
        EditorMenuManager menus = plugin.getEditor();
        MisionManager mm = plugin.getMisionManager();
        String id = h.getMisionId();
        Mision m = mm.get(id);
        if (m == null) {
            menus.abrirLista(admin);
            return;
        }

        if (slot == 45) {
            menus.abrirMision(admin, id);
            return;
        }

        if (slot == 47) {
            if (m.recompensas().size() >= EditorMenuManager.MAX_RECOMPENSAS) {
                msg(admin, "&cMáximo " + EditorMenuManager.MAX_RECOMPENSAS + " recompensas por misión.");
                return;
            }
            ItemStack mano = admin.getInventory().getItemInMainHand();
            if (mano.getType().isAir() || !mano.getType().isItem()) {
                msg(admin, "&cSostené en la mano el item (y la cantidad) que querés dar de recompensa.");
                return;
            }
            List<String> recompensas = new ArrayList<>(m.recompensas());
            recompensas.add("item:" + mano.getType().name() + ":" + mano.getAmount());
            mm.setRecompensas(id, recompensas);
            if (mano.hasItemMeta()) {
                msg(admin, "&eOjo: solo se guardó el TIPO de item (sin nombre ni encantamientos). "
                        + "Para items custom usá 'especial' o un comando.");
            }
            menus.render(h);
            sonido(admin);
            return;
        }

        if (slot == 49) {
            menus.abrirEspeciales(admin, id);
            return;
        }

        if (slot == 51) {
            if (m.recompensas().size() >= EditorMenuManager.MAX_RECOMPENSAS) {
                msg(admin, "&cMáximo " + EditorMenuManager.MAX_RECOMPENSAS + " recompensas por misión.");
                return;
            }
            pedirTexto(admin,
                    "Escribí el COMANDO de consola (sin '/'; usá %jugador% para el nombre. Ej: eco give %jugador% 100).",
                    texto -> {
                        Mision actual = mm.get(id);
                        if (actual == null) return;
                        String comando = texto.trim();
                        if (comando.startsWith("/")) comando = comando.substring(1);
                        List<String> recompensas = new ArrayList<>(actual.recompensas());
                        recompensas.add("comando:" + comando);
                        mm.setRecompensas(id, recompensas);
                        msg(admin, "&7Tip: los comandos no se muestran solos en el menú del jugador; "
                                + "agregá un 'Texto de recompensas' en el editor de la misión.");
                        menus.abrirRecompensas(admin, id);
                    },
                    () -> menus.abrirRecompensas(admin, id));
            return;
        }

        if (slot == 53) {
            List<String> recompensas = new ArrayList<>(m.recompensas());
            if (!recompensas.isEmpty()) {
                recompensas.remove(recompensas.size() - 1);
                mm.setRecompensas(id, recompensas);
            }
            menus.render(h);
            sonido(admin);
            return;
        }

        if (slot >= EditorMenuManager.MAX_RECOMPENSAS) return;

        // Click sobre una recompensa de la lista
        List<String> recompensas = new ArrayList<>(m.recompensas());
        if (slot >= recompensas.size()) return;

        if (click == ClickType.DROP || click == ClickType.CONTROL_DROP) {
            recompensas.remove(slot);
            mm.setRecompensas(id, recompensas);
            menus.render(h);
            sonido(admin);
            return;
        }

        int delta = switch (click) {
            case LEFT -> 1;
            case RIGHT -> -1;
            case SHIFT_LEFT -> 10;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
        if (delta == 0) return;

        String actual = recompensas.get(slot);
        String nueva = ajustarCantidad(actual, delta);
        if (nueva.equals(actual)) return;
        recompensas.set(slot, nueva);
        mm.setRecompensas(id, recompensas);
        menus.render(h);
        sonido(admin);
    }

    /** Suma/resta cantidad a una recompensa "item:" o "especial:". Los comandos no se tocan. */
    private String ajustarCantidad(String linea, int delta) {
        int i = linea.indexOf(':');
        if (i < 0) return linea;

        String tipo = linea.substring(0, i).trim().toLowerCase(Locale.ROOT);
        if (!tipo.equals("item") && !tipo.equals("especial")) return linea;

        String[] partes = linea.substring(i + 1).trim().split(":");
        int cantidad = 1;
        if (partes.length > 1) {
            try {
                cantidad = Integer.parseInt(partes[1].trim());
            } catch (NumberFormatException ex) {
                cantidad = 1;
            }
        }
        cantidad = Math.max(1, Math.min(2304, cantidad + delta));
        return tipo + ":" + partes[0].trim() + ":" + cantidad;
    }

    // ----- ESPECIALES -----

    private void clickEspeciales(Player admin, EditorHolder h, int slot) {
        EditorMenuManager menus = plugin.getEditor();
        MisionManager mm = plugin.getMisionManager();
        String id = h.getMisionId();

        if (slot == 18) {
            menus.abrirRecompensas(admin, id);
            return;
        }

        int indice = slot - 10;
        if (indice < 0 || indice >= RecompensaService.TOKENS.size()) return;

        Mision m = mm.get(id);
        if (m == null) {
            menus.abrirLista(admin);
            return;
        }
        if (m.recompensas().size() >= EditorMenuManager.MAX_RECOMPENSAS) {
            msg(admin, "&cMáximo " + EditorMenuManager.MAX_RECOMPENSAS + " recompensas por misión.");
            return;
        }

        String token = RecompensaService.TOKENS.get(indice);
        List<String> recompensas = new ArrayList<>(m.recompensas());
        recompensas.add("especial:" + token);
        mm.setRecompensas(id, recompensas);
        sonido(admin);
        menus.abrirRecompensas(admin, id);
    }

    // ----- CONFIRMAR BORRADO -----

    private void clickConfirmar(Player admin, EditorHolder h, int slot) {
        EditorMenuManager menus = plugin.getEditor();
        String id = h.getMisionId();

        if (slot == 11) {
            plugin.getMisionManager().eliminar(id);
            plugin.getDatos().olvidarMision(id);
            msg(admin, "&eMisión '" + id + "' eliminada.");
            menus.abrirLista(admin);
        } else if (slot == 15) {
            menus.abrirLista(admin);
        }
    }
}
