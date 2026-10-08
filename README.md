# MisionesPlugin

Plugin standalone para Paper **1.20.1**. Las misiones de **Peladito Botón**,
alcalde de Villa Pelado (el lobby). Vos elegís qué misiones recibe cada
jugador (o todos) y todo se maneja con menús.

## Comandos

| Comando | Quién | Qué hace |
|---|---|---|
| `/mision todos` | admin (`misiones.admin`) | Menú para asignar/quitar misiones a **TODOS** los jugadores (incluso a los que entren después) |
| `/mision <jugador>` | admin | Menú para asignar/quitar misiones a **un jugador** (aunque esté offline, si ya entró alguna vez) |
| `/mision reload` | admin | Recarga `config.yml` y `missions.yml` |
| `/misiones` | todos | Abre el menú de misiones del jugador (lo usa el NPC) |

### Menús de asignación (admin)
- Cada misión aparece con su ícono. **Click = asignar / quitar** (brilla y dice ✔ cuando está asignada).
- `Asignar TODAS` / `Quitar TODAS` (botones verde y rojo abajo).
- Paginado automático si hay más de 45 misiones.
- Si `avisarAlAsignar` está en true, el jugador recibe el aviso de Peladito por chat.

### Menú del jugador
- Muestra solo las misiones que tiene asignadas, con barra de progreso y recompensas.
- Cuando una misión está completa brilla: **click para reclamar**.
- En las de tipo `DELIVER` (entregar items) el click saca los items del inventario y da la recompensa.

## Crear a Peladito Botón con FancyNpcs

El plugin no depende de FancyNpcs: el NPC solo tiene que ejecutar `/misiones` cuando le hacen click.
Parado en Villa Pelado:

```
/npc create peladito_boton
/npc displayname peladito_boton <gold>Peladito Botón
/npc action peladito_boton any_click add player_command misiones
```
(Si algún subcomando cambió en tu versión, `/npc help` lista los actuales. También podés ponerle skin con `/npc skin`.)

## Misiones (`missions.yml`)

Totalmente customizable. Tipos: `BREAK` (romper bloques), `KILL` (matar mobs), `DELIVER` (entregar items).
Está todo explicado en comentarios dentro del archivo, y trae 5 misiones de arranque.

```yaml
misiones:
  cazador_novato:
    nombre: "&cCazador Novato"
    icono: IRON_SWORD
    descripcion: ["Los bichos de noche no dejan", "dormir a nadie."]
    tipo: KILL
    objetivo: "ZOMBIE,SKELETON,SPIDER,CREEPER"
    cantidad: 12
    recompensas:
      - "item:ARROW:24"
      - "especial:libro_florencia"
      - "comando:eco give %jugador% 100"
```

### Recompensas
- `item:MATERIAL:CANTIDAD` — item vanilla.
- `especial:NOMBRE` — items de tus otros plugins: `libro_florencia`, `cuerno_cabra`, `yelmo_testarazo`,
  `pava`, `mate_vacio`, `sudario_aparecido`, `vestigio_aparecido`.
- `comando:COMANDO` — comando de consola (`%jugador%` = nombre). Sirve para economía (Vault), items de
  ExecutableItems (`ei give %jugador% ...`), permisos, lo que quieras.

Los items `especial` se piden por **reflexión** a los otros plugins en el momento de entregar, así que este
plugin no depende de ellos para arrancar. Si falta alguno (o tiene una versión vieja del jar), la misión
**no se puede reclamar** (el jugador ve un aviso, no pierde nada) y queda un warning en consola.
Para `cuerno_cabra` y `yelmo_testarazo` hace falta el `JefeCabraPlugin.jar` nuevo (con `getItemFactory()`),
y para `sudario_aparecido`/`vestigio_aparecido` el `EventoPlugin.jar` actualizado.

## Datos

- `datos.yml`: qué misión tiene asignada cada jugador, progreso y reclamadas. Se guarda cada 60s y al apagar.
- `ignorarCreativo: true` (config.yml): en creativo no suma progreso.

## Limitaciones conocidas

- No hay anti-farmeo en `BREAK` (colocar y romper el mismo bloque suma).
- Las misiones se crean editando `missions.yml` (no hay editor dentro del juego todavía).
