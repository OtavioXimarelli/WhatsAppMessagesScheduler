package dev.ximarelli.whatsappdailygroupscheduler.domain;

import jakarta.persistence.*;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "whatsapp_groups")
public class GroupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name")
    private String name;

    @Column(name = "group_jid", nullable = false, unique = true)
    private String groupJid;

    @Column(name = "is_selected", nullable = false)
    private boolean isSelected;

    public GroupEntity() {
        this.isSelected = true;
    }

    public GroupEntity(String name, String groupJid, boolean isSelected) {
        this.name = name;
        this.groupJid = groupJid;
        this.isSelected = isSelected;
    }

    public GroupEntity(UUID id, String name, String groupJid, boolean isSelected) {
        this.id = id;
        this.name = name;
        this.groupJid = groupJid;
        this.isSelected = isSelected;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGroupJid() {
        return groupJid;
    }

    public void setGroupJid(String groupJid) {
        this.groupJid = groupJid;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupEntity that = (GroupEntity) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "GroupEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", groupJid='" + groupJid + '\'' +
                ", isSelected=" + isSelected +
                '}';
    }
}
