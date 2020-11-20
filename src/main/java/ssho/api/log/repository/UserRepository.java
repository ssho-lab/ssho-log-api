package ssho.api.log.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ssho.api.log.domain.user.model.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    User findByName(String name);
    Optional<User> findById(int id);
}
