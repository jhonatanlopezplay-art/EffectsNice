# Effects Nice — Fabric 1.21.1 (Documentado con IA)

Cuatro efectos controlados por el servidor y dibujados en el cliente con shaders GLSL propios: cielo, grieta, conversión de colores y esfera protectora. No requieren Veil ni un paquete de shaders.

## Instalar y usar

1. Instala **Fabric Loader 0.19.5 o posterior**, **Fabric API para Minecraft 1.21.1** y el mismo `effects-nice-1.2.0.jar` en el servidor y en todos los clientes. Minecraft requiere Java 21 o posterior.
2. Inicia una partida o el servidor. Se crea `config/effects-nice/images/` en su directorio de ejecución.
3. Coloca allí tus archivos PNG, JPG, JPEG o GIF. En un servidor dedicado solo necesitas ponerlos en el servidor; se envían automáticamente a los clientes, sin enlaces externos ni resource packs.
4. Ejecuta los comandos como operador (nivel 2) o con trucos habilitados en una partida individual.

```mcfunction
/efnice skys start
/efnice skys stop
/efnice grieta list
/efnice grieta portal.png 90 28 start
/efnice grieta stop
```

`/efnice grieta <imagen> <largo> <alto> start` acepta un largo de **10–150 grados** y un alto de **2–80 grados**. Son ángulos visuales en el cielo, no bloques. Un buen punto de partida es `90 28`. También se admite `/efnice grieta <imagen> <largo> <alto> stop`; el cierre rápido recomendado es `/efnice grieta stop`.

`list` vuelve a leer la carpeta y actualiza el autocompletado. Nombres admitidos: letras ASCII, números, punto, guion y guion bajo, sin espacios; por ejemplo `portal-01.gif`. Los límites son 8 MiB por archivo, 2048 × 2048 píxeles, 240 cuadros GIF y 32 millones de píxeles acumulados entre cuadros. Si se supera un límite o el archivo no es válido, el comando informa del motivo.

## Animación y posición

- **Cielo:** nubes carmesí oscuras en movimiento, inspiradas en la referencia. La expansión nace en una dirección del cielo y tarda 8 segundos en cubrirlo, con un frente irregular y difuminado. `stop` retira el efecto suavemente. Las nubes normales se desvanecen durante la transición.
- **Grieta:** durante 3 segundos aparece una fractura irregular desde el centro hacia ambos extremos, con pequeñas ramificaciones. Solo al llegar al largo completo empieza la apertura, de 2,5 segundos, que revela la imagen. El borde tiene un núcleo claro, rojo neón y halo suave.
- **Cierre:** primero se cierra la abertura; después se retrae la fractura. Si se detiene durante la entrada, retrocede desde el punto actual sin saltar a una grieta totalmente abierta.
- Mira hacia el lugar del cielo donde quieres el centro antes de ejecutar `start`. Si miras horizontalmente o hacia abajo, se coloca al menos 20° sobre el horizonte. La dirección permanece fija aunque gires la cámara. El efecto está en el cielo y el terreno lo oculta.
- La imagen conserva sus proporciones mediante un recorte centrado para llenar la abertura. Un GIF reproduce sus cuadros, tiempos y composición; no se reduce al primer cuadro.

Hay una grieta y un efecto de cielo por dimensión. Para cambiar de imagen, cierra la grieta y espera a que desaparezca antes de iniciar otra. Los jugadores que llegan después reciben el estado actual y la imagen. Al cambiar de dimensión no se arrastra el efecto anterior. Al reiniciar el servidor los efectos empiezan apagados.

El renderizado está diseñado para el cielo normal del Overworld. El Nether no tiene cielo normal; esta primera versión no transforma su techo. Los efectos se ocultan bajo el agua, lava, nieve en polvo, ceguera o oscuridad. Los paquetes de shaders de Iris/OptiFine y otros mods que sustituyen el cielo requieren pruebas de compatibilidad específicas; prueba inicialmente con Fabric API y este mod.

## Convert: cambiar el color de todo el mundo

```mcfunction
/efnice convert black start
/efnice convert negro start
/efnice convert verde start
/efnice convert black stop
/efnice convert stop
```

`convert <color> start/stop` transforma visualmente toda la dimensión actual: cielo, nubes, niebla, bloques, agua, lava, criaturas, partículas y los efectos de cielo/grieta activos. La entrada y salida duran 1 segundo. Con `black` o `negro`, el mundo termina en **negro puro RGB 0,0,0**, incluso en las zonas luminosas. Los otros colores conservan diferencias de luz para distinguir el relieve. Ejecutar otro color con `start` cambia el color activo; `stop` restaura el mundo independientemente del color indicado.

