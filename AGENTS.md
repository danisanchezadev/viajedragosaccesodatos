# Instrucciones del proyecto

Actúa como asesor técnico: evalúa las propuestas con criterio, señala problemas
concretos y evita elogios automáticos o dar la razón sin fundamento.

## ESTÁNDAR DE INTERFAZ DE CONSOLA — VIAJE A DRAGOS

- Toda pantalla o menú nuevo debe respetar la estética Unicode común y reutilizar
  `org.viajedragos.consola.PanelConsola`; no construir bordes ni alinear con espacios manuales.
- El interior tiene 48 columnas, con dos columnas de margen a cada lado: 44 de texto.
  Los títulos se centran y las líneas largas se dividen automáticamente.
- Utilizar `opcion(numero, texto)` para opciones `[n]`; conservar los números y
  acciones existentes. Mostrar el prompt después de cerrar el panel.
- Usar los mismos separadores y márgenes; `columnas` distribuye los datos y
  `barraVida` representa diez segmentos, limitada entre cero y el máximo.
- Los mensajes breves, validaciones y confirmaciones pueden aparecer fuera de
  paneles cuando sea más legible. Evitar paneles para cada frase del registro.
- Los colores ANSI no cuentan como anchura y deben restablecerse antes del borde.
  Conservar su activación explícita; no añadir emojis. La referencia es la consola
  de IntelliJ con fuente monoespaciada y caracteres latinos.
- La presentación no debe cambiar negocio, combate, dados, precios ni recompensas.
  Las futuras implementaciones de acceso a datos deben conservar esta interfaz.
- Toda modificación visual requiere pruebas pertinentes de alineación, contenido,
  opciones, entradas y conservación del flujo. No debilitar comprobaciones funcionales.
- No hacer commit ni push sin autorización expresa.

## RITMO NARRATIVO DE CONSOLA

- Utilizar exclusivamente `RitmoConsola.pausar(salida)` para espaciar acontecimientos.
  Los tiempos están centralizados en `RitmoConsola.Velocidad`: instantánea 0 ms,
  rápida 250 ms, normal 700 ms y lenta 1200 ms. No repartir `Thread.sleep` por el proyecto.
- Pausar únicamente en los hitos narrativos: encuentro, anuncio de turno/orden,
  antes del resultado enemigo, después del ataque, resultado final y recompensas
  u objetos realmente encontrados. No cambiar el orden de eventos para insertar pausas.
- Cerrar completamente el panel antes de pausar. No introducir esperas en
  `PanelConsola`, entre sus líneas o caracteres, ni efecto máquina de escribir.
- Menús, configuración, validaciones, teclado, inventario, comparación y tienda
  deben responder sin pausas narrativas.
- `main` inicia una sesión en Normal; el menú principal permite cambiar la velocidad.
  Reutilizar el mismo ritmo durante la sesión, incluidos encuentros y aventuras nuevas.
  No guardar la elección en archivos ni bases de datos.
- Conservar las entradas anteriores con flujos/dados proporcionados y el constructor
  anterior de Combate en modo instantáneo. Las pruebas con ritmo deben proporcionar
  una `Espera` que registre llamadas sin dormir; no detectar JUnit ni usar ajustes del desarrollador.
  El combate automático nunca debe esperar, aunque se le proporcione un ritmo no instantáneo.
- La espera real permanece centralizada y secuencial. No añadir hilos ni temporizadores.
  Ante InterruptedException, conservar la interrupción y avisar; no repetir esperas
  mientras persista la señal. No borrar silenciosamente la interrupción.
