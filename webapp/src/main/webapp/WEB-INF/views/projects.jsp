<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="${csrfToken}">
    <title>Agent Memory Hub</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/tabler/css/tabler.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hub.css">
</head>
<body>
<a class="visually-hidden-focusable skip-link" href="#main-content"><spring:message code='ui.skip.main' /></a>
<div class="page">
    <%@ include file="fragments/sidebar.jspf" %>
    <div class="page-wrapper">
        <%@ include file="fragments/topbar.jspf" %>
        <div id="hub-app" v-cloak data-context-path="${pageContext.request.contextPath}" data-user-id="${userId}" data-platform-role="${platformRole}">
                <div class="page-header d-print-none mb-4">
                    <div class="container-xl">
                    <div class="row g-2 align-items-center">
                        <div class="col">
                            <div class="page-pretitle"><spring:message code='ui.workspaces' /></div>
                            <h1 class="page-title"><spring:message code='ui.projectsKnowledge' /></h1>
                            <div class="text-secondary mt-1"><spring:message code='ui.projectsKnowledge.description' /></div>
                        </div>
                        <div class="col-auto ms-auto d-print-none">
                            <div class="d-flex flex-wrap gap-2">
                                <a v-if="platformRole === 'ADMIN'" class="btn btn-outline-primary" href="${pageContext.request.contextPath}/admin/users"><spring:message code='ui.users' /></a>
                                <button class="btn btn-primary" type="button" @click="createProject"><svg class="icon me-1" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#plus"></use></svg><spring:message code='ui.project.new' /></button>
                            </div>
                        </div>
                    </div>
                    </div>
                </div>

                <main id="main-content" class="page-body">
                <div class="container-xl">
                <div v-if="errorMessage" class="alert alert-important alert-danger alert-dismissible" role="alert">
                    <div class="alert-icon"><svg class="icon alert-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#alert-circle"></use></svg></div>
                    <div>{{ errorMessage }}</div>
                    <button type="button" class="btn-close" aria-label="<spring:message code='ui.alert.dismiss' />" @click="errorMessage = ''"></button>
                </div>
                <div v-if="noticeMessage" class="alert alert-important alert-success alert-dismissible" role="status" aria-live="polite">
                    <div class="alert-icon"><svg class="icon alert-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#circle-check"></use></svg></div>
                    <div>{{ noticeMessage }}</div>
                    <button type="button" class="btn-close" aria-label="<spring:message code='ui.alert.dismiss' />" @click="noticeMessage = ''"></button>
                </div>

                <div class="row row-cards align-items-start">
                    <aside class="col-xl-3 col-lg-4">
                        <div class="workspace-sidebar">
                            <section class="card project-list-card">
                                <div class="card-header"><h2 class="card-title mb-0"><spring:message code='ui.project.mine' /></h2></div>
                                <div class="list-group list-group-flush">
                                    <button v-for="project in projectList" :key="project.id" class="list-group-item list-group-item-action" :class="{ active: selectedProject && selectedProject.id === project.id }" type="button" @click="openProject(project.id)">
                                        <span class="d-block fw-semibold">{{ project.name }}</span>
                                        <small :class="selectedProject && selectedProject.id === project.id ? 'text-white-50' : 'text-body-secondary'">{{ project.enabled ? messages.active : messages.inactive }}</small>
                                    </button>
                                    <div v-if="projectList.length === 0" class="p-3 text-body-secondary"><spring:message code='ui.projects.empty' /></div>
                                </div>
                            </section>

                            <nav v-if="selectedProject" class="card mt-3" aria-label="<spring:message code='ui.workspace.navigation' />">
                                <div class="card-header"><h2 class="card-title mb-0"><spring:message code='ui.workspace.project' /></h2></div>
                                <div class="card-body p-2">
                                    <div class="nav nav-pills flex-column">
                                        <button class="nav-link text-start" :class="{ active: activeWorkspaceSection === 'overview' }" type="button" :aria-current="activeWorkspaceSection === 'overview' ? 'page' : null" @click="selectWorkspaceSection('overview')"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#layout-dashboard"></use></svg></span><span><spring:message code='ui.workspace.overview' /></span></button>
                                        <button class="nav-link text-start" :class="{ active: activeWorkspaceSection === 'settings' }" type="button" :aria-current="activeWorkspaceSection === 'settings' ? 'page' : null" @click="selectWorkspaceSection('settings')"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#settings"></use></svg></span><span><spring:message code='ui.settings' /></span></button>
                                        <button class="nav-link text-start" :class="{ active: activeWorkspaceSection === 'connections' }" type="button" :aria-current="activeWorkspaceSection === 'connections' ? 'page' : null" @click="selectWorkspaceSection('connections')"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#plug-connected"></use></svg></span><span><spring:message code='ui.connections' /></span></button>
                                        <button class="nav-link text-start" :class="{ active: activeWorkspaceSection === 'members' }" type="button" :aria-current="activeWorkspaceSection === 'members' ? 'page' : null" @click="selectWorkspaceSection('members')"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#users"></use></svg></span><span><spring:message code='ui.members' /></span></button>
                                    </div>
                                    <hr class="my-2">
                                    <p class="subheader px-3 mb-1"><spring:message code='ui.workspace.profile' /></p>
                                    <div class="nav nav-pills flex-column">
                                        <button class="nav-link text-start" :class="{ active: activeWorkspaceSection === 'profiles' }" type="button" :aria-current="activeWorkspaceSection === 'profiles' ? 'page' : null" @click="selectWorkspaceSection('profiles')"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#user-circle"></use></svg></span><span><spring:message code='ui.profile.list' /></span></button>
                                        <button v-for="resourceType in resourceTypeList" :key="resourceType" class="nav-link text-start" :class="{ active: activeWorkspaceSection === resourceType }" type="button" :disabled="!activeProfileId" :aria-current="activeWorkspaceSection === resourceType ? 'page' : null" @click="selectWorkspaceSection(resourceType)"><span class="nav-link-icon"><svg class="icon" aria-hidden="true"><use :href="'${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#' + (resourceType === 'SKILL' ? 'sparkles' : (resourceType === 'WORKFLOW' ? 'git-branch' : (resourceType === 'MEMORY' ? 'brain' : 'checklist')))"></use></svg></span><span>{{ resourceTypeLabel(resourceType) }}</span></button>
                                    </div>
                                </div>
                            </nav>

                        </div>
                    </aside>

                    <section class="col-xl-9 col-lg-8">
                        <div v-if="!selectedProject" class="empty">
                            <div class="empty-icon"><svg class="icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#brain"></use></svg></div>
                            <p class="empty-title"><spring:message code='ui.workspace.empty.title' /></p>
                            <p class="empty-subtitle text-secondary"><spring:message code='ui.workspace.empty.description' /></p>
                        </div>

                        <template v-if="selectedProject">
                                <section class="card mb-4">
                                    <div class="card-body">
                                        <div class="row align-items-center g-3">
                                        <div class="col">
                                            <div class="page-pretitle"><spring:message code='ui.project.label' /></div>
                                            <h2 class="card-title h2 mb-1">{{ selectedProject.name }}</h2>
                                            <div class="text-secondary">{{ selectedProject.description || messages.descriptionEmpty }}</div>
                                            <span class="badge mt-2" :class="selectedProject.enabled ? 'text-bg-success' : 'text-bg-secondary'">{{ selectedProject.enabled ? messages.active : messages.inactive }}</span>
                                        </div>
                                        <div class="col-12 col-lg-auto">
                                            <div class="d-flex flex-wrap gap-2">
                                                <button v-if="canEditProject" class="btn btn-outline-secondary" type="button" @click="editProject"><spring:message code='ui.project.edit' /></button>
                                                <button v-if="currentProjectRole === 'OWNER'" class="btn btn-outline-danger" type="button" @click="deleteProject"><spring:message code='ui.project.delete' /></button>
                                            </div>
                                            <div class="mt-3">
                                                <label class="form-label" for="active-profile"><spring:message code='ui.profile.selected' /></label>
                                                <select id="active-profile" v-model="activeProfileId" class="form-select" :disabled="currentProjectRole !== 'OWNER'" @change="selectActiveProfile">
                                                    <option value="" :disabled="Boolean(activeProfileId)"><spring:message code='ui.profile.noneSelected' /></option>
                                                    <option v-for="profile in profileList" :key="profile.id" :value="profile.id">{{ profile.name }}</option>
                                                </select>
                                            </div>
                                        </div>
                                        </div>
                                    </div>
                                    <div v-if="activeProfile" class="card-footer d-flex flex-wrap gap-2">
                                        <button v-if="canEditProject" class="btn btn-sm btn-outline-secondary" type="button" @click="editProfile"><spring:message code='ui.profile.edit' /></button>
                                        <button v-if="currentProjectRole === 'OWNER'" class="btn btn-sm btn-outline-danger" type="button" @click="deleteProfile"><spring:message code='ui.profile.delete' /></button>
                                    </div>
                                </section>

                            <div class="workspace-content">

                                <section v-if="activeWorkspaceSection === 'overview'" class="card overview-section">
                                    <div class="card-body">
                                    <div class="mb-4">
                                        <p class="text-primary fw-semibold mb-1"><spring:message code='ui.project.label' /></p>
                                        <h3 class="h3 mb-2"><spring:message code='ui.workspace.overview.title' /></h3>
                                        <p class="text-body-secondary mb-0"><spring:message code='ui.workspace.overview.description' /></p>
                                    </div>
                                    <div class="row row-cards mb-4">
                                        <div class="col-md-4"><div class="card card-sm"><div class="card-body"><div class="subheader"><spring:message code='ui.workspace.overview.profiles' /></div><div class="h1 mb-0">{{ profileList.length }}</div></div></div></div>
                                        <div class="col-md-4"><div class="card card-sm"><div class="card-body"><div class="subheader"><spring:message code='ui.workspace.overview.members' /></div><div class="h1 mb-0">{{ memberList.length }}</div></div></div></div>
                                        <div class="col-md-4"><div class="card card-sm"><div class="card-body"><div class="subheader"><spring:message code='ui.workspace.overview.connections' /></div><div class="h1 mb-0">{{ enabledConnectionCount }}</div></div></div></div>
                                    </div>
                                    <div class="d-flex flex-wrap gap-2">
                                        <button class="btn btn-outline-primary" type="button" @click="selectWorkspaceSection('profiles')"><spring:message code='ui.profile.list' /></button>
                                        <button class="btn btn-outline-primary" type="button" @click="selectWorkspaceSection('SKILL')" :disabled="!activeProfileId"><spring:message code='ui.resource.skill' /></button>
                                        <button class="btn btn-outline-primary" type="button" @click="selectWorkspaceSection('TASK')" :disabled="!activeProfileId"><spring:message code='ui.resource.task' /></button>
                                    </div>
                                    </div>
                                </section>

                                <section v-if="activeWorkspaceSection === 'profiles'" class="card workspace-card profiles-section p-4">
                                    <div class="section-heading d-flex flex-wrap justify-content-between align-items-center gap-3 mb-3">
                                        <div>
                                            <p class="text-primary fw-semibold mb-1"><spring:message code='ui.workspace.profile' /></p>
                                            <h3 class="h4 mb-0"><spring:message code='ui.profile.list' /></h3>
                                        </div>
                                        <button v-if="canEditProject" class="btn btn-primary" type="button" @click="createProfile"><spring:message code='ui.profile.new' /></button>
                                    </div>
                                    <div v-if="profileList.length" class="list-group list-group-flush">
                                        <article v-for="profile in profileList" :key="profile.id" class="list-group-item d-flex flex-wrap justify-content-between align-items-center gap-3" :class="{ 'bg-blue-lt': profile.id === activeProfileId }">
                                            <div>
                                                <h4 class="h6 mb-1">{{ profile.name }}</h4>
                                                <p class="small text-body-secondary mb-0">{{ profile.description || messages.descriptionEmpty }}</p>
                                            </div>
                                            <div class="d-flex flex-wrap gap-2 align-items-center">
                                                <span v-if="profile.id === activeProfileId" class="badge rounded-pill text-bg-primary"><spring:message code='ui.profile.active' /></span>
                                                <span class="badge rounded-pill" :class="profile.enabled ? 'text-bg-success' : 'text-bg-secondary'">{{ profile.enabled ? messages.active : messages.inactive }}</span>
                                            </div>
                                        </article>
                                    </div>
                                    <p v-else class="text-body-secondary py-4 mb-0"><spring:message code='ui.profiles.empty' /></p>
                                </section>
                                        <div v-if="activeWorkspaceSection === 'settings'" class="card workspace-card h-100">
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
                                                    <div class="col-12 form-check form-switch"><input id="pre-tool-context" v-model="settingsForm.preToolUseContextEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="pre-tool-context"><spring:message code='ui.context.preToolUse' /></label></div>
                                                    <div class="col-12 form-check form-switch"><input id="summary-enabled" v-model="settingsForm.automaticSummaryEnabled" class="form-check-input" type="checkbox" :disabled="currentProjectRole !== 'OWNER'"><label class="form-check-label" for="summary-enabled"><spring:message code='ui.summary.automatic' /></label></div>
                                                    <div class="col-md-6"><label class="form-label" for="creation-mode"><spring:message code='ui.task.creation' /></label><select id="creation-mode" v-model="settingsForm.taskCreationMode" class="form-select" :disabled="currentProjectRole !== 'OWNER'"><option value="MANUAL"><spring:message code='ui.manual' /></option><option value="SUGGEST"><spring:message code='ui.suggest' /></option><option value="AUTOMATIC"><spring:message code='ui.automatic' /></option></select></div>
                                                    <div class="col-md-3"><label class="form-label" for="budget"><spring:message code='ui.budget' /></label><input id="budget" v-model.number="settingsForm.contextBudgetTokens" class="form-control" type="number" min="100" max="100000" :disabled="currentProjectRole !== 'OWNER'"></div>
                                                    <div class="col-md-3"><label class="form-label" for="quota"><spring:message code='ui.permanentQuota' /></label><input id="quota" v-model.number="settingsForm.permanentQuotaPercent" class="form-control" type="number" min="0" max="100" :disabled="currentProjectRole !== 'OWNER'"></div>
                                                    <div v-if="currentProjectRole === 'OWNER'" class="col-12 mt-2"><button class="btn btn-sm btn-primary" type="submit"><spring:message code='ui.settings.save' /></button></div>
                                                </form>
                                            </div>
                                        </div>
                                        <div v-if="activeWorkspaceSection === 'connections'" class="card workspace-card h-100">
                                            <div class="card-body">
                                                <p class="text-primary fw-semibold mb-1"><spring:message code='ui.connections' /></p><h3 class="h6 mb-3"><spring:message code='ui.connections.servers' /></h3>
                                                <div v-for="connectionType in ['CONTEXT', 'TASKS']" :key="connectionType" class="d-flex justify-content-between align-items-center border-bottom py-2">
                                                    <div><span class="fw-semibold">{{ connectionForType(connectionType) ? connectionForType(connectionType).name : (connectionType === 'CONTEXT' ? messages.context : messages.tasks) }}</span><small class="d-block text-body-secondary">{{ connectionForType(connectionType) ? (connectionForType(connectionType).enabled ? messages.enabled : messages.disabled) : messages.connectionUnconfigured }}</small></div>
                                                    <div v-if="currentProjectRole === 'OWNER'" class="d-flex gap-2"><button v-if="connectionForType(connectionType)" class="btn btn-sm btn-outline-secondary" type="button" @click="renameConnection(connectionForType(connectionType))"><spring:message code='ui.edit' /></button><button v-if="connectionForType(connectionType)" class="btn btn-sm btn-outline-danger" type="button" @click="deleteConnection(connectionForType(connectionType))"><spring:message code='ui.delete' /></button><button class="btn btn-sm" :class="connectionForType(connectionType) && connectionForType(connectionType).enabled ? 'btn-outline-danger' : 'btn-outline-success'" type="button" @click="toggleConnection(connectionType)">{{ connectionForType(connectionType) && connectionForType(connectionType).enabled ? messages.disable : messages.enable }}</button></div>
                                                </div>
                                            </div>
                                        </div>
                                        <div v-if="activeWorkspaceSection === 'members'" class="card workspace-card h-100">
                                            <div class="card-body">
                                                <p class="text-primary fw-semibold mb-1"><spring:message code='ui.members' /></p><h3 class="h6 mb-3"><spring:message code='ui.members.access' /></h3>
                                                <div v-for="member in memberList" :key="member.userId" class="d-flex justify-content-between align-items-start border-bottom py-2">
                                                    <div><span class="d-block fw-semibold">{{ member.fullName }}</span><small class="text-body-secondary">{{ member.email }}</small><select v-if="currentProjectRole === 'OWNER' && member.projectRole !== 'OWNER'" v-model="member.projectRole" class="form-select form-select-sm mt-1"><option value="READER"><spring:message code='ui.role.reader' /></option><option value="EDITOR"><spring:message code='ui.role.editor' /></option></select><small v-else class="d-block text-body-secondary">{{ roleLabel(member.projectRole) }}</small></div>
                                                    <div v-if="currentProjectRole === 'OWNER' && member.projectRole !== 'OWNER'" class="d-flex flex-column gap-1"><button class="btn btn-sm btn-link p-0" type="button" @click="updateMember(member)"><spring:message code='ui.member.saveRole' /></button><button class="btn btn-sm btn-link text-danger p-0" type="button" @click="removeMember(member)"><spring:message code='ui.remove' /></button></div>
                                                </div>
                                                <button v-if="currentProjectRole === 'OWNER'" class="btn btn-sm btn-outline-primary mt-3 w-100" type="button" @click="addMember"><spring:message code='ui.member.add' /></button>
                                            </div>
                                        </div>
                                <section v-if="isResourceSection" class="card workspace-card resource-section">
                                    <div class="card-header bg-white border-bottom-0 pt-4 px-4">
                                        <div>
                                            <p class="text-primary fw-semibold mb-1"><spring:message code='ui.profile.active' /></p>
                                            <h3 class="h5 mb-0">{{ activeProfile && activeProfile.name }}</h3>
                                        </div>
                                    </div>
                                    <div class="card-body">
                                        <div class="d-flex justify-content-between align-items-center gap-2 mb-3">
                                            <h4 class="h6 mb-0">{{ resourceTypeLabel(activeResourceType) }}</h4>
                                            <button v-if="canEditProject" class="btn btn-sm btn-primary" type="button" @click="createResource"><spring:message code='ui.add' /></button>
                                        </div>

                                        <div v-if="resourceList.length === 0" class="text-body-secondary py-4 text-center"><spring:message code='ui.resources.empty' /></div>
                                        <div class="row g-3">
                                            <article v-for="resource in resourceList" :key="resource.id" class="col-12">
                                                <div class="card resource-card bg-body-tertiary border-0">
                                                    <div class="card-body">
                                                        <div class="d-flex justify-content-between gap-3">
                                                            <div>
                                                                <h5 class="h6">{{ resource.title || resource.name }}</h5>
                                                                <p v-if="resource.description" class="text-body-secondary">{{ resource.description }}</p>
                                                                <p v-if="activeResourceType === 'TASK' && resource.summary" class="small"><span class="fw-semibold"><spring:message code='ui.summary' />:</span> {{ resource.summary }}</p>
                                                                <div class="d-flex flex-wrap gap-1 mb-2"><span v-for="tag in resource.tags || []" :key="tag" class="badge text-bg-secondary">{{ tag }}</span><span v-if="resource.applicationMode" class="badge text-bg-blue-lt">{{ resource.applicationMode === 'ALWAYS' ? messages.modeAlways : messages.modeContextual }}</span><span v-if="resource.enabled !== undefined" class="badge" :class="resource.enabled ? 'text-bg-success' : 'text-bg-secondary'">{{ resource.enabled ? messages.enabled : messages.disabled }}</span></div>
                                                            </div>
                                                            <div v-if="canEditProject" class="d-flex gap-2">
                                                                <button class="btn btn-sm btn-outline-primary align-self-start" type="button" @click="editResource(resource)"><spring:message code='ui.edit' /></button>
                                                                <button class="btn btn-sm btn-outline-danger align-self-start" type="button" @click="deleteResource(resource)"><spring:message code='ui.delete' /></button>
                                                            </div>
                                                        </div>
                                                        <p v-if="resource.content || resource.instructions" class="content-preview mb-0">{{ resource.content || resource.instructions }}</p>
                                                        <div v-if="activeResourceType === 'TASK'" class="mt-3">
                                                            <button class="btn btn-sm btn-outline-secondary me-2" type="button" @click="loadTaskBlocks(resource)"><spring:message code='ui.task.blocks' /></button>
                                                            <button class="btn btn-sm btn-outline-primary" type="button" @click="loadProposals(resource)"><spring:message code='ui.proposal.view' /></button>
                                                            <div v-if="blockTaskId === resource.id" class="mt-3 p-3 bg-white border rounded">
                                                                <h6 class="mb-3"><spring:message code='ui.block.temporary' /></h6>
                                                                <div v-for="block in blockList" :key="block.id" class="border-bottom py-2">
                                                                    <div class="d-flex justify-content-between gap-3"><p class="content-preview mb-2">{{ block.content }}</p><div v-if="canEditProject" class="text-nowrap"><button class="btn btn-sm btn-link" type="button" @click="editTaskBlock(resource, block)"><spring:message code='ui.edit' /></button><button class="btn btn-sm btn-link text-danger" type="button" @click="deleteTaskBlock(resource, block)"><spring:message code='ui.delete' /></button></div></div>
                                                                </div>
                                                                <p v-if="blockList.length === 0" class="small text-body-secondary"><spring:message code='ui.block.empty' /></p>
                                                                <button v-if="canEditProject" class="btn btn-sm btn-primary mt-2" type="button" @click="createTaskBlock(resource)"><spring:message code='ui.block.add' /></button>
                                                            </div>
                                                            <div v-if="proposalTaskId === resource.id" class="mt-3">
                                                                <div class="d-flex justify-content-end mb-2"><button v-if="canEditProject" class="btn btn-sm btn-primary" type="button" @click="createProposal(resource)"><spring:message code='ui.proposal.new' /></button></div>
                                                                <div v-for="proposal in proposalList" :key="proposal.id" class="border rounded p-3 mb-2 bg-white">
                                                                    <div class="d-flex justify-content-between gap-3">
                                                                        <div><p class="fw-semibold mb-1">{{ proposal.reason }}</p><p class="content-preview mb-2">{{ proposal.proposedContent }}</p><div class="d-flex flex-wrap gap-1"><span v-for="tag in proposal.tags || []" :key="tag" class="badge text-bg-secondary">{{ tag }}</span><span class="badge" :class="proposal.processed ? 'text-bg-secondary' : 'text-bg-success'">{{ proposal.processed ? messages.processed : messages.pending }}</span><span v-if="proposal.resultMemoryId" class="badge text-bg-blue-lt">{{ messages.resultMemory }}: {{ proposal.resultMemoryId }}</span></div></div>
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

                            </div>
                        </template>
                    </section>
                </div>
                </div>
            </main>
                <button id="hub-dialog-trigger" class="d-none" type="button" data-bs-toggle="modal" data-bs-target="#hub-dialog" tabindex="-1" aria-hidden="true"></button>
                <div id="hub-dialog" class="modal modal-blur fade" tabindex="-1" aria-hidden="true">
                    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" :class="{ 'modal-lg': dialogFields.length > 3 }">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">{{ dialogTitle }}</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="<spring:message code='ui.dialog.close' />" @click="cancelDialog"></button>
                            </div>
                            <form v-if="dialogMode === 'form'" @submit.prevent="submitDialog">
                                <div class="modal-body">
                                    <div v-if="dialogError" class="alert alert-danger" role="alert">{{ dialogError }}</div>
                                    <div v-for="field in dialogFields" :key="field.key" class="mb-3">
                                        <label v-if="field.type !== 'checkbox'" class="form-label" :for="'hub-dialog-field-' + field.key">{{ field.label }}</label>
                                        <textarea v-if="field.multiline" :id="'hub-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-control" rows="4" :required="field.required"></textarea>
                                        <select v-else-if="field.options" :id="'hub-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-select" :required="field.required"><option v-for="option in field.options" :key="option.value" :value="option.value">{{ option.label }}</option></select>
                                        <label v-else-if="field.type === 'checkbox'" class="form-check form-switch"><input :id="'hub-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-check-input" type="checkbox"><span class="form-check-label">{{ field.label }}</span></label>
                                        <input v-else :id="'hub-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-control" :type="field.inputType || 'text'" :required="field.required" :minlength="field.minlength">
                                    </div>
                                </div>
                                <div class="modal-footer">
                                    <button class="btn btn-link link-secondary" type="button" data-bs-dismiss="modal" @click="cancelDialog"><spring:message code='ui.cancel' /></button>
                                    <button class="btn" :class="dialogDanger ? 'btn-danger' : 'btn-primary'" type="submit">{{ dialogSubmitLabel }}</button>
                                </div>
                            </form>
                            <template v-else>
                                <div class="modal-body">{{ dialogMessage }}</div>
                                <div class="modal-footer">
                                    <button class="btn btn-link link-secondary" type="button" data-bs-dismiss="modal" @click="cancelDialog"><spring:message code='ui.cancel' /></button>
                                    <button class="btn" :class="dialogDanger ? 'btn-danger' : 'btn-primary'" type="button" data-bs-dismiss="modal" @click="confirmDialog">{{ dialogSubmitLabel }}</button>
                                </div>
                            </template>
                            <button id="hub-dialog-close" class="d-none" type="button" data-bs-dismiss="modal" tabindex="-1" aria-hidden="true"></button>
                        </div>
                    </div>
                </div>
        </div>
        <%@ include file="fragments/footer.jspf" %>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/vendor/tabler/js/tabler.min.js"></script>
<script src="${pageContext.request.contextPath}/webjars/vue/3.5.22/dist/vue.global.prod.js"></script>
<script type="module" src="${pageContext.request.contextPath}/assets/js/pages/projects.js"></script>
</body>
</html>