Se conservan los colores originales de los jugadores (incluida su armadura), los ítems caídos, los ítems que se sostienen y los marcos con sus objetos/mapas. La mano en primera persona, el inventario y la interfaz permanecen normales. La exclusión utiliza una máscara con la profundidad del mundo: los jugadores y objetos que están detrás de una pared no deben aparecer a través de ella. Los marcos completos también están excluidos.

Colores con autocompletado: `black/negro`, `white/blanco`, `green/verde`, `red/rojo`, `blue/azul`, `yellow/amarillo`, `orange/naranja`, `purple/morado/violeta`, `pink/rosa`, `cyan/cian`, `magenta`, `gray/grey/gris`, `brown/marron/cafe`.

Este efecto funciona por dimensión, incluido el Nether y el End; el límite del cielo normal descrito arriba solo corresponde a `skys` y `grieta`. Al conectarse, cada jugador recibe la conversión activa. Al cambiar de dimensión o desconectarse se deja de aplicar el estado anterior. No modifica bloques, iluminación del servidor ni ítems del inventario. Se apaga al reiniciar el servidor.

Instala **1.2.0 tanto en servidor como en clientes** y retira el JAR anterior de `mods/`.

### Correcciones de la versión 1.1.1

- Cambiar de imagen o recibir una escena sin grieta libera las transferencias, decodificaciones y texturas anteriores, incluso si la descarga no había terminado.
- El cliente solo acepta fragmentos de la imagen anunciada por el servidor; los inicios duplicados no reinician una transferencia en curso.
- Detener una grieta o solicitar otra imagen cancela la carga pendiente. La decodificación comprueba la cancelación entre cuadros.
- Se rechazan formatos distintos de PNG/JPG/GIF aunque tengan una extensión permitida, y cuadros GIF fuera de su lienzo.

Para comprobarlo en una partida, prueba negro y verde en primera y tercera persona, con un segundo jugador, un ítem caído, un ítem encantado y un mapa enmarcado. Comprueba también paredes delante de jugadores, agua/cristal, lluvia, gráficos Fabulous, cambio de resolución y cambio de dimensión. No se ha certificado compatibilidad con Iris ni otros mods que sustituyan el renderizador.

### Verificación de la versión 1.1.0

- Compilación completa con Java 21 y remapeo `named → intermediary` usando Tiny Remapper y su extensión Mixin.
- 11 pruebas JUnit correctas: fases y reversión, carga de imágenes/GIF, límites, colores y validación de la nueva carga de red.
- 4 pruebas de píxeles ejecutando el GLSL con OpenGL/EGL: negro exacto, otro color, estado apagado y transición con cobertura parcial de la máscara.
- Las cuatro clases de Minecraft modificadas para `convert` se cargan y transforman correctamente con Fabric Loader/Mixin en una comprobación sin ventana.
- No se pudo hacer una prueba visual completa dentro de Minecraft en esta sesión: el entorno no permite conectar con la pantalla. La oclusión real, las transparencias y la interacción con otros mods requieren esa prueba.

## Esfera: protección con paneles hexagonales

```mcfunction
/efnice esfera 32 start
/efnice esfera 32 stop
/efnice esfera stop
```

El radio se mide en **bloques**, admite decimales y va de **2 a 128**. Por ejemplo, radio 32 cubre 64 bloques de diámetro. El centro queda fijo en la posición desde la que ejecutas `start`: si estás sobre el suelo, la mitad superior forma una cúpula y la mitad inferior queda bajo tierra. También puede colocarse en el aire mediante `/execute positioned <x> <y> <z> run efnice esfera 32 start`.

La cubierta es violeta translúcida, con bordes luminosos y una entrada/salida de 1 segundo. La malla es mayormente hexagonal, con doce uniones pentagonales para cerrar la esfera sin una costura o deformación en los polos. Se ve desde fuera y desde dentro, y el terreno la oculta.

