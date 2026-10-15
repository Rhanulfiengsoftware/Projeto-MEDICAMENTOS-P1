package br.edu.unifan.farmacia.validation;

import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Regras de validação do domínio. Todos os métodos coletam <b>todos</b> os
 * erros encontrados (em vez de parar no primeiro) para que o usuário possa
 * corrigir o formulário de uma só vez.
 */
public final class Validador {

    /** Código: exatamente 7 caracteres alfanuméricos. */
    private static final Pattern CODIGO = Pattern.compile("^[A-Za-z0-9]{7}$");
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public static final int NOME_TAMANHO_MINIMO = 3;

    public static final Set<String> UFS = Set.of(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

    private Validador() {
    }

    // ------------------------------------------------------------------
    // Medicamento
    // ------------------------------------------------------------------

    public static List<String> validar(Medicamento m, LocalDate hoje) {
        List<String> erros = new ArrayList<>();
        if (m == null) {
            erros.add("Medicamento não informado.");
            return erros;
        }

        if (isVazio(m.getCodigo()) || !CODIGO.matcher(m.getCodigo()).matches()) {
            erros.add("Código inválido: informe exatamente 7 caracteres alfanuméricos (ex.: AB12345).");
        }

        if (isVazio(m.getNome())) {
            erros.add("Nome é obrigatório.");
        } else if (m.getNome().trim().length() < NOME_TAMANHO_MINIMO) {
            erros.add("Nome deve ter no mínimo " + NOME_TAMANHO_MINIMO + " caracteres.");
        }

        if (isVazio(m.getPrincipioAtivo())) {
            erros.add("Princípio ativo é obrigatório.");
        }

        if (m.getDataValidade() == null) {
            erros.add("Data de validade é obrigatória.");
        } else if (m.getDataValidade().isBefore(hoje)) {
            erros.add("Data de validade não pode ser uma data passada.");
        }

        if (m.getQuantidadeEstoque() < 0) {
            erros.add("Quantidade em estoque não pode ser negativa.");
        }

        if (m.getPreco() == null || m.getPreco().compareTo(BigDecimal.ZERO) <= 0) {
            erros.add("Preço deve ser um valor positivo.");
        }

        erros.addAll(validar(m.getFornecedor()));
        return erros;
    }

    // ------------------------------------------------------------------
    // Fornecedor
    // ------------------------------------------------------------------

    public static List<String> validar(Fornecedor f) {
        List<String> erros = new ArrayList<>();
        if (f == null) {
            erros.add("Fornecedor é obrigatório.");
            return erros;
        }

        if (isVazio(f.getCnpj())) {
            erros.add("CNPJ é obrigatório.");
        } else if (!cnpjValido(f.getCnpj())) {
            erros.add("CNPJ inválido: verifique o formato e os dígitos verificadores.");
        }

        if (isVazio(f.getRazaoSocial())) {
            erros.add("Razão social do fornecedor é obrigatória.");
        }

        String telefone = somenteDigitos(f.getTelefone());
        if (telefone.length() != 10 && telefone.length() != 11) {
            erros.add("Telefone inválido: informe DDD + número (10 ou 11 dígitos).");
        }

        if (isVazio(f.getEmail()) || !EMAIL.matcher(f.getEmail().trim()).matches()) {
            erros.add("E-mail do fornecedor inválido.");
        }

        if (isVazio(f.getCidade())) {
            erros.add("Cidade do fornecedor é obrigatória.");
        }

        if (isVazio(f.getEstado()) || !UFS.contains(f.getEstado().trim().toUpperCase())) {
            erros.add("Estado (UF) do fornecedor inválido.");
        }
        return erros;
    }

    // ------------------------------------------------------------------
    // CNPJ
    // ------------------------------------------------------------------

    private static final int[] PESO_DV1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESO_DV2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    /**
     * Valida o CNPJ (com ou sem máscara): 14 dígitos, não pode ser uma
     * sequência repetida e os dois dígitos verificadores devem conferir.
     */
    public static boolean cnpjValido(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14 || d.chars().distinct().count() == 1) {
            return false;
        }
        int dv1 = calcularDigito(d, PESO_DV1);
        int dv2 = calcularDigito(d, PESO_DV2);
        return dv1 == d.charAt(12) - '0' && dv2 == d.charAt(13) - '0';
    }

    private static int calcularDigito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** Formata 14 dígitos como 00.000.000/0000-00; outros valores voltam como estão. */
    public static String formatarCnpj(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14) {
            return cnpj == null ? "" : cnpj;
        }
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8)
                + "/" + d.substring(8, 12) + "-" + d.substring(12);
    }

    // ------------------------------------------------------------------
    // Utilitários
    // ------------------------------------------------------------------

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static boolean isVazio(String s) {
        return s == null || s.isBlank();
    }
}
