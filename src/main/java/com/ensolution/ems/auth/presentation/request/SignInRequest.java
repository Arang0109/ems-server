package com.ensolution.ems.auth.presentation.request;

public record SignInRequest(
        String username,
        String password
) {}