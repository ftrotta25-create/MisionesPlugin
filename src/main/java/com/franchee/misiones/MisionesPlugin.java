package com.franchee.misiones;

import com.franchee.misiones.listeners.EditorListener;
import com.franchee.misiones.listeners.MenuListener;
import com.franchee.misiones.listeners.ProgresoListener;
import com.franchee.misiones.menu.EditorMenuManager;
import com.franchee.misiones.menu.MenuManager;
import com.franchee.misiones.util.Texto;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MisionesPlugin extends JavaPlugin {

    private MisionManager misionManager;
    private DatosJugadores datos;
    private RecompensaService recompensas;
    private ProgresoService progreso;
    private MenuManager menus;
    private EditorMenuManager editor;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        misionManager = new MisionManager(this);
        misionManager.cargar();
        datos = new DatosJugadores(this);
        recompensas = new RecompensaService(this);
        progreso = new ProgresoService(this);
        menus = new MenuManager(this);
        editor = new EditorMenuManager(this);

        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new ProgresoListener(this), this);
        getServer().getPluginManager().registerEvents(new EditorListener(this), this);

        // Guarda datos.yml cada 60s si hubo cambios (y al apagar).
        getServer().getScheduler().runTaskTimer(this, () -> datos.guardar(), 1200L, 1200L);

        getLogger().info("MisionesPlugin habilitado.");
    }

    @Override
    public void onDisable() {
        if (datos != null) datos.guardar();
        getLogger().info("MisionesPlugin deshabilitado.");
    }

    public MisionManager getMisionManager() {
        return misionManager;
    }

    public DatosJugadores getDatos() {
        return datos;
    }

    public RecompensaService getRecompensas() {
        return recompensas;
    }

    public ProgresoService getProgreso() {
        return progreso;
    }

    public MenuManager getMenus() {
        return menus;
    }

    public EditorMenuManager getEditor() {
        return editor;
    }

    // ---------------- Mensajes ----------------

    public String msg(String clave, String... reemplazos) {
        String texto = getConfig().getString("mensajes." + clave, "");
        for (int i = 0; i + 1 < reemplazos.length; i += 2) {
            texto = texto.replace(reemplazos[i], reemplazos[i + 1]);
        }
        return texto;
    }

    public void enviar(Player jugador, String clave, String... reemplazos) {
        String texto = msg(clave, reemplazos);
        if (!texto.isBlank()) {
            jugador.sendMessage(Texto.chat(texto));
        }
    }

    // ---------------- Comandos ----------------

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String nombre = command.getName().toLowerCase(Locale.ROOT);

        if (nombre.equals("misiones")) {
            if (!(sender instanceof Player jugador)) {
                sender.sendMessage("Este comando solo lo puede usar un jugador.");
                return true;
            }
            enviar(jugador, "bienvenida", "%jugador%", jugador.getName());
            menus.abrirJugador(jugador);
            return true;
        }

        if (nombre.equals("mision")) {
            if (!sender.hasPermission("misiones.admin")) {
                sender.sendMessage("No tenés permiso para usar ese comando.");
                return true;
            }
            if (args.length < 1) {
                sender.sendMessage("Uso: /mision <todos|editor|nombreDelJugador|reload>");
                return true;
            }

            String arg = args[0];

            if (arg.equalsIgnoreCase("reload")) {
                reloadConfig();
                misionManager.cargar();
                sender.sendMessage("Config y misiones recargadas (" + misionManager.todas().size() + " misión/es).");
                return true;
            }

            if (!(sender instanceof Player admin)) {
                sender.sendMessage("Los menús solo se pueden abrir dentro del juego.");
                return true;
            }

            if (arg.equalsIgnoreCase("todos")) {
                menus.abrirAsignarGlobal(admin);
                return true;
            }

            if (arg.equalsIgnoreCase("editor")) {
                editor.abrirLista(admin);
                return true;
            }

            OfflinePlayer objetivo = buscarJugador(arg);
            if (objetivo == null) {
                sender.sendMessage("No conozco a ningún jugador llamado '" + arg
                        + "' (tiene que haber entrado al server alguna vez).");
                return true;
            }
            menus.abrirAsignarJugador(admin, objetivo);
            return true;
        }

        return false;
    }

    private OfflinePlayer buscarJugador(String nombre) {
        Player online = Bukkit.getPlayerExact(nombre);
        if (online != null) return online;

        for (OfflinePlayer jugador : Bukkit.getOfflinePlayers()) {
            if (jugador.getName() != null && jugador.getName().equalsIgnoreCase(nombre)) {
                return jugador;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("mision") || !sender.hasPermission("misiones.admin")) {
            return List.of();
        }
        if (args.length != 1) return List.of();

        List<String> opciones = new ArrayList<>(List.of("todos", "editor", "reload"));
        for (Player p : Bukkit.getOnlinePlayers()) opciones.add(p.getName());

        String escrito = args[0].toLowerCase(Locale.ROOT);
        List<String> resultado = new ArrayList<>();
        for (String o : opciones) {
            if (o.toLowerCase(Locale.ROOT).startsWith(escrito)) resultado.add(o);
        }
        return resultado;
    }
}
