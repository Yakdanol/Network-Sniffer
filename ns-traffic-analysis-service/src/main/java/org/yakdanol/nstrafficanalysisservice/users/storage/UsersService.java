package org.yakdanol.nstrafficanalysisservice.users.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service("analysisUsersService")
@RequiredArgsConstructor
public class UsersService {
    private final UsersRepository usersRepository;

    public boolean isUserExist(String username) {
        return usersRepository.findByFullName(username).isPresent();
    }

    public Users findUser(String username) {
        return usersRepository.findByFullName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No such user"));
    }

    public List<Users> findAllUsers() {
        return usersRepository.findAll();
    }
}
