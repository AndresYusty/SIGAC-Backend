package com.university.sigac.modules.repository.controller;

import com.university.sigac.modules.repository.dto.RepositoryDtos.DashboardDto;
import com.university.sigac.modules.repository.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Role-dependent indicators for the home screen. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboard;

    @GetMapping
    public DashboardDto get() {
        return dashboard.get();
    }
}
