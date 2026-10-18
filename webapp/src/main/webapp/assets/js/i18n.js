export async function loadI18nMessages(contextPath, module, optionalModule) {
    const modulePath = optionalModule ? `${encodeURIComponent(module)}/${encodeURIComponent(optionalModule)}` : encodeURIComponent(module);
    const response = await fetch(`${contextPath}/i18n/${modulePath}`, { headers: { Accept: 'application/json' } });

    if (!response.ok) {
        throw new Error(`Could not load translations (${response.status}).`);
    }

    return response.json();
}
