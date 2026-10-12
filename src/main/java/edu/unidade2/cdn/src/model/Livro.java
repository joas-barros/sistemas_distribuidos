package edu.unidade2.cdn.src.model;

public record Livro(
        String id,
        String nome,
        String autor,
        String lancamento,
        String descricao
) {
    public String toJson() {
        return String.format(
                "{\n" +
                        "  \"id\": \"%s\",\n" +
                        "  \"nome\": \"%s\",\n" +
                        "  \"autor\": \"%s\",\n" +
                        "  \"lancamento\": %s,\n" +
                        "  \"descricao\": \"%s\"\n" +
                        "}",
                id, nome, autor, lancamento, descricao
        );
    }
}
