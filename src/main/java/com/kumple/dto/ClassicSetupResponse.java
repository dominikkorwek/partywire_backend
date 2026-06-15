package com.kumple.dto;

import java.util.List;

public record ClassicSetupResponse(
        String questionContent,
        List<ClassicOptionResponse> options
) {}
