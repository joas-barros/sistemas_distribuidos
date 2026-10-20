package edu.unidade2.cdn.src.model;

public record EdgeResponse(
        String status,
        String cacheStatus,
        int length,
        String data
) {
    public String toJson() {
        String dataJson = (data == null || data.isEmpty()) ? "{}" : data;

        return String.format(
                "{\n" +
                        "  \"status\": \"%s\",\n" +
                        "  \"cache\": \"%s\",\n" +
                        "  \"length\": %d,\n" +
                        "  \"data\": %s\n" +
                        "}",
                status, cacheStatus, length, dataJson
        );
    }
}
