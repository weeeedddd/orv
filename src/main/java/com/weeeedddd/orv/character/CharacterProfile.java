package com.weeeedddd.orv.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;

import java.util.Comparator;
import java.util.List;

/**
 * The skills and attributes a player has acquired.
 *
 * <p>Stigmas are not held here: they already live on the
 * {@code orv:player_sponsor} attachment and are resolved for display by
 * {@link CharacterService}.
 *
 * @param age        the character's age, shown on the sheet
 * @param attributes acquired attributes, each with a rarity
 * @param skills     acquired skills, each with a level and a stolen flag
 */
public record CharacterProfile(
        int age,
        List<Attribute> attributes,
        List<Skill> skills
) {
    public static final int MAX_ENTRY_NAME_LENGTH = 48;
    /** Guards the sync payload against unbounded lists. */
    public static final int MAX_ENTRIES = 128;

    public static final CharacterProfile DEFAULT =
            new CharacterProfile(0, List.of(), List.of());

    public CharacterProfile {
        if (age < 0) {
            throw new IllegalArgumentException("age must not be negative");
        }
        attributes = List.copyOf(attributes);
        skills = List.copyOf(skills);
        if (attributes.size() > MAX_ENTRIES || skills.size() > MAX_ENTRIES) {
            throw new IllegalArgumentException(
                    "profile exceeds " + MAX_ENTRIES + " entries"
            );
        }
    }

    public static final Codec<CharacterProfile> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.INT.optionalFieldOf("age", 0)
                            .forGetter(CharacterProfile::age),
                    Attribute.CODEC.listOf()
                            .optionalFieldOf("attributes", List.of())
                            .forGetter(CharacterProfile::attributes),
                    Skill.CODEC.listOf()
                            .optionalFieldOf("skills", List.of())
                            .forGetter(CharacterProfile::skills)
            ).apply(instance, CharacterProfile::new));


    public CharacterProfile withAge(int newAge) {
        return new CharacterProfile(newAge, attributes, skills);
    }

    /**
     * Adds a skill, replacing any existing entry with the same name so a
     * level-up does not duplicate the row.
     */
    public CharacterProfile withSkill(Skill skill) {
        List<Skill> merged = new java.util.ArrayList<>(
                skills.stream()
                        .filter(existing -> !existing.name()
                                .equalsIgnoreCase(skill.name()))
                        .toList()
        );
        merged.add(skill);
        merged.sort(SKILL_ORDER);
        return new CharacterProfile(age, attributes, merged);
    }

    public CharacterProfile withoutSkill(String name) {
        return new CharacterProfile(
                age,
                attributes,
                skills.stream()
                        .filter(skill -> !skill.name().equalsIgnoreCase(name))
                        .toList()
        );
    }

    public CharacterProfile withAttribute(Attribute attribute) {
        List<Attribute> merged = new java.util.ArrayList<>(
                attributes.stream()
                        .filter(existing -> !existing.name()
                                .equalsIgnoreCase(attribute.name()))
                        .toList()
        );
        merged.add(attribute);
        merged.sort(ATTRIBUTE_ORDER);
        return new CharacterProfile(age, merged, skills);
    }

    public CharacterProfile withoutAttribute(String name) {
        return new CharacterProfile(
                age,
                attributes.stream()
                        .filter(entry -> !entry.name().equalsIgnoreCase(name))
                        .toList(),
                skills
        );
    }

    /** Owned skills first, then alphabetically, matching the sheet layout. */
    private static final Comparator<Skill> SKILL_ORDER =
            Comparator.comparing(Skill::stolen)
                    .thenComparing(Skill::name, String.CASE_INSENSITIVE_ORDER);

    /** Rarest attributes first. */
    private static final Comparator<Attribute> ATTRIBUTE_ORDER =
            Comparator.comparingInt(
                            (Attribute attribute) -> attribute.rarity().ordinal()
                    )
                    .reversed()
                    .thenComparing(
                            Attribute::name,
                            String.CASE_INSENSITIVE_ORDER
                    );

    /**
     * A single skill row.
     *
     * @param name   display name, without brackets
     * @param level  skill level, at least 1
     * @param stolen whether the skill was taken from someone else
     */
    public record Skill(String name, int level, boolean stolen) {
        public Skill {
            name = trim(name);
            if (level < 1) {
                throw new IllegalArgumentException("level must be at least 1");
            }
        }

        public static final Codec<Skill> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Skill::name),
                        Codec.INT.fieldOf("level").forGetter(Skill::level),
                        Codec.BOOL.optionalFieldOf("stolen", false)
                                .forGetter(Skill::stolen)
                ).apply(instance, Skill::new));


        /** Rendered form, e.g. {@code [MUSCLE MEMORY LV.4]}. */
        public String display() {
            return "[" + name.toUpperCase() + " LV." + level + "]";
        }
    }

    /**
     * A single attribute row.
     *
     * @param name   display name
     * @param rarity drives the muted tag drawn after the name
     */
    public record Attribute(String name, Rarity rarity) {
        public Attribute {
            name = trim(name);
        }

        public static final Codec<Attribute> CODEC =
                RecordCodecBuilder.create(instance -> instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Attribute::name),
                        Rarity.CODEC.optionalFieldOf("rarity", Rarity.COMMON)
                                .forGetter(Attribute::rarity)
                ).apply(instance, Attribute::new));


        public String display() {
            return name.toUpperCase();
        }
    }

    /** Attribute rarity, ordered from most common to rarest. */
    public enum Rarity implements StringRepresentable {
        COMMON("common"),
        RARE("rare"),
        EPIC("epic"),
        LEGENDARY("legendary");

        public static final Codec<Rarity> CODEC =
                StringRepresentable.fromEnum(Rarity::values);


        private final String id;

        Rarity(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        /** Rendered form, e.g. {@code (RARE)}. */
        public String tag() {
            return "(" + name() + ")";
        }
    }

    private static String trim(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        String name = raw.trim();
        return name.length() <= MAX_ENTRY_NAME_LENGTH
                ? name
                : name.substring(0, MAX_ENTRY_NAME_LENGTH);
    }
}
