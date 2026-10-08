package com.franchee.misiones.listeners;

import com.franchee.misiones.DatosJugadores;
import com.franchee.misiones.Mision;
import com.franchee.misiones.MisionesPlugin;
import com.franchee.misiones.ProgresoService;
import com.franchee.misiones.menu.MenuHolder;
import com.franchee.misiones.menu.MenuManager;
import com.franchee.misiones.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.List;
import java.util.UUID;

/** Maneja todos los clicks dentro de los menus del plugin. */
public class MenuListener implements Listener {

    private final MisionesPlugin plugin;

    public MenuListener(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof MenuHolder holder)) return;

        e.setCancelled(true); // nada se puede sacar ni meter en nuestros menus

        if (!(e.getWhoClicked() instanceof Player jugador)) return;
        if (e.getClickedInventory() != top) return;

        MenuManager menus = plugin.getMenus();
        int slot = e.getSlot();

        // Navegacion
        if (slot == 45 && holder.getPagina() > 0) {
            holder.setPagina(holder.getPagina() - 1);
            menus.render(holder);
            click(jugador);
            return;
        }
        if (slot == 53 && holder.getPagina() < menus.maxPagina(holder)) {
            holder.setPagina(holder.getPagina() + 1);
            menus.render(holder);
            click(jugador);
            return;
        }

        boolean menuAdmin = holder.getTipo() != MenuHolder.Tipo.JUGADOR;
        if (menuAdmin && !jugador.hasPermission("misiones.admin")) return;

        if (menuAdmin && slot == 47) {
            asignarTodas(jugador, holder, true);
            return;
        }
        if (menuAdmin && slot == 51) {
            asignarTodas(jugador, holder, false);
            return;
        }

        if (slot >= MenuManager.POR_PAGINA) return;

        List<Mision> lista = menus.listaPara(holder);
        int indice = holder.getPagina() * MenuManager.POR_PAGINA + slot;
        if (indice >= lista.size()) return;
        Mision mision = lista.get(indice);

        switch (holder.getTipo()) {
            case ASIGNAR_GLOBAL -> alternarGlobal(jugador, holder, mision);
            case ASIGNAR_JUGADOR -> alternarPersonal(jugador, holder, mision);
            case JUGADOR -> reclamar(jugador, holder, mision);
        }
    }

    private void click(Player jugador) {
        jugador.playSound(jugador.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
    }

    // ---------------- Asignacion (admin) ----------------

    private void alternarGlobal(Player admin, MenuHolder holder, Mision m) {
        boolean ahora = plugin.getDatos().alternarGlobal(m.id());
        admin.sendMessage(Texto.chat(ahora
                ? "&aMisión &f" + m.nombre() + " &aasignada a &lTODOS&a."
                : "&eMisión &f" + m.nombre() + " &equitada de &lTODOS&e."));

        if (ahora && plugin.getConfig().getBoolean("avisarAlAsignar", true)) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                plugin.enviar(p, "misionNuevaTodos", "%mision%", m.nombre(), "%jugador%", p.getName());
            }
        }
        plugin.getMenus().render(holder);
        click(admin);
    }

    private void alternarPersonal(Player admin, MenuHolder holder, Mision m) {
        UUID objetivo = holder.getObjetivo();
        boolean ahora = plugin.getDatos().alternarAsignada(objetivo, m.id());
        admin.sendMessage(Texto.chat(ahora
                ? "&aMisión &f" + m.nombre() + " &aasignada a &f" + holder.getNombreObjetivo() + "&a."
                : "&eMisión &f" + m.nombre() + " &equitada a &f" + holder.getNombreObjetivo() + "&e."));

        if (ahora && plugin.getConfig().getBoolean("avisarAlAsignar", true)) {
            Player online = Bukkit.getPlayer(objetivo);
            if (online != null) {
                plugin.enviar(online, "misionNueva", "%mision%", m.nombre(), "%jugador%", online.getName());
            }
        }
        plugin.getMenus().render(holder);
        click(admin);
    }

    private void asignarTodas(Player admin, MenuHolder holder, boolean valor) {
        DatosJugadores datos = plugin.getDatos();
        boolean global = holder.getTipo() == MenuHolder.Tipo.ASIGNAR_GLOBAL;

        for (Mision m : plugin.getMisionManager().todas()) {
            if (global) {
                datos.setGlobal(m.id(), valor);
            } else {
                datos.setAsignada(holder.getObjetivo(), m.id(), valor);
            }
        }

        String destino = global ? "TODOS" : holder.getNombreObjetivo();
        admin.sendMessage(Texto.chat(valor
                ? "&aAsignaste &lTODAS&a las misiones a &f" + destino + "&a."
                : "&eQuitaste &lTODAS&e las misiones a &f" + destino + "&e."));

        if (valor && plugin.getConfig().getBoolean("avisarAlAsignar", true)) {
            if (global) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    plugin.enviar(p, "misionesNuevasTodos", "%jugador%", p.getName());
                }
            } else {
                Player online = Bukkit.getPlayer(holder.getObjetivo());
                if (online != null) plugin.enviar(online, "misionesNuevas", "%jugador%", online.getName());
            }
        }
        plugin.getMenus().render(holder);
        click(admin);
    }

    // ---------------- Reclamo (jugador) ----------------

    private void reclamar(Player jugador, MenuHolder holder, Mision m) {
        ProgresoService.Resultado resultado = plugin.getProgreso().reclamar(jugador, m);

        switch (resultado) {
            case OK -> {
                plugin.enviar(jugador, "recompensaReclamada", "%jugador%", jugador.getName(), "%mision%", m.nombre());
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }
            case INCOMPLETA -> {
                plugin.enviar(jugador, "incompleta");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            case RECOMPENSA_NO_DISPONIBLE -> {
                plugin.enviar(jugador, "noDisponible");
                jugador.playSound(jugador.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            }
            default -> {
                // YA_RECLAMADA / NO_ASIGNADA: no hace falta avisar nada
            }
        }
        plugin.getMenus().render(holder);
    }
}
