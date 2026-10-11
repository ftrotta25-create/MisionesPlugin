package com.franchee.misiones;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Datos que cambian solos (que mision esta asignada a quien, progreso,
 * reclamadas). Se guardan en datos.yml; en memoria hasta el proximo guardado
 * (cada 60s si hubo cambios, y al apagar el server).
 */
public class DatosJugadores {

    private final MisionesPlugin plugin;
    private final File archivo;
    private final YamlConfiguration yaml;
    private boolean sucio = false;

    public DatosJugadores(MisionesPlugin plugin) {
        this.plugin = plugin;
        this.archivo = new File(plugin.getDataFolder(), "datos.yml");
        this.yaml = YamlConfiguration.loadConfiguration(archivo);
    }

    // ---------------- Asignadas a TODOS ----------------

    public boolean esGlobal(String id) {
        return yaml.getStringList("global").contains(id);
    }

    public void setGlobal(String id, boolean valor) {
        List<String> lista = new ArrayList<>(yaml.getStringList("global"));
        boolean estaba = lista.contains(id);
        if (valor && !estaba) lista.add(id);
        if (!valor && estaba) lista.remove(id);
        yaml.set("global", lista);
        sucio = true;
    }

    /** Devuelve el nuevo estado (true = quedo asignada). */
    public boolean alternarGlobal(String id) {
        boolean nuevo = !esGlobal(id);
        setGlobal(id, nuevo);
        return nuevo;
    }

    // ---------------- Asignadas a un jugador ----------------

    private String ruta(UUID jugador, String campo) {
        return "jugadores." + jugador + "." + campo;
    }

    public boolean esAsignadaPersonal(UUID jugador, String id) {
        return yaml.getStringList(ruta(jugador, "asignadas")).contains(id);
    }

    public void setAsignada(UUID jugador, String id, boolean valor) {
        List<String> lista = new ArrayList<>(yaml.getStringList(ruta(jugador, "asignadas")));
        boolean estaba = lista.contains(id);
        if (valor && !estaba) lista.add(id);
        if (!valor && estaba) lista.remove(id);
        yaml.set(ruta(jugador, "asignadas"), lista);
        sucio = true;
    }

    public boolean alternarAsignada(UUID jugador, String id) {
        boolean nuevo = !esAsignadaPersonal(jugador, id);
        setAsignada(jugador, id, nuevo);
        return nuevo;
    }

    /** True si la mision le corresponde al jugador (por ser global o personal). */
    public boolean tieneAsignada(UUID jugador, String id) {
        return esGlobal(id) || esAsignadaPersonal(jugador, id);
    }

    // ---------------- Progreso y reclamadas ----------------

    public int getProgreso(UUID jugador, String id) {
        return yaml.getInt(ruta(jugador, "progreso." + id), 0);
    }

    public void setProgreso(UUID jugador, String id, int valor) {
        yaml.set(ruta(jugador, "progreso." + id), valor);
        sucio = true;
    }

    public boolean estaReclamada(UUID jugador, String id) {
        return yaml.getStringList(ruta(jugador, "reclamadas")).contains(id);
    }

    public void marcarReclamada(UUID jugador, String id) {
        List<String> lista = new ArrayList<>(yaml.getStringList(ruta(jugador, "reclamadas")));
        if (!lista.contains(id)) lista.add(id);
        yaml.set(ruta(jugador, "reclamadas"), lista);
        sucio = true;
    }

    // ---------------- Limpieza al borrar una mision ----------------

    /** Saca la mision de las asignaciones, el progreso y las reclamadas de todos. */
    public void olvidarMision(String id) {
        setGlobal(id, false);

        ConfigurationSection jugadores = yaml.getConfigurationSection("jugadores");
        if (jugadores != null) {
            for (String uuid : jugadores.getKeys(false)) {
                for (String campo : new String[]{"asignadas", "reclamadas"}) {
                    String ruta = "jugadores." + uuid + "." + campo;
                    List<String> lista = new ArrayList<>(yaml.getStringList(ruta));
                    if (lista.remove(id)) {
                        yaml.set(ruta, lista);
                    }
                }
                yaml.set("jugadores." + uuid + ".progreso." + id, null);
            }
        }
        sucio = true;
    }

    // ---------------- Guardado ----------------

    public void guardar() {
        if (!sucio) return;
        try {
            yaml.save(archivo);
            sucio = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar datos.yml: " + ex.getMessage());
        }
    }
}
