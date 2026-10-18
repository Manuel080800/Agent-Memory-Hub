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
    descriptionEmpty: translations['ui.description.empty'],
    projectCreated: translations['ui.project.created'],
    projectUpdated: translations['ui.project.updated'],
    projectDeleteConfirm: translations['ui.project.delete.confirm'],
    projectDeleted: translations['ui.project.deleted'],
    projectNamePrompt: translations['ui.project.name.prompt'],
    projectDescriptionPrompt: translations['ui.project.description.prompt'],
    profileCreated: translations['ui.profile.created'],
    profileUpdated: translations['ui.profile.updated'],
    profileDeleted: translations['ui.profile.deleted'],
    profileDeleteConfirm: translations['ui.profile.delete.confirm'],
    profileNamePrompt: translations['ui.profile.name.prompt'],
    profileDescriptionPrompt: translations['ui.profile.description.prompt'],
    settingsSaved: translations['ui.settings.saved'],
    settingsResetConfirm: translations['ui.settings.reset.confirm'],
    settingsReset: translations['ui.settings.reset'],
    memberAdded: translations['ui.member.added'],
    memberRoleUpdated: translations['ui.member.roleUpdated'],
    memberRemoveConfirm: translations['ui.member.remove.confirm'],
    roleOwner: translations['ui.role.owner'],
    memberRemoved: translations['ui.member.removed'],
    connectionContextName: translations['ui.connection.contextName'],
    connectionTasksName: translations['ui.connection.tasksName'],
    connectionUpdated: translations['ui.connection.updated'],
    resourceCreated: translations['ui.resource.created'],
    resourceNamePrompt: translations['ui.resource.name.prompt'],
    resourceUpdated: translations['ui.resource.updated'],
    resourceDeleteConfirm: translations['ui.resource.delete.confirm'],
    resourceDeleted: translations['ui.resource.deleted'],
    proposalContentPrompt: translations['ui.proposal.content.prompt'],
    proposalReasonPrompt: translations['ui.proposal.reason.prompt'],
    proposalUpdated: translations['ui.proposal.updated'],
    proposalDeleteConfirm: translations['ui.proposal.delete.confirm'],
    proposalDeleted: translations['ui.proposal.deleted'],
    blockAdded: translations['ui.block.added'],
    blockContentPrompt: translations['ui.block.content.prompt'],
    blockUpdated: translations['ui.block.updated'],
    blockDeleteConfirm: translations['ui.block.delete.confirm'],
    blockDeleted: translations['ui.block.deleted'],
    memoryTitlePrompt: translations['ui.memory.title.prompt'],
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
            memberForm: { userId: '', projectRole: 'READER' },
            blockList: [],
            blockTaskId: null,
            blockContent: '',
            messages,
            platformRole,
            currentProjectRole: null,
            selectedProject: null,
            activeProfile: null,
            activeProfileId: '',
            activeResourceType: 'SKILL',
            proposalTaskId: null,
            resourceTypeList: ['SKILL', 'WORKFLOW', 'MEMORY', 'TASK'],
            projectForm: { name: '', description: '' },
            profileForm: { name: '', description: '' },
            resourceForm: { title: '', description: '', instructions: '', applicationMode: 'CONTEXTUAL' },
            showCreateProject: false,
            showCreateProfile: false,
            showCreateResource: false,
            errorMessage: '',
            noticeMessage: ''
        };
    },
    mounted() {
        this.loadProjects();
    },
    computed: {
        canEditProject() {
            return this.currentProjectRole === 'OWNER' || this.currentProjectRole === 'EDITOR';
        }
    },
    methods: {
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
                this.profileList = profilePage.items || [];
                this.settingsRecord = settingsRecord;
                this.settingsForm = { ...settingsRecord };
                this.memberList = memberPage.items || [];
                this.currentProjectRole = (this.memberList.find(member => member.userId === userId) || {}).projectRole || null;
                this.connectionList = connectionPage.items || [];
                this.activeProfileId = this.profileList.some(profile => profile.id === project.activeProfileId) ? project.activeProfileId : '';
                this.activeProfile = this.profileList.find(profile => profile.id === this.activeProfileId) || null;
                this.resourceList = [];
                if (this.activeProfileId) await this.loadResources(this.activeResourceType);
            } catch (error) { this.showError(error); }
        },
        async createProject() {
            try {
                await this.request('/projects', { method: 'POST', body: JSON.stringify({ ...this.projectForm, enabled: true }) });
                this.projectForm = { name: '', description: '' };
                this.showCreateProject = false;
                this.noticeMessage = messages.projectCreated;
                await this.loadProjects();
            } catch (error) { this.showError(error); }
        },
        async editProject() {
            const name = window.prompt(messages.projectNamePrompt, this.selectedProject.name);
            if (name === null || !name.trim()) return;
            const description = window.prompt(messages.projectDescriptionPrompt, this.selectedProject.description || '');
            if (description === null) return;
            try {
                const projectPath = '/projects/' + this.selectedProject.id;
                this.selectedProject = await this.request(projectPath, {
                    method: 'PUT',
                    body: JSON.stringify({ name: name.trim(), description, enabled: this.selectedProject.enabled })
                });
                await this.loadProjects();
                this.noticeMessage = messages.projectUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProject() {
            if (!window.confirm(messages.projectDeleteConfirm)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '?confirm=true', { method: 'DELETE' });
                this.projectList = this.projectList.filter(project => project.id !== this.selectedProject.id);
                this.selectedProject = null;
                this.profileList = [];
                this.resourceList = [];
                this.noticeMessage = messages.projectDeleted;
            } catch (error) { this.showError(error); }
        },
        async createProfile() {
            try {
                const profile = await this.request('/projects/' + this.selectedProject.id + '/profiles', { method: 'POST', body: JSON.stringify({ ...this.profileForm, enabled: true }) });
                this.profileForm = { name: '', description: '' };
                this.showCreateProfile = false;
                this.profileList.push(profile);
                this.noticeMessage = messages.profileCreated;
            } catch (error) { this.showError(error); }
        },
        async editProfile() {
            const profile = this.activeProfile;
            const name = window.prompt(messages.profileNamePrompt, profile.name);
            if (name === null || !name.trim()) return;
            const description = window.prompt(messages.profileDescriptionPrompt, profile.description || '');
            if (description === null) return;
            try {
                const profilePath = '/projects/' + this.selectedProject.id + '/profiles/' + profile.id;
                this.activeProfile = await this.request(profilePath, {
                    method: 'PUT',
                    body: JSON.stringify({ name: name.trim(), description, enabled: profile.enabled })
                });
                Object.assign(profile, this.activeProfile);
                this.noticeMessage = messages.profileUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProfile() {
            if (!window.confirm(messages.profileDeleteConfirm)) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '?confirm=true', { method: 'DELETE' });
                this.profileList = this.profileList.filter(profile => profile.id !== this.activeProfileId);
                this.activeProfile = null;
                this.activeProfileId = '';
                this.resourceList = [];
                this.noticeMessage = messages.profileDeleted;
            } catch (error) { this.showError(error); }
        },
        async selectActiveProfile() {
            if (!this.activeProfileId) return;
            try {
                this.activeProfile = await this.request('/projects/' + this.selectedProject.id + '/active-profile/' + this.activeProfileId, { method: 'PUT' });
                this.selectedProject.activeProfileId = this.activeProfileId;
                await this.loadResources(this.activeResourceType);
            } catch (error) { this.showError(error); }
        },
        async saveSettings() {
            try {
                this.settingsRecord = await this.request('/projects/' + this.selectedProject.id + '/settings', { method: 'PUT', body: JSON.stringify(this.settingsPayload()) });
                this.settingsForm = { ...this.settingsRecord };
                this.noticeMessage = messages.settingsSaved;
            } catch (error) { this.showError(error); }
        },
        async resetSettings() {
            if (!window.confirm(messages.settingsResetConfirm)) return;
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
            try {
                const member = await this.request('/projects/' + this.selectedProject.id + '/members', { method: 'POST', body: JSON.stringify(this.memberForm) });
                this.memberList.push(member);
                this.memberForm = { userId: '', projectRole: 'READER' };
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
            if (!window.confirm(messages.memberRemoveConfirm.replace('{0}', member.fullName))) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/members/' + member.userId + '?confirm=true', { method: 'DELETE' });
                this.memberList = this.memberList.filter(item => item.userId !== member.userId);
                this.noticeMessage = messages.memberRemoved;
            } catch (error) { this.showError(error); }
        },
        connectionForType(connectionType) {
            return this.connectionList.find(connection => connection.connectionType === connectionType) || null;
        },
        async toggleConnection(connectionType) {
            try {
                const connection = this.connectionForType(connectionType);
                const path = '/projects/' + this.selectedProject.id + '/mcp-connections';
                if (connection) {
                    await this.request(path + '/' + connection.id, { method: 'PUT', body: JSON.stringify({ enabled: !connection.enabled }) });
                } else {
                    const name = connectionType === 'CONTEXT' ? messages.connectionContextName : messages.connectionTasksName;
                    await this.request(path, { method: 'POST', body: JSON.stringify({ name, connectionType, enabled: true }) });
                }
                const page = await this.request(path);
                this.connectionList = page.items || [];
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
            try {
                const body = this.resourceBody();
                const resource = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/resources/' + this.activeResourceType, { method: 'POST', body: JSON.stringify(body) });
                this.resourceList.unshift(resource);
                this.resourceForm = { title: '', description: '', instructions: '', applicationMode: 'CONTEXTUAL' };
                this.showCreateResource = false;
                this.noticeMessage = messages.resourceCreated.replace('{0}', this.resourceTypeLabel(this.activeResourceType));
            } catch (error) { this.showError(error); }
        },
        resourceBody() {
            const enabled = true;
            if (this.activeResourceType === 'SKILL' || this.activeResourceType === 'WORKFLOW') {
                return { name: this.resourceForm.title, description: '', instructions: this.resourceForm.instructions, enabled, ...(this.activeResourceType === 'SKILL' ? { tags: [], applicationMode: 'ALWAYS' } : {}) };
            }
            if (this.activeResourceType === 'MEMORY') {
                return { title: this.resourceForm.title, content: this.resourceForm.description, tags: [], applicationMode: this.resourceForm.applicationMode, enabled };
            }
            return { title: this.resourceForm.title, description: this.resourceForm.description, summary: '' };
        },
        async editResource(resource) {
            const resourceType = this.activeResourceType;
            const currentName = resource.name || resource.title;
            const updatedName = window.prompt(messages.resourceNamePrompt, currentName);
            if (updatedName === null || !updatedName.trim()) return;
            const contentPrompt = resourceType === 'MEMORY' ? messages.content : resourceType === 'TASK' ? messages.objective : messages.instructions;
            const currentContent = resource.content || resource.instructions || resource.description || '';
            const updatedContent = window.prompt(contentPrompt, currentContent);
            if (updatedContent === null) return;
            if ((resourceType === 'MEMORY' || resourceType === 'TASK') && !updatedContent.trim()) return;
            const body = this.updatedResourceBody(resource, updatedName.trim(), updatedContent);
            try {
                const resourcePath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/' + resourcePathByType[resourceType] + '/' + resource.id;
                const updatedResource = await this.request(resourcePath, { method: 'PUT', body: JSON.stringify(body) });
                Object.assign(resource, updatedResource);
                this.noticeMessage = messages.resourceUpdated;
            } catch (error) { this.showError(error); }
        },
        updatedResourceBody(resource, updatedName, updatedContent) {
            if (this.activeResourceType === 'SKILL') {
                return { name: updatedName, description: resource.description || '', instructions: updatedContent, tags: resource.tags || [], applicationMode: resource.applicationMode || 'ALWAYS', enabled: resource.enabled };
            }
            if (this.activeResourceType === 'WORKFLOW') {
                return { name: updatedName, description: resource.description || '', instructions: updatedContent, enabled: resource.enabled };
            }
            if (this.activeResourceType === 'MEMORY') {
                return { title: updatedName, content: updatedContent, tags: resource.tags || [], applicationMode: resource.applicationMode || 'CONTEXTUAL', enabled: resource.enabled };
            }
            return { title: updatedName, description: updatedContent, summary: resource.summary || '' };
        },
        async deleteResource(resource) {
            if (!window.confirm(messages.resourceDeleteConfirm)) return;
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
        async editProposal(task, proposal) {
            const proposedContent = window.prompt(messages.proposalContentPrompt, proposal.proposedContent);
            if (proposedContent === null || !proposedContent.trim()) return;
            const reason = window.prompt(messages.proposalReasonPrompt, proposal.reason);
            if (reason === null || !reason.trim()) return;
            try {
                const proposalPath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id;
                const updatedProposal = await this.request(proposalPath, {
                    method: 'PUT',
                    body: JSON.stringify({ proposedContent, reason, tags: proposal.tags || [] })
                });
                Object.assign(proposal, updatedProposal);
                this.noticeMessage = messages.proposalUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteProposal(task, proposal) {
            if (!window.confirm(messages.proposalDeleteConfirm)) return;
            try {
                const proposalPath = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id + '?confirm=true';
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
            try {
                const block = await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks', {
                    method: 'POST',
                    body: JSON.stringify({ content: this.blockContent })
                });
                this.blockList.push(block);
                this.blockContent = '';
                this.noticeMessage = messages.blockAdded;
            } catch (error) { this.showError(error); }
        },
        async editTaskBlock(task, block) {
            const content = window.prompt(messages.blockContentPrompt, block.content);
            if (content === null || !content.trim()) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks/' + block.id;
                const updatedBlock = await this.request(path, { method: 'PUT', body: JSON.stringify({ content }) });
                Object.assign(block, updatedBlock);
                this.noticeMessage = messages.blockUpdated;
            } catch (error) { this.showError(error); }
        },
        async deleteTaskBlock(task, block) {
            if (!window.confirm(messages.blockDeleteConfirm)) return;
            try {
                const path = '/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/blocks/' + block.id + '?confirm=true';
                await this.request(path, { method: 'DELETE' });
                this.blockList = this.blockList.filter(item => item.id !== block.id);
                this.noticeMessage = messages.blockDeleted;
            } catch (error) { this.showError(error); }
        },
        async approveProposal(task, proposal) {
            const title = window.prompt(messages.memoryTitlePrompt, proposal.title || messages.memoryFromTask);
            if (!title) return;
            try {
                await this.request('/projects/' + this.selectedProject.id + '/profiles/' + this.activeProfileId + '/tasks/' + task.id + '/proposals/' + proposal.id + '/approve', {
                    method: 'POST',
                    body: JSON.stringify({ title, content: proposal.proposedContent, tags: proposal.tags || [], applicationMode: 'CONTEXTUAL' })
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
