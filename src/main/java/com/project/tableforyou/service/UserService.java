package com.project.tableforyou.service;

import com.project.tableforyou.domain.dto.UserDto;
import com.project.tableforyou.domain.entity.User;
import com.project.tableforyou.repository.UserRepository;
import com.project.tableforyou.service.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final MailService mailService;
    private final Map<String, String> codeMap = new ConcurrentHashMap<>();

    /* 회원 추가 */
    @Transactional
    public Long create(UserDto.Request dto) {
        dto.setPassword(bCryptPasswordEncoder.encode(dto.getPassword()));
        User user = dto.toEntity();

        userRepository.save(user);

        return user.getId();
    }

    /* 회원 불러오기 */
    @Transactional(readOnly = true)
    public UserDto.Response findById(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 회원이 존재하지 않습니다. id: " + id));

        return new UserDto.Response(user);
    }

    /* 전체 회원 불러오기 */
    @Transactional(readOnly = true)
    public Page<UserDto.Response> userPageList(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(UserDto.Response::new);
    }


    /* 회원 업데이트 */
    @Transactional
    public void update(Long id, UserDto.Request dto) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 회원이 존재하지 않습니다. id: " + id));
        dto.setPassword(bCryptPasswordEncoder.encode(dto.getPassword()));
        user.update(dto.getNickname(), dto.getPassword(), dto.getEmail());
    }

    /* 회원 삭제 */
    @Transactional
    public void delete(Long id) {
        User user = userRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("해당 회원이 존재하지 않습니다. id: " + id));
        userRepository.delete(user);
    }

    /* 회원가입 오류 확인 */
    public Map<String, String> validateHandler(Errors errors) {
        Map<String, String> validateResult = new HashMap<>();

        for (FieldError error: errors.getFieldErrors()) {
            String validKeyName = "valid_" + error.getField();
            validateResult.put(validKeyName, error.getDefaultMessage());
        }
        return validateResult;
    }

    /* 인증 메일 보내기 */
    public void sendCodeToMail(String email) {
        String authCode = createCode();
        mailService.sendMail(email, authCode);
        codeMap.put(email, authCode);
    }

    /* 인증 번호 만들기 */
    private String createCode() {
        try {
            Random random = SecureRandom.getInstanceStrong();   // 암호학적으로 안전한 무작위 수를 생성. 인증번호는 보안적으로 중요하기 SecureRandom 사용.
            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < 6; i++) {
                sb.append(random.nextInt(10));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            log.info("Failed to create secure random instance", e);
            throw new RuntimeException("Failed to generate secure random number", e);
        }
    }

    /* 인증 코드 확인 */
    public boolean verifiedCode(String email, String authCode) {
        if(codeMap.get(email).equals(authCode)) {
            codeMap.remove(email);      // 인증번호가 맞다면 삭제.
            return true;
        } else
            return false;
    }
}
