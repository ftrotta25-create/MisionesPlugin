package com.franchee.misiones;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Carga y valida las misiones de missions.yml. */
public class MisionManager {

    private final MisionesPlugin plugin;
    private final Map<String, Mision> misiones = new LinkedHashMap<>();

    public MisionManager(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    public void cargar() {
        misiones.clear();

        File archivo = new File(plugin.getDataFolder(), "missions.yml");
        if (!archivo.exists()) {
            plugin.saveResource("missions.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(archivo);

        ConfigurationSection seccion = yaml.getConfigurationSection("misiones");
        if (seccion == null) {
            plugin.getLogger().warning("missions.yml no tiene la sección 'misiones'.");
            return;
        }

        for (String id : seccion.getKeys(false)) {
            ConfigurationSection m = seccion.getConfigurationSection(id);
            if (m == null) continue;
            if (id.contains(".") || id.contains(" ")) {
                plugin.getLogger().warning("Misión '" + id + "' ignorada: el ID no puede tener puntos ni espacios.");
                continue;
            }
            Mision mision = parsear(id, m);
            if (mision != null) {
                misiones.put(id, mision);
            }
        }
        plugin.getLogger().info(misiones.size() + " misión(es) cargada(s).");
    }

    private Mision parsear(String id, ConfigurationSection m) {
        Mision.TipoMision tipo;
        try {
            tipo = Mision.TipoMision.valueOf(m.getString("tipo", "BREAK").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Misión '" + id + "' ignorada: tipo inválido (usá BREAK, KILL o DELIVER).");
            return null;
        }

        Set<String> objetivos = new LinkedHashSet<>();
        for (String bruto : m.getString("objetivo", "").split(",")) {
            String o = bruto.trim().toUpperCase(Locale.ROOT);
            if (o.isEmpty()) continue;
            if (tipo == Mision.TipoMision.KILL) {
                try {
                    EntityType.valueOf(o);
                    objetivos.add(o);
                } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Misión '" + id + "': mob desconocido '" + o + "' (se ignora ese).");
                }
            } else {
                Material material = Material.getMaterial(o);
                if (material == null) {
                    plugin.getLogger().warning("Misión '" + id + "': material desconocido '" + o + "' (se ignora ese).");
                } else {
                    objetivos.add(material.name());
                }
            }
        }
        if (objetivos.isEmpty()) {
            plugin.getLogger().warning("Misión '" + id + "' ignorada: no tiene ningún objetivo válido.");
            return null;
        }

        Material icono = Material.getMaterial(m.getString("icono", "PAPER").trim().toUpperCase(Locale.ROOT));
        if (icono == null || !icono.isItem()) {
            icono = Material.PAPER;
        }

        String filtro = m.getString("filtroNombre");
        if (filtro != null && filtro.isBlank()) filtro = null;

        String textoObjetivo = m.getString("textoObjetivo");
        if (textoObjetivo != null && textoObjetivo.isBlank()) textoObjetivo = null;

        return new Mision(
                id,
                m.getString("nombre", id),
                m.getStringList("descripcion"),
                tipo,
                objetivos,
                textoObjetivo,
                Math.max(1, m.getInt("cantidad", 1)),
                icono,
                filtro,
                m.getStringList("recompensas"),
                m.getStringList("textoRecompensas"));
    }

    public Collection<Mision> todas() {
        return misiones.values();
    }

    public List<Mision> lista() {
        return new ArrayList<>(misiones.values());
    }

    public Mision get(String id) {
        return misiones.get(id);
    }
}
