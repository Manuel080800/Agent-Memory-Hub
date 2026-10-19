import { loadI18nMessages } from '../i18n.js';

const appElement = document.querySelector('#hub-app');
const contextPath = appElement.dataset.contextPath;
const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
const userId = appElement.dataset.userId;
const platformRole = appElement.dataset.platformRole;
const translations = await loadI18nMessages(contextPath, 'ui');
const resourcePathByType = { SKILL: 'skills', WORKFLOW: 'workflows', MEMORY: 'memories', TASK: 'tasks' };

const messages = {
    requestFailed: translations['ui.request.failed'],
    requestError: translations['ui.request.error'],
    active: translations['ui.active'],
    inactive: translations['ui.inactive'],
    enabled: translations['ui.enabled'],
    disabled: translations['ui.disabled'],
    connectionUnconfigured: translations['ui.connection.unconfigured'],
    disable: translations['ui.disable'],
    enable: translations['ui.enable'],
    context: translations['ui.context'],
    tasks: translations['ui.tasks'],
    roleReader: translations['ui.role.reader'],
    roleEditor: translations['ui.role.editor'],
    title: translations['ui.title'],
    resourceName: translations['ui.resource.name'],
    content: translations['ui.content'],
    objective: translations['ui.objective'],
    instructions: translations['ui.instructions'],
    description: translations['ui.description'],
    mode: translations['ui.mode'],
    modeAlways: translations['ui.mode.always'],
    modeContextual: translations['ui.mode.contextual'],
    tagsPlaceholder: translations['ui.tags.placeholder'],
    descriptionEmpty: translations['ui.description.empty'],
    projectCreated: translations['ui.project.created'],
    projectCreateTitle: translations['ui.project.create'],
    projectUpdated: translations['ui.project.updated'],
    projectEditTitle: translations['ui.project.edit.title'],
    projectDeleteConfirm: translations['ui.project.delete.confirm'],
    projectDeleted: translations['ui.project.deleted'],
    projectNamePrompt: translations['ui.project.name.prompt'],
    projectDescriptionPrompt: translations['ui.project.description.prompt'],
    profileCreated: translations['ui.profile.created'],
    profileCreateTitle: translations['ui.profile.create'],
    profileUpdated: translations['ui.profile.updated'],
    profileEditTitle: translations['ui.profile.edit.title'],
    profileDeleted: translations['ui.profile.deleted'],
    profileDeleteConfirm: translations['ui.profile.delete.confirm'],
    profileNamePrompt: translations['ui.profile.name.prompt'],
    profileDescriptionPrompt: translations['ui.profile.description.prompt'],
    settingsSaved: translations['ui.settings.saved'],
    settingsResetConfirm: translations['ui.settings.reset.confirm'],
    settingsReset: translations['ui.settings.reset'],
    connectionNamePrompt: translations['ui.connection.name.prompt'],
    connectionDeleteConfirm: translations['ui.connection.delete.confirm'],
    confirmationTitle: translations['ui.dialog.confirmation.title'],
    requiredFields: translations['ui.dialog.required'],
    confirm: translations['ui.confirm'],
    create: translations['ui.create'],
    delete: translations['ui.delete'],
    save: translations['ui.save'],
    memberAdded: translations['ui.member.added'],
    memberAddTitle: translations['ui.member.add'],
    memberUserIdPrompt: translations['ui.member.userId'],
    memberRoleUpdated: translations['ui.member.roleUpdated'],
    memberRemoveConfirm: translations['ui.member.remove.confirm'],
    roleOwner: translations['ui.role.owner'],
    memberRemoved: translations['ui.member.removed'],
    connectionContextName: translations['ui.connection.contextName'],
    connectionTasksName: translations['ui.connection.tasksName'],
    connectionUpdated: translations['ui.connection.updated'],
    resourceCreated: translations['ui.resource.created'],
    resourceCreateTitles: {
        SKILL: translations['ui.resource.create.skill'],
        WORKFLOW: translations['ui.resource.create.workflow'],
        MEMORY: translations['ui.resource.create.memory'],
        TASK: translations['ui.resource.create.task']
    },
    resourceEditTitle: translations['ui.resource.edit.title'],
    resourceNamePrompt: translations['ui.resource.name.prompt'],
    resourceUpdated: translations['ui.resource.updated'],
    resourceDeleteConfirm: translations['ui.resource.delete.confirm'],
    resourceDeleted: translations['ui.resource.deleted'],
    proposalContentPrompt: translations['ui.proposal.content.prompt'],
    proposalEditTitle: translations['ui.proposal.edit.title'],
    proposalReasonPrompt: translations['ui.proposal.reason.prompt'],
    proposalTagsPrompt: translations['ui.tags'],
    proposalNewTitle: translations['ui.proposal.new.title'],
    proposalNew: translations['ui.proposal.new'],
    proposalCreated: translations['ui.proposal.created'],
    pending: translations['ui.proposal.pending'],
    processed: translations['ui.proposal.processed'],
    resultMemory: translations['ui.proposal.resultMemory'],
    proposalUpdated: translations['ui.proposal.updated'],
    proposalDeleteConfirm: translations['ui.proposal.delete.confirm'],
    proposalDeleted: translations['ui.proposal.deleted'],
    blockAdded: translations['ui.block.added'],
    blockAdd: translations['ui.block.add'],
    blockNew: translations['ui.block.new'],
    blockContentPrompt: translations['ui.block.content.prompt'],
    blockUpdated: translations['ui.block.updated'],
    blockDeleteConfirm: translations['ui.block.delete.confirm'],
    blockDeleted: translations['ui.block.deleted'],
    memoryTitlePrompt: translations['ui.memory.title.prompt'],
    proposalApproveTitle: translations['ui.proposal.approve.title'],
    memoryFromTask: translations['ui.memory.fromTask'],
    proposalApproved: translations['ui.proposal.approved'],
    resourceSkill: translations['ui.resource.skill'],
    resourceWorkflow: translations['ui.resource.workflow'],
    resourceMemory: translations['ui.resource.memory'],
    resourceTask: translations['ui.resource.task'],
    serverCommunicationError: translations['ui.server.communicationError']
};
const { createApp } = window.Vue;

