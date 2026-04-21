package com.metriclab.model.dto;

import java.util.List;

public record UploadIndex(List<UploadedFileInfo> files) {
}
