# Agent Memory Hub para Codex Desktop

Esta carpeta contiene un marketplace local con **dos plugins independientes**: Agent Memory Hub Context y Agent Memory Hub Tasks. Cada plugin declara un solo servidor MCP y sus propios hooks e instrucciones. Puedes instalar y habilitar uno sin el otro. Nada se registra ni se instala automáticamente. Los paquetes usan el manifiesto de compatibilidad de Codex Desktop (`.codex-plugin/plugin.json` y `.mcp.json`) para que el runtime de hooks de la app cargue sus definiciones.

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

## Instalar en Codex Desktop

Abre este repositorio en Codex Desktop. En el directorio de plugins, selecciona el marketplace local **Agent Memory Hub** e instala por separado **Agent Memory Hub Context** y **Agent Memory Hub Tasks**. Reinicia Codex Desktop después de añadir el marketplace si todavía no aparece.

El marketplace de este repositorio está definido en `.agents/plugins/marketplace.json`. Cada plugin se puede activar o retirar por separado.

Codex exige confianza para ejecutar hooks de plugins. Los hooks de este paquete solo inyectan orientación, no hacen llamadas de red ni leen o escriben archivos; las operaciones de datos se realizan mediante el MCP correspondiente después de que el agente consulte la configuración local. No se listan proyectos al iniciar ni se elige uno automáticamente. Los hooks no crean tareas ni escriben en el Hub.

La guía oficial explica los marketplaces locales y el formato de compatibilidad de Codex Desktop: [Package your plugin](https://developers.openai.com/plugins/build/plugins).

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
