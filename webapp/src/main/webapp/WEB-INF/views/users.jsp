<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="${csrfToken}">
    <title><spring:message code='ui.users' /> · Agent Memory Hub</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/tabler/css/tabler.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hub.css">
</head>
<body>
<a class="visually-hidden-focusable skip-link" href="#main-content"><spring:message code='ui.skip.main' /></a>
<div class="page">
    <%@ include file="fragments/sidebar.jspf" %>
    <div class="page-wrapper">
        <%@ include file="fragments/topbar.jspf" %>
        <div id="users-app" v-cloak data-context-path="${pageContext.request.contextPath}">
                <div class="page-header d-print-none mb-4">
                    <div class="container-xl">
                    <div class="row align-items-center g-2">
                        <div class="col">
                            <div class="page-pretitle"><spring:message code='ui.admin' /></div>
                            <h1 class="page-title"><spring:message code='ui.users' /></h1>
                        </div>
                        <div class="col-auto ms-auto d-print-none">
                            <div class="d-flex gap-2"><button class="btn btn-primary" type="button" @click="createUser"><svg class="icon me-1" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#plus"></use></svg><spring:message code='ui.user.create' /></button><a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/dashboard"><spring:message code='ui.users.back' /></a></div>
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
                <section class="card users-table-card">
                    <div class="card-header"><h2 class="card-title mb-0"><spring:message code='ui.user.accounts' /></h2></div>
                    <div class="table-responsive">
                        <table class="table table-vcenter card-table">
                            <thead><tr><th><spring:message code='ui.name' /></th><th><spring:message code='ui.user.email' /></th><th><spring:message code='ui.role' /></th><th><spring:message code='ui.status' /></th><th><spring:message code='ui.user.resetPassword' /></th><th></th></tr></thead>
                            <tbody>
                                <tr v-for="user in userList" :key="user.id">
                                    <td><input v-model="user.fullName" class="form-control form-control-sm"></td>
                                    <td><input v-model="user.email" class="form-control form-control-sm" type="email"></td>
                                    <td><select v-model="user.platformRole" class="form-select form-select-sm"><option value="USER"><spring:message code='ui.role.user' /></option><option value="ADMIN"><spring:message code='ui.role.admin' /></option></select></td>
                                    <td><select v-model="user.enabled" class="form-select form-select-sm"><option :value="true"><spring:message code='ui.active' /></option><option :value="false"><spring:message code='ui.user.disabled' /></option></select></td>
                                    <td><input v-model="user.passwordDraft" class="form-control form-control-sm" type="password" minlength="12" autocomplete="new-password" placeholder="<spring:message code='ui.user.resetPassword' />"></td>
                                    <td class="text-end text-nowrap"><button class="btn btn-sm btn-primary me-2" type="button" @click="saveUser(user)"><spring:message code='ui.save' /></button><button class="btn btn-sm btn-outline-danger" type="button" @click="deleteUser(user)"><spring:message code='ui.delete' /></button></td>
                                </tr>
                                <tr v-if="userList.length === 0"><td colspan="6" class="text-center text-body-secondary py-4"><spring:message code='ui.users.empty' /></td></tr>
                            </tbody>
                        </table>
                    </div>
                </section>
                </div>
            </main>
            <button id="users-dialog-trigger" class="d-none" type="button" data-bs-toggle="modal" data-bs-target="#users-dialog" tabindex="-1" aria-hidden="true"></button>
            <div id="users-dialog" class="modal modal-blur fade" tabindex="-1" aria-hidden="true">
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
                                    <label v-if="field.type !== 'checkbox'" class="form-label" :for="'users-dialog-field-' + field.key">{{ field.label }}</label>
                                    <select v-if="field.options" :id="'users-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-select" :required="field.required"><option v-for="option in field.options" :key="option.value" :value="option.value">{{ option.label }}</option></select>
                                    <label v-else-if="field.type === 'checkbox'" class="form-check form-switch"><input :id="'users-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-check-input" type="checkbox"><span class="form-check-label">{{ field.label }}</span></label>
                                    <input v-else :id="'users-dialog-field-' + field.key" v-model="dialogValues[field.key]" class="form-control" :type="field.inputType || 'text'" :required="field.required" :minlength="field.minlength" :autocomplete="field.autocomplete">
                                </div>
                            </div>
                            <div class="modal-footer">
                                <button class="btn btn-link link-secondary" type="button" data-bs-dismiss="modal" @click="cancelDialog"><spring:message code='ui.cancel' /></button>
                                <button class="btn btn-primary" type="submit">{{ dialogSubmitLabel }}</button>
                            </div>
                        </form>
                        <template v-else>
                            <div class="modal-body">{{ dialogMessage }}</div>
                            <div class="modal-footer">
                                <button class="btn btn-link link-secondary" type="button" data-bs-dismiss="modal" @click="cancelDialog"><spring:message code='ui.cancel' /></button>
                                <button class="btn btn-danger" type="button" data-bs-dismiss="modal" @click="confirmDialog">{{ dialogSubmitLabel }}</button>
                            </div>
                        </template>
                        <button id="users-dialog-close" class="d-none" type="button" data-bs-dismiss="modal" tabindex="-1" aria-hidden="true"></button>
                    </div>
                </div>
            </div>
        </div>
        <%@ include file="fragments/footer.jspf" %>
    </div>
</div>
<script src="${pageContext.request.contextPath}/assets/vendor/tabler/js/tabler.min.js"></script>
<script src="${pageContext.request.contextPath}/webjars/vue/3.5.22/dist/vue.global.prod.js"></script>
<script type="module" src="${pageContext.request.contextPath}/assets/js/pages/users.js"></script>
</body>
</html>
