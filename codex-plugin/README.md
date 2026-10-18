# Agent Memory Hub para Codex

Esta carpeta contiene un marketplace local con **dos plugins independientes**: Agent Memory Hub Context y Agent Memory Hub Tasks. Cada plugin declara un solo servidor MCP y sus propios hooks e instrucciones. Puedes instalar y habilitar uno sin el otro. Nada se registra ni se instala automáticamente.

## Requisitos

- Iniciar Agent Memory Hub localmente según la guía del repositorio.
- Tener disponibles en `https://localhost:8443` los endpoints `/api/mcp/context` y `/api/mcp/tasks`.
- Importar explícitamente `config-local/tls/localhost.crt` al llavero de inicio de sesión de macOS y marcarlo como confiable para que Codex conecte por HTTPS. No desactives la validación TLS.

Los plugins no incluyen certificados, contraseñas ni otros secretos. Ambos servidores MCP usan HTTPS de loopback.

## Validar y generar el paquete

Desde la raíz del repositorio:

```sh
python3 scripts/package-codex-plugin.py
python3 scripts/package-codex-plugin.py --output dist/agent-memory-hub-codex-marketplace.zip
```

El validador comprueba que cada plugin declare exactamente un servidor MCP, use el endpoint correcto, incluya sus hooks y tenga las rutas del manifiesto completas. El ZIP contiene el marketplace con ambos plugins.

## Instalar más adelante en Codex

Cuando decidas instalarlos, desde la raíz del repositorio registra el marketplace local:

```sh
codex plugin marketplace add ./codex-plugin
```

Después, instala **Agent Memory Hub Context**, **Agent Memory Hub Tasks**, o ambos desde el directorio de plugins de Codex. Cada uno se puede activar o retirar por separado. Registrar el marketplace cambia la configuración de Codex solo cuando ejecutes explícitamente el comando; esta implementación no lo ejecuta ni modifica tu configuración personal.

Los hooks empaquetados requieren revisión y confianza de Codex al instalarse. Ambos conjuntos de hooks aportan orientación; después de leer la referencia local, Codex consulta la configuración y los datos del proyecto vinculado mediante el MCP correspondiente. No se listan proyectos al iniciar ni se elige uno automáticamente. Los hooks no crean tareas, no escriben en el Hub y no leen ni escriben archivos del workspace por su cuenta.

La guía oficial de plugins explica el registro de un marketplace local, la instalación desde el directorio de plugins y la revisión de hooks: [Package your plugin](https://developers.openai.com/plugins/build/plugins).

## Referencia por workspace

Para vincular un worktree, Codex debe crear `.agent-memory-hub/workspace.json` con sus herramientas de archivos, después de que el usuario indique el proyecto. El formato aceptado es:

```json
{
  "version": 1,
  "serverUrl": "https://localhost:8443",
  "projectId": "<id-devuelto-por-el-hub>",
  "taskId": null
}
```

El archivo es local a cada workspace. No contiene credenciales y no se debe incluir en commits. La plataforma/Core sigue siendo la fuente de verdad de proyectos, perfiles y contenido; el perfil activo se consulta al Hub.
