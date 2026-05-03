package com.wuxl.englishcoach.api.coach.dto;

public record SavedNoteResponse(String type, String key, String label, String userText, String betterText) {
}
