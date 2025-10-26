package com.gng.test.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PersonRecord(
        @NotBlank(message = "ID is required")
        @Pattern(regexp = "^[0-9a-fA-F\\-]{36}$", message = "Invalid UUID format")
        String uuid,

        @NotBlank(message = "Code is required")
        String id,

        @NotBlank(message = "Name is required")
        String name,

        @NotBlank(message = "Likes field is required")
        String likes,

        @NotBlank(message = "Vehicle field is required")
        String transport,

        @DecimalMin(value = "0.0", message = "value1 must be >= 0.0")
        double avg_speed,

        @DecimalMin(value = "0.0", message = "value2 must be >= 0.0")
        double top_speed
) {
        public static Builder builder() {
                return new Builder();
        }

        public static class Builder {
                private String uuid;
                private String id;
                private String name;
                private String likes;
                private String transport;
                private double avg_speed;
                private double top_speed;

                public Builder uuid(String uuid) {
                        this.uuid = uuid;
                        return this;
                }

                public Builder id(String id) {
                        this.id = id;
                        return this;
                }

                public Builder name(String name) {
                        this.name = name;
                        return this;
                }

                public Builder likes(String likes) {
                        this.likes = likes;
                        return this;
                }

                public Builder transport(String transport) {
                        this.transport = transport;
                        return this;
                }

                public Builder avgSpeed(double avg_speed) {
                        this.avg_speed = avg_speed;
                        return this;
                }

                public Builder topSpeed(double top_speed) {
                        this.top_speed = top_speed;
                        return this;
                }

                public PersonRecord build() {
                        return new PersonRecord(uuid, id, name, likes, transport, avg_speed, top_speed);
                }
        }
}