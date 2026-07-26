package com.woori.library.dto;

import java.util.List;

public record SyncResponse(int synced, List<String> failed) {}
