package com.fadikazoura.nextrep.account;

import com.fadikazoura.nextrep.auth.TokenResponse;
import com.fadikazoura.nextrep.user.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PutMapping("/account")
    public UserResponse updateAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.updateAccount(userId(jwt), request);
    }

    @PutMapping("/password")
    public TokenResponse changePassword(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ChangePasswordRequest request) {
        return accountService.changePassword(userId(jwt), request);
    }

    /** Logs out every device, this one included. */
    @PostMapping("/sessions/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logOutEverywhere(@AuthenticationPrincipal Jwt jwt) {
        accountService.logOutEverywhere(userId(jwt));
    }

    @GetMapping("/export")
    public ResponseEntity<DataExport> export(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"nextrep-data-" + LocalDate.now() + ".json\"")
                .body(accountService.export(userId(jwt)));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PasswordConfirmation confirmation) {
        accountService.deleteAccount(userId(jwt), confirmation);
    }

    @PutMapping("/avatar")
    public UserResponse choosePreset(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AvatarPresetRequest request) {
        return accountService.choosePreset(userId(jwt), request.preset());
    }

    @PostMapping(path = "/avatar/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserResponse uploadPhoto(@AuthenticationPrincipal Jwt jwt, @RequestParam("file") MultipartFile file) throws IOException {
        if (file.getSize() > AvatarImages.MAX_BYTES) throw new InvalidAvatarException("The picture must be at most 200 KB");
        return accountService.uploadPhoto(userId(jwt), file.getBytes());
    }

    @DeleteMapping("/avatar")
    public UserResponse resetAvatar(@AuthenticationPrincipal Jwt jwt) {
        return accountService.resetAvatar(userId(jwt));
    }

    /** The picture needs the token, so the web app fetches it with JavaScript (an <img> tag can't send headers). */
    @GetMapping("/avatar/photo")
    public ResponseEntity<byte[]> photo(@AuthenticationPrincipal Jwt jwt) {
        return accountService.photo(userId(jwt))
                .map(avatar -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(avatar.getContentType()))
                        .cacheControl(CacheControl.noCache().cachePrivate())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(avatar.getData()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
