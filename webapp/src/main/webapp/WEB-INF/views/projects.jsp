<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="${csrfToken}">
    <title>Agent Memory Hub</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
    <style>
        body { background: #f5f7fb; }
        .hub-header { background: #10213d; }
        .hub-brand { letter-spacing: .02em; }
        .resource-card { min-height: 10rem; }
        .content-preview { white-space: pre-wrap; overflow-wrap: anywhere; max-height: 15rem; overflow: auto; }
    </style>
</head>
<body>
<nav class="navbar navbar-dark hub-header shadow-sm">
    <div class="container-fluid px-lg-5">
        <a class="navbar-brand hub-brand fw-semibold" href="${pageContext.request.contextPath}/dashboard">Agent Memory Hub</a>
        <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <button class="btn btn-outline-light btn-sm" type="submit"><spring:message code='ui.logout' /></button>
        </form>
    </div>
</nav>

<main id="hub-app" class="container-fluid px-lg-5 py-4" data-user-id="${userId}" data-platform-role="${platformRole}">
    <div class="d-flex flex-wrap justify-content-between align-items-end gap-3 mb-4">
        <div>
            <p class="text-primary fw-semibold mb-1"><spring:message code='ui.workspaces' /></p>
            <h1 class="h2 mb-0"><spring:message code='ui.projectsKnowledge' /></h1>
        </div>
        <div class="d-flex gap-2">
            <a v-if="platformRole === 'ADMIN'" class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/users"><spring:message code='ui.users' /></a>
            <button class="btn btn-primary" type="button" @click="showCreateProject = !showCreateProject"><spring:message code='ui.project.new' /></button>
        </div>
    </div>

    <div v-if="errorMessage" class="alert alert-danger" role="alert">{{ errorMessage }}</div>
    <div v-if="noticeMessage" class="alert alert-success" role="status">{{ noticeMessage }}</div>

    <section v-if="showCreateProject" class="card border-0 shadow-sm mb-4">
        <div class="card-body">
            <h2 class="h5"><spring:message code='ui.project.create' /></h2>
            <form class="row g-3" @submit.prevent="createProject">
                <div class="col-md-4"><label class="form-label" for="project-name"><spring:message code='ui.name' /></label><input id="project-name" v-model="projectForm.name" class="form-control" required></div>
                <div class="col-md-6"><label class="form-label" for="project-description"><spring:message code='ui.description' /></label><input id="project-description" v-model="projectForm.description" class="form-control"></div>
                <div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit"><spring:message code='ui.create' /></button></div>
            </form>
        </div>
    </section>

    <div class="row g-4">
        <aside class="col-xl-3 col-lg-4">
            <section class="card border-0 shadow-sm">
                <div class="card-header bg-white py-3"><h2 class="h6 mb-0"><spring:message code='ui.project.mine' /></h2></div>
                <div class="list-group list-group-flush">
                    <button v-for="project in projectList" :key="project.id" class="list-group-item list-group-item-action py-3" :class="{ active: selectedProject && selectedProject.id === project.id }" type="button" @click="openProject(project.id)">
                        <span class="d-block fw-semibold">{{ project.name }}</span>
                        <small :class="selectedProject && selectedProject.id === project.id ? 'text-white-50' : 'text-body-secondary'">{{ project.enabled ? messages.active : messages.inactive }}</small>
                    </button>
                    <div v-if="projectList.length === 0" class="p-3 text-body-secondary"><spring:message code='ui.projects.empty' /></div>
                </div>
            </section>
        </aside>

        <section class="col-xl-9 col-lg-8">
            <div v-if="!selectedProject" class="card border-0 shadow-sm">
                <div class="card-body py-5 text-center text-body-secondary"><spring:message code='ui.project.selectHint' /></div>
            </div>

            <template v-if="selectedProject">
                <section class="card border-0 shadow-sm mb-4">
                    <div class="card-body d-flex flex-wrap justify-content-between align-items-start gap-3">
                        <div>
                            <p class="text-primary fw-semibold mb-1"><spring:message code='ui.project.label' /></p>
                            <h2 class="h3">{{ selectedProject.name }}</h2>
                            <p class="text-body-secondary mb-0">{{ selectedProject.description || messages.descriptionEmpty }}</p>
                        </div>
                        <div class="d-flex flex-wrap gap-2 align-items-end">
                            <button v-if="canEditProject" class="btn btn-outline-secondary" type="button" @click="editProject"><spring:message code='ui.project.edit' /></button>
                            <button v-if="currentProjectRole === 'OWNER'" class="btn btn-outline-danger" type="button" @click="deleteProject"><spring:message code='ui.project.delete' /></button>
                            <div>
                                <label class="form-label" for="active-profile"><spring:message code='ui.profile.selected' /></label>
                                <select id="active-profile" v-model="activeProfileId" class="form-select" :disabled="currentProjectRole !== 'OWNER'" @change="selectActiveProfile">
                                    <option value=""><spring:message code='ui.profile.noneSelected' /></option>
                                    <option v-for="profile in profileList" :key="profile.id" :value="profile.id">{{ profile.name }}</option>
                                </select>
                            </div>
                            <button v-if="canEditProject" class="btn btn-outline-primary" type="button" @click="showCreateProfile = !showCreateProfile"><spring:message code='ui.profile.new' /></button>
                        </div>
                    </div>
                    <div v-if="activeProfile" class="card-footer bg-white d-flex flex-wrap gap-2">
                        <button v-if="canEditProject" class="btn btn-sm btn-outline-secondary" type="button" @click="editProfile"><spring:message code='ui.profile.edit' /></button>
                        <button v-if="currentProjectRole === 'OWNER'" class="btn btn-sm btn-outline-danger" type="button" @click="deleteProfile"><spring:message code='ui.profile.delete' /></button>
                    </div>
                </section>

                <div class="row g-3 mb-4">
                    <section class="col-xl-5">
                        <div class="card border-0 shadow-sm h-100">
                            <div class="card-body">
                                <div class="d-flex justify-content-between align-items-center mb-3">
                                    <div><p class="text-primary fw-semibold mb-1"><spring:message code='ui.settings' /></p><h3 class="h6 mb-0"><spring:message code='ui.settings.recoveryTasks' /></h3></div>
                                    <button v-if="currentProjectRole === 'OWNER'" class="btn btn-sm btn-outline-secondary" type="button" @click="resetSettings"><spring:message code='ui.reset' /></button>
                                </div>
                                <form class="row g-2" @submit.prevent="saveSettings">
                                    <div class="col-12 form-check form-switch"><input id="context-enabled" v-model="settingsForm.contextRetrievalEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="context-enabled"><spring:message code='ui.context.retrieval' /></label></div>
                                    <div class="col-12 form-check form-switch"><input id="compact-enabled" v-model="settingsForm.compactRecoveryEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="compact-enabled"><spring:message code='ui.context.compaction' /></label></div>
                                    <div class="col-12 form-check form-switch"><input id="selection-enabled" v-model="settingsForm.contextualSelectionEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="selection-enabled"><spring:message code='ui.context.selection' /></label></div>
                                    <div class="col-12 form-check form-switch"><input id="task-retrieval" v-model="settingsForm.selectedTaskRetrievalEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="task-retrieval"><spring:message code='ui.task.selectedRetrieval' /></label></div>
                                    <div class="col-12 form-check form-switch"><input id="summary-enabled" v-model="settingsForm.automaticSummaryEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="summary-enabled"><spring:message code='ui.summary.automatic' /></label></div>
                                    <div class="col-md-6"><label class="form-label" for="creation-mode"><spring:message code='ui.task.creation' /></label><select id="creation-mode" v-model="settingsForm.taskCreationMode" class="form-select" :disabled="currentProjectRole !== 'OWNER'"><option value="MANUAL"><spring:message code='ui.manual' /></option><option value="SUGGEST"><spring:message code='ui.suggest' /></option><option value="AUTOMATIC"><spring:message code='ui.automatic' /></option></select></div>
                                    <div class="col-md-3"><label class="form-label" for="budget"><spring:message code='ui.budget' /></label><input id="budget" v-model.number="settingsForm.contextBudgetTokens" class="form-control" type="number" min="100" max="100000" :disabled="currentProjectRole !== 'OWNER'"></div>
                                    <div class="col-md-3"><label class="form-label" for="quota"><spring:message code='ui.permanentQuota' /></label><input id="quota" v-model.number="settingsForm.permanentQuotaPercent" class="form-control" type="number" min="0" max="100" :disabled="currentProjectRole !== 'OWNER'"></div>
                                    <div v-if="currentProjectRole === 'OWNER'" class="col-12 mt-2"><button class="btn btn-sm btn-primary" type="submit"><spring:message code='ui.settings.save' /></button></div>
                                </form>
                            </div>
                        </div>
                    </section>
                    <section class="col-xl-4">
                        <div class="card border-0 shadow-sm h-100">
                            <div class="card-body">
                                <p class="text-primary fw-semibold mb-1"><spring:message code='ui.connections' /></p><h3 class="h6 mb-3"><spring:message code='ui.connections.servers' /></h3>
                                <div v-for="connectionType in ['CONTEXT', 'TASKS']" :key="connectionType" class="d-flex justify-content-between align-items-center border-bottom py-2">
                                    <div><span class="fw-semibold">{{ connectionType === 'CONTEXT' ? messages.context : messages.tasks }}</span><small class="d-block text-body-secondary">{{ connectionForType(connectionType) ? (connectionForType(connectionType).enabled ? messages.enabled : messages.disabled) : messages.connectionUnconfigured }}</small></div>
                                    <button v-if="currentProjectRole === 'OWNER'" class="btn btn-sm" :class="connectionForType(connectionType) && connectionForType(connectionType).enabled ? 'btn-outline-danger' : 'btn-outline-success'" type="button" @click="toggleConnection(connectionType)">{{ connectionForType(connectionType) && connectionForType(connectionType).enabled ? messages.disable : messages.enable }}</button>
                                </div>
                            </div>
                        </div>
                    </section>
                    <section class="col-xl-3">
                        <div class="card border-0 shadow-sm h-100">
                            <div class="card-body">
                                <p class="text-primary fw-semibold mb-1"><spring:message code='ui.members' /></p><h3 class="h6 mb-3"><spring:message code='ui.members.access' /></h3>
                                <div v-for="member in memberList" :key="member.userId" class="d-flex justify-content-between align-items-start border-bottom py-2">
                                    <div><span class="d-block fw-semibold">{{ member.fullName }}</span><small class="text-body-secondary">{{ member.email }}</small><select v-if="currentProjectRole === 'OWNER' && member.projectRole !== 'OWNER'" v-model="member.projectRole" class="form-select form-select-sm mt-1"><option value="READER"><spring:message code='ui.role.reader' /></option><option value="EDITOR"><spring:message code='ui.role.editor' /></option></select><small v-else class="d-block text-body-secondary">{{ roleLabel(member.projectRole) }}</small></div>
                                    <div v-if="currentProjectRole === 'OWNER' && member.projectRole !== 'OWNER'" class="d-flex flex-column gap-1"><button class="btn btn-sm btn-link p-0" type="button" @click="updateMember(member)"><spring:message code='ui.member.saveRole' /></button><button class="btn btn-sm btn-link text-danger p-0" type="button" @click="removeMember(member)"><spring:message code='ui.remove' /></button></div>
                                </div>
                                <form v-if="currentProjectRole === 'OWNER'" class="mt-3" @submit.prevent="addMember">
                                    <label class="form-label" for="member-user-id"><spring:message code='ui.member.userId' /></label><input id="member-user-id" v-model="memberForm.userId" class="form-control form-control-sm mb-2" required>
                                    <select v-model="memberForm.projectRole" class="form-select form-select-sm mb-2"><option value="READER"><spring:message code='ui.role.reader' /></option><option value="EDITOR"><spring:message code='ui.role.editor' /></option></select>
                                    <button class="btn btn-sm btn-outline-primary w-100" type="submit"><spring:message code='ui.member.add' /></button>
                                </form>
                            </div>
                        </div>
                    </section>
                </div>

                <section v-if="showCreateProfile && canEditProject" class="card border-0 shadow-sm mb-4">
                    <div class="card-body">
                        <h3 class="h5"><spring:message code='ui.profile.create' /></h3>
                        <form class="row g-3" @submit.prevent="createProfile">
                            <div class="col-md-4"><label class="form-label" for="profile-name"><spring:message code='ui.name' /></label><input id="profile-name" v-model="profileForm.name" class="form-control" required></div>
                            <div class="col-md-6"><label class="form-label" for="profile-description"><spring:message code='ui.description' /></label><input id="profile-description" v-model="profileForm.description" class="form-control"></div>
                            <div class="col-md-2 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit"><spring:message code='ui.create' /></button></div>
                        </form>
                    </div>
                </section>

                <section v-if="activeProfileId" class="card border-0 shadow-sm">
                    <div class="card-header bg-white border-bottom-0 pt-3">
                        <div class="d-flex flex-wrap justify-content-between align-items-center gap-3">
                            <div>
                                <p class="text-primary fw-semibold mb-1"><spring:message code='ui.profile.active' /></p>
                                <h3 class="h5 mb-0">{{ activeProfile && activeProfile.name }}</h3>
                            </div>
                            <div class="btn-group flex-wrap" role="group" aria-label="<spring:message code='ui.resource.type' />">
                                <button v-for="resourceType in resourceTypeList" :key="resourceType" class="btn" :class="activeResourceType === resourceType ? 'btn-primary' : 'btn-outline-primary'" type="button" @click="loadResources(resourceType)">{{ resourceTypeLabel(resourceType) }}</button>
                            </div>
                        </div>
                    </div>
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center gap-2 mb-3">
                            <h4 class="h6 mb-0">{{ resourceTypeLabel(activeResourceType) }}</h4>
                            <button v-if="canEditProject" class="btn btn-sm btn-primary" type="button" @click="showCreateResource = !showCreateResource"><spring:message code='ui.add' /></button>
                        </div>

                        <form v-if="showCreateResource && canEditProject" class="row g-3 p-3 bg-body-tertiary rounded mb-4" @submit.prevent="createResource">
                            <div class="col-md-6"><label class="form-label" for="resource-title">{{ activeResourceType === 'MEMORY' || activeResourceType === 'TASK' ? messages.title : messages.resourceName }}</label><input id="resource-title" v-model="resourceForm.title" class="form-control" required></div>
                            <div v-if="activeResourceType !== 'SKILL' && activeResourceType !== 'WORKFLOW'" class="col-md-6"><label class="form-label" for="resource-description">{{ activeResourceType === 'MEMORY' ? messages.content : messages.objective }}</label><textarea id="resource-description" v-model="resourceForm.description" class="form-control" rows="2" required></textarea></div>
                            <div v-if="activeResourceType === 'SKILL' || activeResourceType === 'WORKFLOW'" class="col-md-6"><label class="form-label" for="resource-instructions"><spring:message code='ui.instructions' /></label><textarea id="resource-instructions" v-model="resourceForm.instructions" class="form-control" rows="2" required></textarea></div>
                            <div v-if="activeResourceType === 'MEMORY'" class="col-md-4"><label class="form-label" for="application-mode"><spring:message code='ui.mode' /></label><select id="application-mode" v-model="resourceForm.applicationMode" class="form-select"><option value="CONTEXTUAL"><spring:message code='ui.mode.contextual' /></option><option value="ALWAYS"><spring:message code='ui.mode.always' /></option></select></div>
                            <div class="col-12"><button class="btn btn-primary" type="submit"><spring:message code='ui.save' /></button> <button class="btn btn-link" type="button" @click="showCreateResource = false"><spring:message code='ui.cancel' /></button></div>
                        </form>

                        <div v-if="resourceList.length === 0" class="text-body-secondary py-4 text-center"><spring:message code='ui.resources.empty' /></div>
                        <div class="row g-3">
                            <article v-for="resource in resourceList" :key="resource.id" class="col-12">
                                <div class="card resource-card bg-body-tertiary border-0">
                                    <div class="card-body">
                                        <div class="d-flex justify-content-between gap-3">
                                            <div>
                                                <h5 class="h6">{{ resource.title || resource.name }}</h5>
                                                <p v-if="resource.description" class="text-body-secondary">{{ resource.description }}</p>
                                            </div>
                                            <div v-if="canEditProject" class="d-flex gap-2">
                                                <button class="btn btn-sm btn-outline-primary align-self-start" type="button" @click="editResource(resource)"><spring:message code='ui.edit' /></button>
                                                <button class="btn btn-sm btn-outline-danger align-self-start" type="button" @click="deleteResource(resource)"><spring:message code='ui.delete' /></button>
                                            </div>
                                        </div>
                                        <p v-if="resource.content || resource.instructions || resource.summary" class="content-preview mb-0">{{ resource.content || resource.instructions || resource.summary }}</p>
                                        <div v-if="activeResourceType === 'TASK'" class="mt-3">
                                            <button class="btn btn-sm btn-outline-secondary me-2" type="button" @click="loadTaskBlocks(resource)"><spring:message code='ui.task.blocks' /></button>
                                            <button class="btn btn-sm btn-outline-primary" type="button" @click="loadProposals(resource)"><spring:message code='ui.proposal.view' /></button>
                                            <div v-if="blockTaskId === resource.id" class="mt-3 p-3 bg-white border rounded">
                                                <h6 class="mb-3"><spring:message code='ui.block.temporary' /></h6>
                                                <div v-for="block in blockList" :key="block.id" class="border-bottom py-2">
                                                    <div class="d-flex justify-content-between gap-3"><p class="content-preview mb-2">{{ block.content }}</p><div v-if="canEditProject" class="text-nowrap"><button class="btn btn-sm btn-link" type="button" @click="editTaskBlock(resource, block)"><spring:message code='ui.edit' /></button><button class="btn btn-sm btn-link text-danger" type="button" @click="deleteTaskBlock(resource, block)"><spring:message code='ui.delete' /></button></div></div>
                                                </div>
                                                <p v-if="blockList.length === 0" class="small text-body-secondary"><spring:message code='ui.block.empty' /></p>
                                                <form v-if="canEditProject" class="mt-3" @submit.prevent="createTaskBlock(resource)"><textarea v-model="blockContent" class="form-control mb-2" rows="2" placeholder="<spring:message code='ui.block.new' />" required></textarea><button class="btn btn-sm btn-primary" type="submit"><spring:message code='ui.block.add' /></button></form>
                                            </div>
                                            <div v-if="proposalTaskId === resource.id" class="mt-3">
                                                <div v-for="proposal in proposalList" :key="proposal.id" class="border rounded p-3 mb-2 bg-white">
                                                    <div class="d-flex justify-content-between gap-3">
                                                        <div><p class="fw-semibold mb-1">{{ proposal.reason }}</p><p class="content-preview mb-2">{{ proposal.proposedContent }}</p></div>
                                                        <div class="d-flex gap-2 align-self-start"><button v-if="canEditProject && !proposal.processed" class="btn btn-sm btn-outline-primary" type="button" @click="editProposal(resource, proposal)"><spring:message code='ui.edit' /></button><button v-if="currentProjectRole === 'OWNER' && !proposal.processed" class="btn btn-sm btn-success" type="button" @click="approveProposal(resource, proposal)"><spring:message code='ui.proposal.approve' /></button><button v-if="canEditProject && !proposal.processed" class="btn btn-sm btn-outline-danger" type="button" @click="deleteProposal(resource, proposal)"><spring:message code='ui.delete' /></button></div>
                                                    </div>
                                                </div>
                                                <p v-if="proposalList.length === 0" class="small text-body-secondary mb-0"><spring:message code='ui.proposal.empty' /></p>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </article>
                        </div>
                    </div>
                </section>
            </template>
        </section>
    </div>
</main>

<script src="${pageContext.request.contextPath}/webjars/vue/3.5.22/dist/vue.global.prod.js"></script>
<script>
const csrfToken = document.querySelector('meta[name="csrf-token"]').content;
const contextPath = '${pageContext.request.contextPath}';
const userId = document.querySelector('#hub-app').dataset.userId;
const platformRole = document.querySelector('#hub-app').dataset.platformRole;
const resourcePathByType = { SKILL: 'skills', WORKFLOW: 'workflows', MEMORY: 'memories', TASK: 'tasks' };
const messages = {
    requestFailed: "<spring:message code='ui.request.failed' javaScriptEscape='true' />",
    requestError: "<spring:message code='ui.request.error' javaScriptEscape='true' />",
    active: "<spring:message code='ui.active' javaScriptEscape='true' />",
    inactive: "<spring:message code='ui.inactive' javaScriptEscape='true' />",
    enabled: "<spring:message code='ui.enabled' javaScriptEscape='true' />",
    disabled: "<spring:message code='ui.disabled' javaScriptEscape='true' />",
    connectionUnconfigured: "<spring:message code='ui.connection.unconfigured' javaScriptEscape='true' />",
    disable: "<spring:message code='ui.disable' javaScriptEscape='true' />",
    enable: "<spring:message code='ui.enable' javaScriptEscape='true' />",
    context: "<spring:message code='ui.context' javaScriptEscape='true' />",
    tasks: "<spring:message code='ui.tasks' javaScriptEscape='true' />",
    roleReader: "<spring:message code='ui.role.reader' javaScriptEscape='true' />",
    roleEditor: "<spring:message code='ui.role.editor' javaScriptEscape='true' />",
    title: "<spring:message code='ui.title' javaScriptEscape='true' />",
    resourceName: "<spring:message code='ui.resource.name' javaScriptEscape='true' />",
    content: "<spring:message code='ui.content' javaScriptEscape='true' />",
    objective: "<spring:message code='ui.objective' javaScriptEscape='true' />",
    instructions: "<spring:message code='ui.instructions' javaScriptEscape='true' />",
    descriptionEmpty: "<spring:message code='ui.description.empty' javaScriptEscape='true' />",
    projectCreated: "<spring:message code='ui.project.created' javaScriptEscape='true' />",
    projectUpdated: "<spring:message code='ui.project.updated' javaScriptEscape='true' />",
    projectDeleteConfirm: "<spring:message code='ui.project.delete.confirm' javaScriptEscape='true' />",
    projectDeleted: "<spring:message code='ui.project.deleted' javaScriptEscape='true' />",
    projectNamePrompt: "<spring:message code='ui.project.name.prompt' javaScriptEscape='true' />",
    projectDescriptionPrompt: "<spring:message code='ui.project.description.prompt' javaScriptEscape='true' />",
    profileCreated: "<spring:message code='ui.profile.created' javaScriptEscape='true' />",
    profileUpdated: "<spring:message code='ui.profile.updated' javaScriptEscape='true' />",
    profileDeleted: "<spring:message code='ui.profile.deleted' javaScriptEscape='true' />",
    profileDeleteConfirm: "<spring:message code='ui.profile.delete.confirm' javaScriptEscape='true' />",
    profileNamePrompt: "<spring:message code='ui.profile.name.prompt' javaScriptEscape='true' />",
    profileDescriptionPrompt: "<spring:message code='ui.profile.description.prompt' javaScriptEscape='true' />",
    settingsSaved: "<spring:message code='ui.settings.saved' javaScriptEscape='true' />",
    settingsResetConfirm: "<spring:message code='ui.settings.reset.confirm' javaScriptEscape='true' />",
    settingsReset: "<spring:message code='ui.settings.reset' javaScriptEscape='true' />",
    memberAdded: "<spring:message code='ui.member.added' javaScriptEscape='true' />",
    memberRoleUpdated: "<spring:message code='ui.member.roleUpdated' javaScriptEscape='true' />",
    memberRemoveConfirm: "<spring:message code='ui.member.remove.confirm' javaScriptEscape='true' />",
    roleOwner: "<spring:message code='ui.role.owner' javaScriptEscape='true' />",
    memberRemoved: "<spring:message code='ui.member.removed' javaScriptEscape='true' />",
    connectionContextName: "<spring:message code='ui.connection.contextName' javaScriptEscape='true' />",
    connectionTasksName: "<spring:message code='ui.connection.tasksName' javaScriptEscape='true' />",
    connectionUpdated: "<spring:message code='ui.connection.updated' javaScriptEscape='true' />",
    resourceCreated: "<spring:message code='ui.resource.created' javaScriptEscape='true' />",
    resourceNamePrompt: "<spring:message code='ui.resource.name.prompt' javaScriptEscape='true' />",
    resourceUpdated: "<spring:message code='ui.resource.updated' javaScriptEscape='true' />",
    resourceDeleteConfirm: "<spring:message code='ui.resource.delete.confirm' javaScriptEscape='true' />",
    resourceDeleted: "<spring:message code='ui.resource.deleted' javaScriptEscape='true' />",
    proposalContentPrompt: "<spring:message code='ui.proposal.content.prompt' javaScriptEscape='true' />",
    proposalReasonPrompt: "<spring:message code='ui.proposal.reason.prompt' javaScriptEscape='true' />",
    proposalUpdated: "<spring:message code='ui.proposal.updated' javaScriptEscape='true' />",
    proposalDeleteConfirm: "<spring:message code='ui.proposal.delete.confirm' javaScriptEscape='true' />",
    proposalDeleted: "<spring:message code='ui.proposal.deleted' javaScriptEscape='true' />",
    blockAdded: "<spring:message code='ui.block.added' javaScriptEscape='true' />",
    blockContentPrompt: "<spring:message code='ui.block.content.prompt' javaScriptEscape='true' />",
    blockUpdated: "<spring:message code='ui.block.updated' javaScriptEscape='true' />",
    blockDeleteConfirm: "<spring:message code='ui.block.delete.confirm' javaScriptEscape='true' />",
    blockDeleted: "<spring:message code='ui.block.deleted' javaScriptEscape='true' />",
    memoryTitlePrompt: "<spring:message code='ui.memory.title.prompt' javaScriptEscape='true' />",
    memoryFromTask: "<spring:message code='ui.memory.fromTask' javaScriptEscape='true' />",
    proposalApproved: "<spring:message code='ui.proposal.approved' javaScriptEscape='true' />",
    resourceSkill: "<spring:message code='ui.resource.skill' javaScriptEscape='true' />",
    resourceWorkflow: "<spring:message code='ui.resource.workflow' javaScriptEscape='true' />",
    resourceMemory: "<spring:message code='ui.resource.memory' javaScriptEscape='true' />",
    resourceTask: "<spring:message code='ui.resource.task' javaScriptEscape='true' />",
    serverCommunicationError: "<spring:message code='ui.server.communicationError' javaScriptEscape='true' />"
};
const { createApp } = Vue;

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
</script>
</body>
</html>
