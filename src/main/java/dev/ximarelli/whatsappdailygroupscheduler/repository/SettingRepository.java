package dev.ximarelli.whatsappdailygroupscheduler.repository;

import dev.ximarelli.whatsappdailygroupscheduler.domain.SettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<SettingEntity, String> {
}
