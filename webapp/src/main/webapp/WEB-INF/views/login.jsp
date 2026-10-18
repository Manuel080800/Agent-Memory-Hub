<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><spring:message code='ui.login.title' /></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/vendor/tabler/css/tabler.min.css">
    <link rel="stylesheet" href="${pageContext.request.contextPath}/assets/css/hub.css">
</head>
<body>
<main class="page page-center" id="main-content">
    <div class="container container-tight py-4">
        <div class="text-center mb-4">
            <a class="hub-login-brand" href="${pageContext.request.contextPath}/login" aria-label="Agent Memory Hub">
                <span class="avatar avatar-lg bg-blue-lt text-blue fw-bold" aria-hidden="true">AM</span>
                <span>Agent Memory Hub</span>
            </a>
        </div>
        <section class="card card-md">
            <div class="card-body">
                <h1 class="h2 text-center mb-4"><spring:message code='ui.login.heading' /></h1>
                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-important alert-danger alert-dismissible" role="alert">
                        <div class="alert-icon"><svg class="icon alert-icon" aria-hidden="true"><use href="${pageContext.request.contextPath}/assets/vendor/tabler/icons.svg#alert-circle"></use></svg></div>
                        <div><c:out value="${errorMessage}" /></div>
                        <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="<spring:message code='ui.alert.dismiss' />"></button>
                    </div>
                </c:if>
                <form method="post" action="${pageContext.request.contextPath}/login">
                    <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}' />">
                    <div class="mb-3">
                        <label class="form-label" for="email"><spring:message code='ui.login.email' /></label>
                        <input class="form-control" id="email" name="email" type="email" autocomplete="username" required>
                    </div>
                    <div class="mb-4">
                        <label class="form-label" for="password"><spring:message code='ui.login.password' /></label>
                        <input class="form-control" id="password" name="password" type="password" autocomplete="current-password" required>
                    </div>
                    <div class="form-footer">
                        <button class="btn btn-primary w-100" type="submit"><spring:message code='ui.login.submit' /></button>
                    </div>
                </form>
            </div>
        </section>
    </div>
</main>
<script src="${pageContext.request.contextPath}/assets/vendor/tabler/js/tabler.min.js"></script>
</body>
</html>
