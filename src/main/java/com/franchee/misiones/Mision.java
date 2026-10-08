package com.franchee.misiones;

import org.bukkit.Material;

import java.util.List;
import java.util.Set;

/** Una mision definida en missions.yml. */
public record Mision(
        String id,
        String nombre,
        List<String> descripcion,
        TipoMision tipo,
        Set<String> objetivos,
        String textoObjetivo,
        int cantidad,
        Material icono,
        String filtroNombre,
        List<String> recompensas,
        List<String> textoRecompensas) {

    public enum TipoMision { BREAK, KILL, DELIVER }
}