createApp({
    data() {
        return {
            projectList: [],
            profileList: [],
            resourceList: [],
            proposalList: [],
            memberList: [],
            connectionList: [],
            settingsRecord: null,
            settingsForm: {},
            blockList: [],
            blockTaskId: null,
            messages,
            platformRole,
            currentProjectRole: null,
            selectedProject: null,
            activeWorkspaceSection: 'overview',
            activeProfile: null,
            activeProfileId: '',
            activeResourceType: 'SKILL',
            proposalTaskId: null,
            resourceTypeList: ['SKILL', 'WORKFLOW', 'MEMORY', 'TASK'],
            errorMessage: '',
            noticeMessage: '',
            dialogMode: '',
            dialogTitle: '',
            dialogMessage: '',
            dialogFields: [],
            dialogValues: {},
            dialogSubmitLabel: '',
            dialogDanger: false,
            dialogError: '',
            dialogResolve: null
        };
    },
    mounted() {
        document.getElementById('hub-dialog').addEventListener('hidden.bs.modal', this.cancelDialog);
        this.loadProjects();
    },
    computed: {
        canEditProject() {
            return this.currentProjectRole === 'OWNER' || this.currentProjectRole === 'EDITOR';
        },
        enabledConnectionCount() {
            return this.connectionList.filter(connection => connection.enabled).length;
        },
        isResourceSection() {
            return this.resourceTypeList.includes(this.activeWorkspaceSection);
        }
    },
    methods: {
        showFormDialog(title, fields, submitLabel = messages.save) {
            return new Promise(resolve => {
                this.dialogMode = 'form';
                this.dialogTitle = title;
                this.dialogMessage = '';
                this.dialogFields = fields;
                this.dialogValues = Object.fromEntries(fields.map(field => [field.key, field.value ?? '']));
                this.dialogSubmitLabel = submitLabel;
                this.dialogDanger = false;
                this.dialogError = '';
                this.dialogResolve = resolve;
                this.$nextTick(() => document.getElementById('hub-dialog-trigger').click());
            });
        },
        showConfirmDialog(message, danger = false) {
            return new Promise(resolve => {
                this.dialogMode = 'confirm';
                this.dialogTitle = messages.confirmationTitle;
                this.dialogMessage = message;
                this.dialogFields = [];
                this.dialogValues = {};
                this.dialogSubmitLabel = danger ? messages.delete : messages.confirm;
                this.dialogDanger = danger;
                this.dialogError = '';
                this.dialogResolve = resolve;
                this.$nextTick(() => document.getElementById('hub-dialog-trigger').click());
            });
        },
        completeDialog(result) {
            const resolve = this.dialogResolve;
            this.dialogResolve = null;
            if (resolve) resolve(result);
        },
        cancelDialog() {
            this.completeDialog(null);
        },
        confirmDialog() {
            this.completeDialog(true);
        },
        submitDialog() {
            if (this.dialogFields.some(field => field.required && !String(this.dialogValues[field.key] || '').trim())) {
                this.dialogError = messages.requiredFields;
                return;
            }
            this.completeDialog({ ...this.dialogValues });
            document.getElementById('hub-dialog-close').click();
        },
        async request(path, options = {}) {
            const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
            if (options.method && options.method !== 'GET') headers['X-CSRF-Token'] = csrfToken;
            const response = await fetch(contextPath + path, { ...options, headers });
            if (!response.ok) {
                const result = await response.json().catch(() => ({}));
                throw new Error(result.message || result.error?.message || messages.requestFailed.replace('{0}', response.status));
            }
            return response.status === 204 ? null : response.json();
        },
        async loadProjects() {
            try {
                const page = await this.request('/projects');
                this.projectList = page.items || [];
            } catch (error) { this.showError(error); }
        },
        async openProject(projectId) {
            try {
                this.clearMessages();
                const [project, profilePage, settingsRecord, memberPage, connectionPage] = await Promise.all([
                    this.request('/projects/' + projectId),
                    this.request('/projects/' + projectId + '/profiles'),
                    this.request('/projects/' + projectId + '/settings'),
                    this.request('/projects/' + projectId + '/members'),
                    this.request('/projects/' + projectId + '/mcp-connections')
                ]);
                this.selectedProject = project;
                this.activeWorkspaceSection = 'overview';
                this.profileList = profilePage.items || [];
                this.settingsRecord = settingsRecord;
                this.settingsForm = { ...settingsRecord };
                this.memberList = memberPage.items || [];
                this.currentProjectRole = (this.memberList.find(member => member.userId === userId) || {}).projectRole || null;
                this.connectionList = connectionPage.items || [];
                this.activeProfileId = this.profileList.some(profile => profile.id === project.activeProfileId) ? project.activeProfileId : '';
                this.activeProfile = this.profileList.find(profile => profile.id === this.activeProfileId) || null;
                this.resourceList = [];
                this.proposalList = [];
                this.proposalTaskId = null;
                this.blockList = [];
                this.blockTaskId = null;
            } catch (error) { this.showError(error); }
        },
        async createProject() {
            const values = await this.showFormDialog(messages.projectCreateTitle, [
                { key: 'name', label: messages.projectNamePrompt, value: '', required: true },
                { key: 'description', label: messages.projectDescriptionPrompt, value: '' },
                { key: 'enabled', label: messages.enabled, value: true, type: 'checkbox' }
            ], messages.create);
            if (!values) return;
            try {
                await this.request('/projects', { method: 'POST', body: JSON.stringify({ name: values.name.trim(), description: values.description, enabled: values.enabled }) });
                this.noticeMessage = messages.projectCreated;
                await this.loadProjects();
            } catch (error) { this.showError(error); }
        },
        async editProject() {
            const values = await this.showFormDialog(messages.projectEditTitle, [
                { key: 'name', label: messages.projectNamePrompt, value: this.selectedProject.name, required: true },
                { key: 'description', label: messages.projectDescriptionPrompt, value: this.selectedProject.description || '', required: false },
                { key: 'enabled', label: messages.enabled, value: this.selectedProject.enabled, type: 'checkbox' }
            ]);
            if (!values) return;
            try {
                const projectPath = '/projects/' + this.selectedProject.id;
                this.selectedProject = await this.request(projectPath, {
                    method: 'PUT',
                    headers: { 'If-Match': String(this.selectedProject.version) },
                    body: JSON.stringify({ name: values.name.trim(), description: values.description, enabled: values.enabled })
                });
                await this.loadProjects();
                this.noticeMessage = messages.projectUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProject() {
            if (!await this.showConfirmDialog(messages.projectDeleteConfirm, true)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id, { method: 'DELETE' });
                this.projectList = this.projectList.filter(project => project.id !== this.selectedProject.id);
                this.selectedProject = null;
                this.profileList = [];
                this.resourceList = [];
                this.noticeMessage = messages.projectDeleted;
            } catch (error) { this.showError(error); }
        },
        async createProfile() {
            const values = await this.showFormDialog(messages.profileCreateTitle, [
                { key: 'name', label: messages.profileNamePrompt, value: '', required: true },
                { key: 'description', label: messages.profileDescriptionPrompt, value: '' },
                { key: 'enabled', label: messages.enabled, value: true, type: 'checkbox' }
            ], messages.create);
            if (!values || !this.selectedProject) return;
            try {
                const profile = await this.request('/projects/' + this.selectedProject.id + '/profiles', { method: 'POST', body: JSON.stringify({ name: values.name.trim(), description: values.description, enabled: values.enabled }) });
                this.profileList.push(profile);
                this.noticeMessage = messages.profileCreated;
            } catch (error) { this.showError(error); }
        },
        async editProfile() {
            const profile = this.activeProfile;
            const values = await this.showFormDialog(messages.profileEditTitle, [
                { key: 'name', label: messages.profileNamePrompt, value: profile.name, required: true },
                { key: 'description', label: messages.profileDescriptionPrompt, value: profile.description || '', required: false },
                { key: 'enabled', label: messages.enabled, value: profile.enabled, type: 'checkbox' }
            ]);
            if (!values) return;
            try {
                const profilePath = '/projects/' + this.selectedProject.id + '/profiles/' + profile.id;
                this.activeProfile = await this.request(profilePath, {
                    method: 'PUT',
                    headers: { 'If-Match': String(profile.version) },
                    body: JSON.stringify({ name: values.name.trim(), description: values.description, enabled: values.enabled })
                });
                Object.assign(profile, this.activeProfile);
                this.noticeMessage = messages.profileUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProfile() {
            if (!await this.showConfirmDialog(messages.profileDeleteConfirm, true)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId, { method: 'DELETE' });
                this.profileList = this.profileList.filter(profile => profile.id !== this.activeProfileId);
                this.activeProfile = null;
                this.activeProfileId = '';
                this.resourceList = [];
                this.activeWorkspaceSection = 'profiles';
                this.noticeMessage = messages.profileDeleted;
            } catch (error) { this.showError(error); }
        },
        async selectActiveProfile() {
            if (!this.activeProfileId) return;
            try {
                this.activeProfile = await this.request('/projects/' + this.selectedProject.id + '/active-profile/' + this.activeProfileId, { method: 'PUT' });
                this.selectedProject.activeProfileId = this.activeProfileId;
                this.resourceList = [];
                if (this.isResourceSection) await this.loadResources(this.activeWorkspaceSection);
            } catch (error) { this.showError(error); }
        },
        async selectWorkspaceSection(section) {
            this.clearMessages();
            this.activeWorkspaceSection = section;
            if (this.resourceTypeList.includes(section)) {
                this.resourceList = [];
                this.activeResourceType = section;
                await this.loadResources(section);
            }
        },
        async saveSettings() {
            try {
                this.settingsRecord = await this.request('/projects/' + this.selectedProject.id + '/settings', { method: 'PUT', headers: { 'If-Match': String(this.settingsRecord.version) }, body: JSON.stringify(this.settingsPayload()) });
                this.settingsForm = { ...this.settingsRecord };
                this.noticeMessage = messages.settingsSaved;
            } catch (error) { this.showError(error); }
        },
        async resetSettings() {
            if (!await this.showConfirmDialog(messages.settingsResetConfirm)) return;
            try {
                this.settingsRecord = await this.request('/projects/' + this.selectedProject.id + '/settings/reset', { method: 'POST' });
                this.settingsForm = { ...this.settingsRecord };
                this.noticeMessage = messages.settingsReset;
            } catch (error) { this.showError(error); }
        },
        settingsPayload() {
            return {
                contextRetrievalEnabled: this.settingsForm.contextRetrievalEnabled,
                compactRecoveryEnabled: this.settingsForm.compactRecoveryEnabled,
                contextualSelectionEnabled: this.settingsForm.contextualSelectionEnabled,
                taskCreationMode: this.settingsForm.taskCreationMode,
                automaticSummaryEnabled: this.settingsForm.automaticSummaryEnabled,
                selectedTaskRetrievalEnabled: this.settingsForm.selectedTaskRetrievalEnabled,
                contextBudgetTokens: this.settingsForm.contextBudgetTokens,
                permanentQuotaPercent: this.settingsForm.permanentQuotaPercent,
                preToolUseContextEnabled: this.settingsForm.preToolUseContextEnabled
            };
        },
        async addMember() {
            const values = await this.showFormDialog(messages.memberAddTitle, [
                { key: 'userId', label: messages.memberUserIdPrompt, value: '', required: true },
                { key: 'projectRole', label: messages.roleEditor, value: 'READER', options: [{ value: 'READER', label: messages.roleReader }, { value: 'EDITOR', label: messages.roleEditor }] }
            ], messages.memberAddTitle);
            if (!values) return;
            try {
                const member = await this.request('/projects/' + this.selectedProject.id + '/members', { method: 'POST', body: JSON.stringify({ userId: values.userId.trim(), projectRole: values.projectRole }) });
                this.memberList.push(member);
                this.noticeMessage = messages.memberAdded;
            } catch (error) { this.showError(error); }
        },
        async updateMember(member) {
            try {
                const path = '/projects/' + this.selectedProject.id + '/members/' + member.userId;
                const updatedMember = await this.request(path, {
                    method: 'PUT',
                    body: JSON.stringify({ userId: member.userId, projectRole: member.projectRole })
                });
                Object.assign(member, updatedMember);
                this.noticeMessage = messages.memberRoleUpdated;
            } catch (error) { this.showError(error); }
        },
        async removeMember(member) {
            if (!await this.showConfirmDialog(messages.memberRemoveConfirm.replace('{0}', member.fullName), true)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/members/' + member.userId, { method: 'DELETE' });
                this.memberList = this.memberList.filter(item => item.userId !== member.userId);
                this.noticeMessage = messages.memberRemoved;
            } catch (error) { this.showError(error); }
        },
        connectionForType(connectionType) {
            return this.connectionList.find(connection => connection.connectionType === connectionType) || null;
        },
        async toggleConnection(connectionType) {
            const connection = this.connectionForType(connectionType);
            const path = '/projects/' + this.selectedProject.id + '/mcp-connections';
            var connectionName;
            if (!connection) {
                const defaultName = connectionType === 'CONTEXT' ? messages.connectionContextName : messages.connectionTasksName;
                const values = await this.showFormDialog(messages.connectionNamePrompt, [
                    { key: 'name', label: messages.connectionNamePrompt, value: defaultName, required: true }
                ], messages.create);
                if (!values) return;
                connectionName = values.name.trim();
            }
            try {
                if (connection) {
                    await this.request(path + '/' + connection.id, { method: 'PUT', headers: { 'If-Match': String(connection.version) }, body: JSON.stringify({ enabled: !connection.enabled }) });
                } else {
                    await this.request(path, { method: 'POST', body: JSON.stringify({ name: connectionName, connectionType, enabled: true }) });
                }
                const page = await this.request(path);
                this.connectionList = page.items || [];
                this.noticeMessage = messages.connectionUpdated;
            } catch (error) { this.showError(error); }
        },
        async renameConnection(connection) {
            const values = await this.showFormDialog(messages.connectionNamePrompt, [
                { key: 'name', label: messages.connectionNamePrompt, value: connection.name, required: true }
            ]);
            if (!values) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/mcp-connections/' + connection.id;
                await this.request(path, { method: 'PUT', headers: { 'If-Match': String(connection.version) }, body: JSON.stringify({ name: values.name.trim() }) });
                const page = await this.request('/projects/' + this.selectedProject.id + '/mcp-connections');
                this.connectionList = page.items || [];
                this.noticeMessage = messages.connectionUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteConnection(connection) {
            if (!await this.showConfirmDialog(messages.connectionDeleteConfirm, true)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/mcp-connections/' + connection.id, { method: 'DELETE' });
                this.connectionList = this.connectionList.filter(item => item.id !== connection.id);
                this.noticeMessage = messages.connectionUpdated;
            } catch (error) { this.showError(error); }
        },
        async loadResources(resourceType) {
            if (!this.activeProfileId) return;
            try {
                this.activeResourceType = resourceType;
                this.proposalTaskId = null;
                this.blockTaskId = null;
                const page = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/' + resourcePathByType[resourceType]);
                this.resourceList = page.items || [];
            } catch (error) { this.showError(error); }
        },
        async createResource() {
            const resourceType = this.activeResourceType;
            const fields = [{ key: 'name', label: resourceType === 'MEMORY' || resourceType === 'TASK' ? messages.title : messages.resourceName, value: '', required: true }];
            if (resourceType === 'SKILL' || resourceType === 'WORKFLOW') {
                fields.push({ key: 'description', label: messages.description, value: '', multiline: true });
                fields.push({ key: 'instructions', label: messages.instructions, value: '', multiline: true, required: true });
                fields.push({ key: 'enabled', label: messages.enabled, value: true, type: 'checkbox' });
                if (resourceType === 'SKILL') {
                    fields.push({ key: 'tags', label: messages.proposalTagsPrompt, value: '' });
                    fields.push({ key: 'applicationMode', label: messages.mode, value: 'CONTEXTUAL', options: [{ value: 'CONTEXTUAL', label: messages.modeContextual }, { value: 'ALWAYS', label: messages.modeAlways }] });
                }
            } else if (resourceType === 'MEMORY') {
                fields.push({ key: 'content', label: messages.content, value: '', multiline: true, required: true });
                fields.push({ key: 'tags', label: messages.proposalTagsPrompt, value: '' });
                fields.push({ key: 'applicationMode', label: messages.mode, value: 'CONTEXTUAL', options: [{ value: 'CONTEXTUAL', label: messages.modeContextual }, { value: 'ALWAYS', label: messages.modeAlways }] });
                fields.push({ key: 'enabled', label: messages.enabled, value: true, type: 'checkbox' });
            } else {
                fields.push({ key: 'description', label: messages.objective, value: '', multiline: true, required: true });
                fields.push({ key: 'summary', label: messages.summary, value: '', multiline: true });
            }
            const values = await this.showFormDialog(messages.resourceCreateTitles[resourceType], fields, messages.create);
            if (!values) return;
            var body;
            if (resourceType === 'SKILL') {
                body = { name: values.name.trim(), description: values.description, instructions: values.instructions, enabled: values.enabled, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean), applicationMode: values.applicationMode };
            } else if (resourceType === 'WORKFLOW') {
                body = { name: values.name.trim(), description: values.description, instructions: values.instructions, enabled: values.enabled };
            } else if (resourceType === 'MEMORY') {
                body = { title: values.name.trim(), content: values.content, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean), applicationMode: values.applicationMode, enabled: values.enabled };
            } else {
                body = { title: values.name.trim(), description: values.description, summary: values.summary };
            }
            try {
                const resource = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/resources/' + resourceType, { method: 'POST', headers: { 'Idempotency-Key': window.crypto.randomUUID() }, body: JSON.stringify(body) });
                this.resourceList.unshift(resource);
                this.noticeMessage = messages.resourceCreated.replace('{0}', this.resourceTypeLabel(resourceType));
            } catch (error) { this.showError(error); }
        },
        async editResource(resource) {
            const resourceType = this.activeResourceType;
            const currentName = resource.name || resource.title;
            const fields = [{ key: 'name', label: messages.resourceNamePrompt, value: currentName, required: true }];
            if (resourceType === 'SKILL' || resourceType === 'WORKFLOW') {
                fields.push({ key: 'description', label: messages.description, value: resource.description || '', multiline: true });
                fields.push({ key: 'instructions', label: messages.instructions, value: resource.instructions || '', multiline: true, required: true });
                fields.push({ key: 'enabled', label: messages.enabled, value: resource.enabled, type: 'checkbox' });
                if (resourceType === 'SKILL') {
                    fields.push({ key: 'tags', label: messages.proposalTagsPrompt, value: (resource.tags || []).join(', ') });
                    fields.push({ key: 'applicationMode', label: messages.mode, value: resource.applicationMode || 'ALWAYS', options: [{ value: 'CONTEXTUAL', label: messages.modeContextual }, { value: 'ALWAYS', label: messages.modeAlways }] });
                }
            } else if (resourceType === 'MEMORY') {
                fields.push({ key: 'content', label: messages.content, value: resource.content || '', multiline: true, required: true });
                fields.push({ key: 'tags', label: messages.proposalTagsPrompt, value: (resource.tags || []).join(', ') });
                fields.push({ key: 'applicationMode', label: messages.mode, value: resource.applicationMode || 'CONTEXTUAL', options: [{ value: 'CONTEXTUAL', label: messages.modeContextual }, { value: 'ALWAYS', label: messages.modeAlways }] });
                fields.push({ key: 'enabled', label: messages.enabled, value: resource.enabled, type: 'checkbox' });
            } else {
                fields.push({ key: 'description', label: messages.objective, value: resource.description || '', multiline: true, required: true });
                fields.push({ key: 'summary', label: messages.summary, value: resource.summary || '', multiline: true });
            }
            const values = await this.showFormDialog(messages.resourceEditTitle, fields);
            if (!values) return;
            var body;
            if (resourceType === 'SKILL') {
                body = { name: values.name.trim(), description: values.description, instructions: values.instructions, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean), applicationMode: values.applicationMode, enabled: values.enabled };
            } else if (resourceType === 'WORKFLOW') {
                body = { name: values.name.trim(), description: values.description, instructions: values.instructions, enabled: values.enabled };
            } else if (resourceType === 'MEMORY') {
                body = { title: values.name.trim(), content: values.content, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean), applicationMode: values.applicationMode, enabled: values.enabled };
            } else {
                body = { title: values.name.trim(), description: values.description, summary: values.summary };
            }
            try {
                const resourcePath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/' + resourcePathByType[resourceType] + '/' + resource.id;
                const updatedResource = await this.request(resourcePath, { method: 'PUT', headers: { 'If-Match': String(resource.version) }, body: JSON.stringify(body) });
                Object.assign(resource, updatedResource);
                this.noticeMessage = messages.resourceUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteResource(resource) {
            if (!await this.showConfirmDialog(messages.resourceDeleteConfirm, true)) return;
            try {
                const resourceId = resource.id;
                await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/' + resourcePathByType[this.activeResourceType] + '/' + resourceId, { method: 'DELETE' });
                this.resourceList = this.resourceList.filter(item => item.id !== resourceId);
                this.noticeMessage = messages.resourceDeleted;
            } catch (error) { this.showError(error); }
        },
        async loadProposals(task) {
            try {
                this.proposalTaskId = task.id;
                const page = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals');
                this.proposalList = page.items || [];
            } catch (error) { this.showError(error); }
        },
        async createProposal(task) {
            const values = await this.showFormDialog(messages.proposalNewTitle, [
                { key: 'proposedContent', label: messages.proposalContentPrompt, value: '', multiline: true, required: true },
                { key: 'reason', label: messages.proposalReasonPrompt, value: '', required: true },
                { key: 'tags', label: messages.proposalTagsPrompt, value: '' }
            ], messages.create);
            if (!values) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals';
                const proposal = await this.request(path, { method: 'POST', headers: { 'Idempotency-Key': window.crypto.randomUUID() }, body: JSON.stringify({ proposedContent: values.proposedContent, reason: values.reason, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean) }) });
                this.proposalList.unshift(proposal);
                this.noticeMessage = messages.proposalCreated;
            } catch (error) { this.showError(error); }
        },
        async editProposal(task, proposal) {
            const values = await this.showFormDialog(messages.proposalEditTitle, [
                { key: 'proposedContent', label: messages.proposalContentPrompt, value: proposal.proposedContent, multiline: true, required: true },
                { key: 'reason', label: messages.proposalReasonPrompt, value: proposal.reason, required: true },
                { key: 'tags', label: messages.proposalTagsPrompt, value: (proposal.tags || []).join(', ') }
            ]);
            if (!values) return;
            try {
                const proposalPath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id;
                const updatedProposal = await this.request(proposalPath, {
                    method: 'PUT',
                    headers: { 'If-Match': String(proposal.version) },
                    body: JSON.stringify({ proposedContent: values.proposedContent, reason: values.reason, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean) })
                });
                Object.assign(proposal, updatedProposal);
                this.noticeMessage = messages.proposalUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProposal(task, proposal) {
            if (!await this.showConfirmDialog(messages.proposalDeleteConfirm, true)) return;
            try {
                const proposalPath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id;
                await this.request(proposalPath, { method: 'DELETE' });
                this.proposalList = this.proposalList.filter(item => item.id !== proposal.id);
                this.noticeMessage = messages.proposalDeleted;
            } catch (error) { this.showError(error); }
        },
        async loadTaskBlocks(task) {
            try {
                this.blockTaskId = task.id;
                const blockPage = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks');
                this.blockList = blockPage.items || [];
            } catch (error) { this.showError(error); }
        },
        async createTaskBlock(task) {
            const values = await this.showFormDialog(messages.blockAdd, [
                { key: 'content', label: messages.blockNew, value: '', multiline: true, required: true }
            ], messages.blockAdd);
            if (!values) return;
            try {
                const block = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks', {
                    method: 'POST',
                    headers: { 'Idempotency-Key': window.crypto.randomUUID() },
                    body: JSON.stringify({ content: values.content })
                });
                this.blockList.push(block);
                this.noticeMessage = messages.blockAdded;
            } catch (error) { this.showError(error); }
        },
        async editTaskBlock(task, block) {
            const values = await this.showFormDialog(messages.blockContentPrompt, [
                { key: 'content', label: messages.blockContentPrompt, value: block.content, multiline: true, required: true }
            ]);
            if (!values) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks/' + block.id;
                const updatedBlock = await this.request(path, { method: 'PUT', headers: { 'If-Match': String(block.version) }, body: JSON.stringify({ content: values.content }) });
                Object.assign(block, updatedBlock);
                this.noticeMessage = messages.blockUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteTaskBlock(task, block) {
            if (!await this.showConfirmDialog(messages.blockDeleteConfirm, true)) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks/' + block.id;
                await this.request(path, { method: 'DELETE' });
                this.blockList = this.blockList.filter(item => item.id !== block.id);
                this.noticeMessage = messages.blockDeleted;
            } catch (error) { this.showError(error); }
        },
        async approveProposal(task, proposal) {
            const values = await this.showFormDialog(messages.proposalApproveTitle, [
                { key: 'title', label: messages.memoryTitlePrompt, value: proposal.title || messages.memoryFromTask, required: true },
                { key: 'content', label: messages.content, value: proposal.proposedContent, multiline: true, required: true },
                { key: 'tags', label: messages.proposalTagsPrompt, value: (proposal.tags || []).join(', ') },
                { key: 'applicationMode', label: messages.mode, value: 'CONTEXTUAL', options: [{ value: 'CONTEXTUAL', label: messages.modeContextual }, { value: 'ALWAYS', label: messages.modeAlways }] }
            ]);
            if (!values) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id + '/approve', {
                    method: 'POST',
                    body: JSON.stringify({ title: values.title.trim(), content: values.content, tags: values.tags.split(/[\n,]/).map(tag => tag.trim()).filter(Boolean), applicationMode: values.applicationMode })
                });
                this.proposalList = this.proposalList.filter(item => item.id !== proposal.id);
                this.noticeMessage = messages.proposalApproved;
            } catch (error) { this.showError(error); }
        },
        resourceTypeLabel(resourceType) {
            return { SKILL: messages.resourceSkill, WORKFLOW: messages.resourceWorkflow, MEMORY: messages.resourceMemory, TASK: messages.resourceTask }[resourceType];
        },
        roleLabel(role) {
            return { OWNER: messages.roleOwner, EDITOR: messages.roleEditor, READER: messages.roleReader }[role] || role;
        },
        showError(error) {
            this.errorMessage = error.message || messages.serverCommunicationError;
            this.noticeMessage = '';
        },
        clearMessages() {
            this.errorMessage = '';
            this.noticeMessage = '';
        }
    }
}).mount('#hub-app');
