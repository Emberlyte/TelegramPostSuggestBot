package org.example.predlozka_bot2.Model;

import jakarta.persistence.*;
import lombok.Data;
import org.example.predlozka_bot2.Enums.AppealBanUserStatus;
import org.example.predlozka_bot2.Enums.BanStatus;

@Data
@Entity
@Table(name = "banned_users")
public class BannedUsers {

    @Id
    @Column(name = "sender_id")
    private long senderId;

    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BanStatus status = BanStatus.UNBANNED;

    @Column
    @Enumerated(EnumType.ORDINAL)
    private AppealBanUserStatus appealBanUserStatus;

    @Column
    private String AppealText;
}
