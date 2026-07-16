package com.busantrip.user;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table("app_users")
public record AppUser(
        @Id @Column("id") Long id,
        @Column("email") String email,
        @Column("password_hash") String passwordHash,
        @Column("display_name") String displayName,
        @Column("created_at") Instant createdAt
) {
}
