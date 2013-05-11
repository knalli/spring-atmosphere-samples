<%@ include file="/WEB-INF/views/includes/taglibs.jsp"%>

<!DOCTYPE HTML>
<html>
	<head>
		<title>Welcome to Spring Web MVC - Atmosphere Sample</title>
		<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />

		<link rel="stylesheet" href="<c:url value='/css/blueprint/screen.css'/>" type="text/css" media="screen, projection">
		<link rel="stylesheet" href="<c:url value='/css/blueprint/print.css'/>" type="text/css" media="print">
		<!--[if lt IE 8]>
			<link rel="stylesheet" href="/css/blueprint/ie.css" type="text/css" media="screen, projection">
		<![endif]-->

		<link rel="stylesheet" href="<c:url value='/css/main.css'/>" type="text/css">

	</head>
	<body>
        <header>
            NO LOGIN
            | <label for="loggedIn">Logged in: <input id="loggedIn" type="checkbox" disabled <sec:authorize access="isFullyAuthenticated()">checked="checked"</sec:authorize></label>
            | <sec:authorize access="isFullyAuthenticated()"><a href="/doLogout">Logout</a></sec:authorize>
        </header>
		<form method="POST" action="doLogin">
            <input id="username" name="username">
            <input id="password" name="password" type="password">
            <button type="submit">Login</button>
		</form>
	</body>
</html>
