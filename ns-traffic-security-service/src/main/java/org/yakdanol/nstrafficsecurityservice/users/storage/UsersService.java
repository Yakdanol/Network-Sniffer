package org.yakdanol.nstrafficsecurityservice.users.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service("securityUsersService")
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
}
