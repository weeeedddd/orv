package com.weeeedddd.orv.guild;

public enum GuildRole {
    LEADER("Leader", 3),
    VICE_LEADER("Vice-Leader", 2),
    MEMBER("Member", 1),
    NONE("No Guild", 0);

    private final String displayName;
    private final int authorityLevel;

    GuildRole(String displayName, int authorityLevel) {
        this.displayName = displayName;
        this.authorityLevel = authorityLevel;
    }

    public String displayName() {
        return displayName;
    }

    public int authorityLevel() {
        return authorityLevel;
    }

    public boolean canInvite() {
        return this == LEADER || this == VICE_LEADER;
    }

    public boolean canPromote(GuildRole targetRole) {
        return this == LEADER && targetRole == MEMBER;
    }

    public boolean canDemote(GuildRole targetRole) {
        return this == LEADER && targetRole == VICE_LEADER;
    }

    public boolean canKick(GuildRole targetRole) {
        return switch (this) {
            case LEADER -> targetRole == VICE_LEADER || targetRole == MEMBER;
            case VICE_LEADER -> targetRole == MEMBER;
            default -> false;
        };
    }

    public boolean canTransferLeadership(GuildRole targetRole) {
        return this == LEADER && targetRole == VICE_LEADER;
    }
}
