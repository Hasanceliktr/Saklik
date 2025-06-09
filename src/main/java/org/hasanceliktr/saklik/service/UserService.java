package org.hasanceliktr.saklik.service;

import jakarta.transaction.Transactional;
import org.hasanceliktr.saklik.dto.RegisterRequestDto;
import org.hasanceliktr.saklik.dto.UserDto;
import org.hasanceliktr.saklik.entity.User;
import org.hasanceliktr.saklik.exception.EmailAlreadyExistsException;
import org.hasanceliktr.saklik.exception.UsernameAlreadyExistsException;
import org.hasanceliktr.saklik.repository.UserRepository;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;


    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.modelMapper = modelMapper;
    }

    @Transactional
    public UserDto registerUser(RegisterRequestDto registerRequestDto) {
        logger.info("Yeni kullanıcı kayıt isteği alındı:{}",registerRequestDto.getUsername());
        if(userRepository.existsByUsername(registerRequestDto.getUsername())) {
            logger.warn("Kullanıcı adı zaten mevcut:{}",registerRequestDto.getUsername());
            throw new UsernameAlreadyExistsException("Bu kullanıcı adı zaten kullanımda."+registerRequestDto.getUsername());
        }
        if(userRepository.existsByEmail(registerRequestDto.getEmail())) {
            logger.warn("E-posta zaten mevcut:{}",registerRequestDto.getEmail());
            throw new EmailAlreadyExistsException("Bu E-posta adresi zaten kullanımda"+registerRequestDto.getEmail());
        }

        User newUser = new User();
        newUser.setUsername(registerRequestDto.getUsername());
        newUser.setEmail(registerRequestDto.getEmail());
        newUser.setPassword(passwordEncoder.encode(registerRequestDto.getPassword()));
        User savedUser = userRepository.save(newUser);
        logger.info("Kullanıcı başarıyla kaydedildi:{} (ID:{})",savedUser.getUsername(),savedUser.getId());
        return modelMapper.map(savedUser,UserDto.class);

    }
}
