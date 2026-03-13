package com.kotobalearn.backend.dto;

import com.kotobalearn.backend.model.Tag;

public record TagDto(
    Integer tagId,
    String  tagCode,
    String  tagType,
    String  tagLabel
) {
    public static TagDto from(Tag tag) {
        return new TagDto(tag.getTagId(), tag.getTagCode(), tag.getTagType(), tag.getTagLabel());
    }
}