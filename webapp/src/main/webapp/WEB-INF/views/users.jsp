<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="csrf-token" content="${csrfToken}">
    <title><spring:message code='ui.users' /> · Agent Memory Hub</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
</head>
<body class="bg-body-tertiary">
<nav class="navbar navbar-dark" style="background:#10213d">
    <div class="container-fluid px-lg-5">
        <a class="navbar-brand fw-semibold" href="${pageContext.request.contextPath}/dashboard">Agent Memory Hub</a>
        <form method="post" action="${pageContext.request.contextPath}/logout" class="m-0">
            <input type="hidden" name="csrfToken" value="${csrfToken}">
            <button class="btn btn-outline-light btn-sm" type="submit"><spring:message code='ui.logout' /></button>
        </form>
    </div>
</nav>
<main id="users-app" class="container-fluid px-lg-5 py-4" data-context-path="${pageContext.request.contextPath}">
    <div class="d-flex justify-content-between align-items-end mb-4">
        <div><p class="text-primary fw-semibold mb-1"><spring:message code='ui.admin' /></p><h1 class="h2 mb-0"><spring:message code='ui.users' /></h1></div>
        <a class="btn btn-outline-secondary" href="${pageContext.request.contextPath}/dashboard"><spring:message code='ui.users.back' /></a>
    </div>
    <div v-if="errorMessage" class="alert alert-danger">{{ errorMessage }}</div>
    <div v-if="noticeMessage" class="alert alert-success">{{ noticeMessage }}</div>
    <section class="card border-0 shadow-sm mb-4">
        <div class="card-body">
            <h2 class="h5"><spring:message code='ui.user.create' /></h2>
            <form class="row g-3" @submit.prevent="createUser">
                <div class="col-lg-3"><label class="form-label" for="full-name"><spring:message code='ui.user.fullName' /></label><input id="full-name" v-model="createForm.fullName" class="form-control" required></div>
                <div class="col-lg-3"><label class="form-label" for="user-email"><spring:message code='ui.user.email' /></label><input id="user-email" v-model="createForm.email" class="form-control" type="email" required></div>
                <div class="col-lg-3"><label class="form-label" for="user-password"><spring:message code='ui.user.initialPassword' /></label><input id="user-password" v-model="createForm.password" class="form-control" type="password" minlength="12" required></div>
                <div class="col-lg-2"><label class="form-label" for="user-role"><spring:message code='ui.role' /></label><select id="user-role" v-model="createForm.platformRole" class="form-select"><option value="USER"><spring:message code='ui.role.user' /></option><option value="ADMIN"><spring:message code='ui.role.admin' /></option></select></div>
                <div class="col-lg-1 d-flex align-items-end"><button class="btn btn-primary w-100" type="submit"><spring:message code='ui.create' /></button></div>
            </form>
        </div>
    </section>
    <section class="card border-0 shadow-sm">
        <div class="card-header bg-white py-3"><h2 class="h5 mb-0"><spring:message code='ui.user.accounts' /></h2></div>
        <div class="table-responsive">
            <table class="table align-middle mb-0">
                <thead><tr><th><spring:message code='ui.name' /></th><th><spring:message code='ui.user.email' /></th><th><spring:message code='ui.role' /></th><th><spring:message code='ui.status' /></th><th></th></tr></thead>
                <tbody>
                    <tr v-for="user in userList" :key="user.id">
                        <td><input v-model="user.fullName" class="form-control form-control-sm"></td>
                        <td><input v-model="user.email" class="form-control form-control-sm" type="email"></td>
                        <td><select v-model="user.platformRole" class="form-select form-select-sm"><option value="USER"><spring:message code='ui.role.user' /></option><option value="ADMIN"><spring:message code='ui.role.admin' /></option></select></td>
                        <td><select v-model="user.enabled" class="form-select form-select-sm"><option :value="true"><spring:message code='ui.active' /></option><option :value="false"><spring:message code='ui.user.disabled' /></option></select></td>
                        <td class="text-end text-nowrap"><button class="btn btn-sm btn-primary me-2" type="button" @click="saveUser(user)"><spring:message code='ui.save' /></button><button class="btn btn-sm btn-outline-danger" type="button" @click="deleteUser(user)"><spring:message code='ui.delete' /></button></td>
                    </tr>
                    <tr v-if="userList.length === 0"><td colspan="5" class="text-center text-body-secondary py-4"><spring:message code='ui.users.empty' /></td></tr>
                </tbody>
            </table>
        </div>
    </section>
</main>
<script src="${pageContext.request.contextPath}/webjars/vue/3.5.22/dist/vue.global.prod.js"></script>
<script type="module" src="${pageContext.request.contextPath}/assets/js/pages/users.js"></script>
</body>
</html>
