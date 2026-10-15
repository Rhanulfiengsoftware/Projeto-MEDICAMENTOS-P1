package br.edu.unifan.farmacia.service;

import br.edu.unifan.farmacia.model.Medicamento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Relatórios do estoque, todos implementados com a API Stream
 * (filter, map, reduce, sorted, collect, groupingBy, partitioningBy).
 */
public class RelatorioService {

    public static final int DIAS_VENCIMENTO_PROXIMO = 30;
    public static final int LIMITE_ESTOQUE_BAIXO = 5;

    /** Linha do relatório de valor de estoque por fornecedor. */
    public record ValorEstoqueFornecedor(String cnpj, String razaoSocial,
                                         int totalUnidades, BigDecimal valorTotal) {
    }

    /**
     * Medicamentos que vencem entre hoje e hoje + 30 dias (inclusive),
     * do que vence primeiro para o que vence depois.
     */
    public List<Medicamento> proximosAoVencimento(List<Medicamento> medicamentos, LocalDate hoje) {
        LocalDate limite = hoje.plusDays(DIAS_VENCIMENTO_PROXIMO);
        return medicamentos.stream()
                .filter(m -> !m.getDataValidade().isBefore(hoje) && !m.getDataValidade().isAfter(limite))
                .sorted(Comparator.comparing(Medicamento::getDataValidade))
                .toList();
    }

    /** Medicamentos com menos de 5 unidades em estoque (menor quantidade primeiro). */
    public List<Medicamento> estoqueBaixo(List<Medicamento> medicamentos) {
        return medicamentos.stream()
                .filter(m -> m.getQuantidadeEstoque() < LIMITE_ESTOQUE_BAIXO)
                .sorted(Comparator.comparingInt(Medicamento::getQuantidadeEstoque))
                .toList();
    }

    /**
     * Valor total do estoque (preço x quantidade) agrupado por fornecedor,
     * do maior para o menor valor.
     */
    public List<ValorEstoqueFornecedor> valorTotalPorFornecedor(List<Medicamento> medicamentos) {
        Map<String, List<Medicamento>> porCnpj = medicamentos.stream()
                .collect(Collectors.groupingBy(m -> m.getFornecedor().getCnpj(),
                        LinkedHashMap::new, Collectors.toList()));

        return porCnpj.values().stream()
                .map(grupo -> new ValorEstoqueFornecedor(
                        grupo.get(0).getFornecedor().getCnpj(),
                        grupo.get(0).getFornecedor().getRazaoSocial(),
                        grupo.stream().mapToInt(Medicamento::getQuantidadeEstoque).sum(),
                        grupo.stream()
                                .map(Medicamento::getValorTotalEstoque)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)))
                .sorted(Comparator.comparing(ValorEstoqueFornecedor::valorTotal).reversed())
                .toList();
    }

    /** Valor total de todo o estoque (map + reduce). */
    public BigDecimal valorTotalGeral(List<Medicamento> medicamentos) {
        return medicamentos.stream()
                .map(Medicamento::getValorTotalEstoque)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Separa os medicamentos em controlados ({@code true}) e não
     * controlados ({@code false}), cada grupo ordenado por nome.
     */
    public Map<Boolean, List<Medicamento>> controladosVersusNaoControlados(List<Medicamento> medicamentos) {
        return medicamentos.stream()
                .sorted(Comparator.comparing(Medicamento::getNome, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.partitioningBy(Medicamento::isControlado));
    }
}
