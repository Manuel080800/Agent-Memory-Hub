<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="${language}">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><spring:message code='ui.login.title' /></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/webjars/bootstrap/5.3.8/css/bootstrap.min.css">
</head>
<body class="bg-body-tertiary">
<main class="container min-vh-100 d-flex align-items-center justify-content-center">
    <section class="card shadow-sm border-0" style="max-width: 28rem; width: 100%;">
        <div class="card-body p-4 p-md-5">
            <p class="text-primary fw-semibold mb-2">Agent Memory Hub</p>
            <h1 class="h3 mb-4"><spring:message code='ui.login.heading' /></h1>
            <c:if test="${not empty errorMessage}">
                <div class="alert alert-danger" role="alert"><c:out value="${errorMessage}" /></div>
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
                <button class="btn btn-primary w-100" type="submit"><spring:message code='ui.login.submit' /></button>
            </form>
        </div>
    </section>
</main>
</body>
</html>
