package com.university.sigac.modules.users.controller;

import com.university.sigac.modules.users.dto.AuthDtos.MessageResponse;
import com.university.sigac.modules.users.dto.UserDtos.ChangePasswordRequest;
import com.university.sigac.modules.users.dto.UserDtos.UserCreateRequest;
import com.university.sigac.modules.users.dto.UserDtos.UserDto;
import com.university.sigac.modules.users.dto.UserDtos.UserUpdateRequest;
import com.university.sigac.modules.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @GetMapping("/me")
    public UserDto myProfile() {
        return service.currentProfile();
    }

    @PutMapping("/me/password")
    public MessageResponse changeMyPassword(@Valid @RequestBody ChangePasswordRequest req) {
        service.changeOwnPassword(req);
        return new MessageResponse("Password updated successfully");
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public Page<UserDto> list(@RequestParam(required = false) String q,
                              @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return service.list(q, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UserDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UserDto create(@Valid @RequestBody UserCreateRequest req) {
        return service.create(req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UserDto update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest req) {
        return service.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DEV')")
    public UserDto changeStatus(@PathVariable Long id, @RequestParam boolean active) {
        return service.changeStatus(id, active);
    }
}
