package com.pyin.plugin.system.user.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.pyin.plugin.system.api.model.SystemUserAuthInfo;
import com.pyin.plugin.system.api.model.SystemUserView;
import com.pyin.plugin.system.user.entity.UserEntity;
import com.pyin.plugin.system.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SystemUserPublicServiceImplTest {

    @Mock
    private UserService userService;

    private SystemUserPublicServiceImpl publicService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        publicService = new SystemUserPublicServiceImpl(userService);
    }

    @Test
    void shouldExposeAuthenticationInfoByUsername() {
        when(userService.findByUsername("admin")).thenReturn(user(1L, "admin", "Pyin Admin", UserService.STATUS_ENABLED));

        SystemUserAuthInfo authInfo = publicService.findAuthInfoByUsername("admin");

        assertThat(authInfo.id()).isEqualTo(1L);
        assertThat(authInfo.username()).isEqualTo("admin");
        assertThat(authInfo.displayName()).isEqualTo("Pyin Admin");
        assertThat(authInfo.passwordHash()).isEqualTo("hashed-password");
        assertThat(authInfo.enabled()).isTrue();
    }

    @Test
    void shouldExposeDisabledUserView() {
        when(userService.findById(2L)).thenReturn(user(2L, "disabled", "Disabled User", UserService.STATUS_DISABLED));

        SystemUserView user = publicService.findUserById(2L);

        assertThat(user.id()).isEqualTo(2L);
        assertThat(user.enabled()).isFalse();
    }

    private UserEntity user(Long id, String username, String displayName, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash("hashed-password");
        user.setStatus(status);
        return user;
    }
}
