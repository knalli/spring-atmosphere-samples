package org.springframework.mvc.samples.atmosphere.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Scope(value="session",proxyMode = ScopedProxyMode.TARGET_CLASS)
public class Session {

	private String username;

	@PostConstruct
	public void init() {
		setUsername(SecurityContextHolder.getContext().getAuthentication().getName());
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
