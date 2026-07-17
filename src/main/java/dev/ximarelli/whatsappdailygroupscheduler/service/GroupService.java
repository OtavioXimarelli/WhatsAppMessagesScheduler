package dev.ximarelli.whatsappdailygroupscheduler.service;

import dev.ximarelli.whatsappdailygroupscheduler.domain.GroupEntity;
import dev.ximarelli.whatsappdailygroupscheduler.domain.SettingEntity;
import dev.ximarelli.whatsappdailygroupscheduler.repository.GroupRepository;
import dev.ximarelli.whatsappdailygroupscheduler.repository.SettingRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
public class GroupService {
    private static final String TARGET_GROUP_SETTING_KEY = "target_group_jid";

    private final GroupRepository groupRepository;
    private final SettingRepository settingRepository;

    public GroupService(GroupRepository groupRepository, SettingRepository settingRepository) {
        this.groupRepository = groupRepository;
        this.settingRepository = settingRepository;
    }

    public List<GroupEntity> getAllGroups() {
        return groupRepository.findAll();
    }

    public Optional<GroupEntity> getGroupById(UUID id) {
        return groupRepository.findById(id);
    }

    public List<GroupEntity> getSelectedGroups() {
        return groupRepository.findByIsSelectedTrue();
    }

    @Transactional
    public GroupEntity saveOrUpdateGroup(GroupEntity groupEntity) {
        if (groupEntity.getGroupJid() == null || groupEntity.getGroupJid().isBlank()) {
            throw new IllegalArgumentException("Group JID cannot be empty");
        }

        Optional<GroupEntity> existing = groupEntity.getId() != null
                ? groupRepository.findById(groupEntity.getId())
                : groupRepository.findByGroupJid(groupEntity.getGroupJid());

        if (existing.isPresent()) {
            GroupEntity toUpdate = existing.get();
            if (groupEntity.getName() != null) {
                toUpdate.setName(groupEntity.getName());
            }
            toUpdate.setGroupJid(groupEntity.getGroupJid());
            toUpdate.setSelected(groupEntity.isSelected());
            GroupEntity saved = groupRepository.save(toUpdate);
            if (saved.isSelected()) {
                syncTargetGroupSetting(saved.getGroupJid());
            }
            return saved;
        } else {
            GroupEntity saved = groupRepository.save(groupEntity);
            if (saved.isSelected()) {
                syncTargetGroupSetting(saved.getGroupJid());
            }
            return saved;
        }
    }

    @Transactional
    public GroupEntity selectGroup(UUID id, boolean exclusive) {
        GroupEntity target = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group not found with id " + id));

        if (exclusive) {
            List<GroupEntity> all = groupRepository.findAll();
            for (GroupEntity g : all) {
                g.setSelected(g.getId().equals(id));
            }
            groupRepository.saveAll(all);
        } else {
            target.setSelected(true);
            groupRepository.save(target);
        }

        syncTargetGroupSetting(target.getGroupJid());
        return target;
    }

    @Transactional
    public GroupEntity deselectGroup(UUID id) {
        GroupEntity target = groupRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Group not found with id " + id));
        target.setSelected(false);
        return groupRepository.save(target);
    }

    @Transactional
    public void deleteGroupById(UUID id) {
        if (!groupRepository.existsById(id)) {
            throw new NoSuchElementException("Group not found with id " + id);
        }
        groupRepository.deleteById(id);
    }

    @Transactional
    public void updateTargetGroupJid(String targetGroupJid) {
        if (targetGroupJid == null || targetGroupJid.isBlank()) {
            throw new IllegalArgumentException("Target Group JID cannot be empty");
        }
        syncTargetGroupSetting(targetGroupJid);

        Optional<GroupEntity> existing = groupRepository.findByGroupJid(targetGroupJid);
        if (existing.isPresent()) {
            GroupEntity g = existing.get();
            g.setSelected(true);
            groupRepository.save(g);
        } else {
            GroupEntity newGroup = new GroupEntity(targetGroupJid, targetGroupJid, true);
            groupRepository.save(newGroup);
        }
    }

    private void syncTargetGroupSetting(String groupJid) {
        SettingEntity setting = settingRepository.findById(TARGET_GROUP_SETTING_KEY)
                .orElse(new SettingEntity(TARGET_GROUP_SETTING_KEY, groupJid));
        setting.setSettingValue(groupJid);
        settingRepository.save(setting);
    }
}