- **Protección real en el servidor:** absorbe las entidades de proyectil al tocar o cruzar la superficie, en ambas direcciones, sin ejecutar su impacto o explosión. Incluye flechas, tridentes, bolas de fuego, bolas de nieve, huevos, pociones, perlas, cargas de viento y cohetes. Los proyectiles absorbidos desaparecen, incluidos los tridentes. El cálculo recorre el desplazamiento completo del tick para detectar proyectiles rápidos.
- **Paso de jugadores:** al acercarse un jugador se abre solo la zona de su cuerpo y se cierra gradualmente al alejarse. Funciona caminando, agachado, nadando o volando. No coloca bloques ni empuja a los jugadores. Las aberturas son visuales: los proyectiles siguen bloqueados junto al jugador. Se muestran hasta 32 aberturas simultáneas, priorizando las más cercanas a la cámara; los espectadores no generan aberturas.
- **Estado por dimensión:** hay una esfera por dimensión, también en Nether y End. Otro `start` cambia el centro y el radio. `stop` desactiva inmediatamente la protección mientras la cubierta se desvanece; el radio indicado en `stop` no tiene que coincidir con el activo. Al entrar o cambiar de dimensión se sincroniza el estado. Se apaga al reiniciar el servidor.

La esfera protege su **superficie**: no elimina ataques que nacen y terminan dentro, daño cuerpo a cuerpo, rayos de guardianes ni explosiones de TNT. Las criaturas pueden atravesarla. Los proyectiles de otros mods que usan `ProjectileEntity` reciben la comprobación general; las mecánicas que sustituyan su movimiento o su daño necesitan pruebas de compatibilidad.

Verificación automatizada de 1.2.0: 21 pruebas JUnit, incluidas colisiones de entrada/salida, trayectorias rápidas, contactos tangenciales, límites de red, reversión y continuidad de la malla. El shader también se comprueba con OpenGL sin ventana: compilación/enlace, panel visible, abertura local, conservación del panel circundante y efecto apagado. Esto no sustituye la prueba visual en Minecraft.

Para probar en partida: activa radio 16, dispara desde fuera y desde dentro con arco, tridente, poción y cohete; atraviesa el borde caminando y volando; observa a un segundo jugador cruzarlo; cambia de dimensión y vuelve; prueba `stop`. Comprueba también agua/cristal, gráficos Fabulous y el efecto `convert` activo.

## Compilar

La plantilla usa **Gradle 9.7.1 + Loom 1.18**, que requiere **JDK 25 para ejecutar Gradle**. El mod se compila con `--release 21`, de modo que el JAR sigue siendo compatible con Java 21. En IntelliJ selecciona JDK 25 como Gradle JVM.

```sh
./gradlew build
```

El JAR instalable queda en `build/libs/effects-nice-1.2.0.jar`; no instales el `-sources.jar`. `build` ejecuta las pruebas JUnit de las fases, reversión, decodificación de imágenes, GIF, límites de carga y esfera protectora.

```sh
./gradlew runClient
./gradlew test
```

## Verificación manual en una partida

1. Con el cielo despejado, ejecuta `skys start`: observa el nacimiento central y la expansión; gira la cámara y verifica que el efecto no se pega a la pantalla.
2. Ejecuta una grieta con un PNG y con un GIF: primero debe dibujarse la fractura, después abrirse; prueba detenerla en ambas fases y una vez abierta.
3. Mira desde dentro de una casa: el techo debe tapar el efecto. Mira desde distintos ángulos y con diferentes FOV.
4. Conecta un segundo cliente durante el efecto y cambia de dimensión; comprueba el estado compartido y la limpieza de las texturas al salir.
5. Prueba un archivo inexistente y uno que supere los límites: el servidor debe responder con un error sin activar la grieta.

## Código

- `effect/EffectServer`: comandos, escenas por dimensión, carga asíncrona y distribución de archivos.
- `effect/Animation`: fases reversibles compartidas entre servidor y cliente.
- `effect/Media`: validación y decodificación limitada de PNG/JPG/GIF.
- `client/effect/ClientEffects`: recepción, reproducción GIF y liberación de texturas.
- `client/effect/SkyRenderer` y `client/mixin/SkyMixin`: integración con el pase de cielo de Minecraft.
- `effect/SphereGeometry`, `effect/SphereMesh`, `client/effect/SphereRenderer` y `mixin/Sphere*Mixin`: colisiones, malla, aperturas y protección contra proyectiles.
- `assets/effects-nice/shaders/core/`: shaders del cielo y de la grieta. Colores, ruido y grosor de bordes se pueden ajustar aquí.

Referencias de API: [WorldRenderer 1.21.1](https://maven.fabricmc.net/docs/yarn-1.21.1+build.3/net/minecraft/client/render/WorldRenderer.html), [registro de shaders de Fabric](https://maven.fabricmc.net/docs/fabric-api-0.100.1+1.21/net/fabricmc/fabric/api/client/rendering/v1/CoreShaderRegistrationCallback.html).
