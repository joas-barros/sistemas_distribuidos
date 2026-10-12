package edu.unidade2.cdn.src.model;

public record Livro(
        String id,
        String nome,
        String autor,
        int lancamento,
        String descricao
) {
    public String toJson() {
        return String.format(
                "{\n" +
                        "  \"id\": \"%s\",\n" +
                        "  \"nome\": \"%s\",\n" +
                        "  \"autor\": \"%s\",\n" +
                        "  \"lancamento\": %d,\n" +
                        "  \"descricao\": \"%s\"\n" +
                        "}",
                id, nome, autor, lancamento, descricao
        );
    }
}
