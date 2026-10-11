package com.franchee.misiones;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Carga, valida y EDITA las misiones de missions.yml.
 * Cada cambio hecho desde el editor del juego se guarda al toque en el
 * archivo (dejando antes una copia en missions.yml.bak) y se recarga.
 */
public class MisionManager {

    private final MisionesPlugin plugin;
    private final Map<String, Mision> misiones = new LinkedHashMap<>();
    private File archivo;
    private YamlConfiguration yaml;

    public MisionManager(MisionesPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------------- Carga ----------------

    public void cargar() {
        archivo = new File(plugin.getDataFolder(), "missions.yml");
        if (!archivo.exists()) {
            plugin.saveResource("missions.yml", false);
        }
        yaml = YamlConfiguration.loadConfiguration(archivo);
        reconstruir();
        plugin.getLogger().info(misiones.size() + " misión(es) cargada(s).");
    }

    private void reconstruir() {
        misiones.clear();

        ConfigurationSection seccion = yaml.getConfigurationSection("misiones");
        if (seccion == null) return;

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
    }

    /**
     * Valida una lista de objetivos separados por coma. Devuelve los validos
     * (normalizados, en mayusculas) y deja en "invalidos" los que no sirven.
     * BREAK exige bloques, DELIVER exige items, KILL exige tipos de mob.
     */
    public Set<String> validarObjetivos(Mision.TipoMision tipo, String csv, List<String> invalidos) {
        Set<String> validos = new LinkedHashSet<>();
        if (csv == null) return validos;

        for (String bruto : csv.split(",")) {
            String o = bruto.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
            if (o.isEmpty()) continue;

            if (tipo == Mision.TipoMision.KILL) {
                try {
                    EntityType.valueOf(o);
                    validos.add(o);
                } catch (IllegalArgumentException ex) {
                    invalidos.add(o);
                }
            } else {
                Material material = Material.getMaterial(o);
                boolean sirve = material != null
                        && (tipo == Mision.TipoMision.BREAK ? material.isBlock() : material.isItem());
                if (sirve) {
                    validos.add(material.name());
                } else {
                    invalidos.add(o);
                }
            }
        }
        return validos;
    }

    private Mision parsear(String id, ConfigurationSection m) {
        Mision.TipoMision tipo;
        try {
            tipo = Mision.TipoMision.valueOf(m.getString("tipo", "BREAK").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Misión '" + id + "' ignorada: tipo inválido (usá BREAK, KILL o DELIVER).");
            return null;
        }

        List<String> invalidos = new ArrayList<>();
        Set<String> objetivos = validarObjetivos(tipo, m.getString("objetivo", ""), invalidos);
        for (String invalido : invalidos) {
            plugin.getLogger().warning("Misión '" + id + "': objetivo no válido '" + invalido + "' (se ignora ese).");
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

    // ---------------- Consulta ----------------

    public Collection<Mision> todas() {
        return misiones.values();
    }

    public List<Mision> lista() {
        return new ArrayList<>(misiones.values());
    }

    public Mision get(String id) {
        return misiones.get(id);
    }

    public boolean existe(String id) {
        return yaml.getConfigurationSection("misiones." + id) != null;
    }

    // ---------------- Edicion (usada por el editor del juego) ----------------

    private ConfigurationSection seccion(String id) {
        return yaml.getConfigurationSection("misiones." + id);
    }

    /** Crea una mision nueva con valores por defecto validos. False si el ID ya existe. */
    public boolean crear(String id) {
        if (existe(id)) return false;

        ConfigurationSection s = yaml.createSection("misiones." + id);
        s.set("nombre", "&f" + id);
        s.set("icono", "PAPER");
        s.set("descripcion", new ArrayList<String>());
        s.set("tipo", "BREAK");
        s.set("objetivo", "STONE");
        s.set("cantidad", 10);
        s.set("recompensas", new ArrayList<String>());
        guardarYReconstruir();
        return true;
    }

    public void eliminar(String id) {
        yaml.set("misiones." + id, null);
        guardarYReconstruir();
    }

    private void set(String id, String campo, Object valor) {
        ConfigurationSection s = seccion(id);
        if (s == null) return;
        s.set(campo, valor);
        guardarYReconstruir();
    }

    public void setNombre(String id, String nombre) {
        set(id, "nombre", nombre);
    }

    public void setIcono(String id, Material material) {
        set(id, "icono", material.name());
    }

    /** Cambia el tipo y resetea el objetivo a uno valido para ese tipo. */
    public void setTipo(String id, Mision.TipoMision tipo) {
        ConfigurationSection s = seccion(id);
        if (s == null) return;
        s.set("tipo", tipo.name());
        s.set("objetivo", tipo == Mision.TipoMision.KILL ? "ZOMBIE" : "STONE");
        if (tipo != Mision.TipoMision.KILL) {
            s.set("filtroNombre", null);
        }
        guardarYReconstruir();
    }

    public void setObjetivo(String id, String objetivoCsv) {
        set(id, "objetivo", objetivoCsv);
    }

    public void setCantidad(String id, int cantidad) {
        set(id, "cantidad", Math.max(1, cantidad));
    }

    /** null o vacio = sin filtro. */
    public void setFiltroNombre(String id, String filtro) {
        set(id, "filtroNombre", (filtro == null || filtro.isBlank()) ? null : filtro);
    }

    /** null o vacio = texto automatico. */
    public void setTextoObjetivo(String id, String texto) {
        set(id, "textoObjetivo", (texto == null || texto.isBlank()) ? null : texto);
    }

    public void setDescripcion(String id, List<String> lineas) {
        set(id, "descripcion", new ArrayList<>(lineas));
    }

    public void setRecompensas(String id, List<String> lineas) {
        set(id, "recompensas", new ArrayList<>(lineas));
    }

    /** Lista vacia = se muestra el texto automatico. */
    public void setTextoRecompensas(String id, List<String> lineas) {
        set(id, "textoRecompensas", lineas.isEmpty() ? null : new ArrayList<>(lineas));
    }

    private void guardarYReconstruir() {
        try {
            if (archivo.exists()) {
                Files.copy(archivo.toPath(),
                        new File(plugin.getDataFolder(), "missions.yml.bak").toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            yaml.save(archivo);
        } catch (IOException ex) {
            plugin.getLogger().warning("No se pudo guardar missions.yml: " + ex.getMessage());
        }
        reconstruir();
    }
}
