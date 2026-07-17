package dev.ximarelli.whatsappdailygroupscheduler.repository;

import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {
    Optional<GroupEntity> findByGroupJid(String groupJid);
    List<GroupEntity> findByIsSelectedTrue();
    boolean existsByGroupJid(String groupJid);
}
