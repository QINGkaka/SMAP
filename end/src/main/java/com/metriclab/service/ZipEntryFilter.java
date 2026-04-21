package com.metriclab.service;

final class ZipEntryFilter {

    private ZipEntryFilter() {
    }

    static boolean isAnalyzableJava(String entryName) {
        if (entryName == null || !entryName.endsWith(".java")) {
            return false;
        }
        String normalized = entryName.replace("\\", "/");
        if (normalized.startsWith("__MACOSX/") || normalized.contains("/__MACOSX/")) {
            return false;
        }
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        return !fileName.startsWith("._") && !fileName.equals(".DS_Store");
    }
}
