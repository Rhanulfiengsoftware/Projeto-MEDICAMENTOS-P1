package br.edu.unifan.farmacia;

import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;
import br.edu.unifan.farmacia.service.RelatorioService;
import br.edu.unifan.farmacia.service.RelatorioService.ValorEstoqueFornecedor;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);
    private final RelatorioService service = new RelatorioService();

    private final Fornecedor f1 = new Fornecedor("11222333000181", "Forn A", "6233334444", "a@a.com", "Goiânia", "GO");
    private final Fornecedor f2 = new Fornecedor("04567890000179", "Forn B", "1133334444", "b@b.com", "São Paulo", "SP");

    private Medicamento med(String cod, String nome, LocalDate validade, int qtd, String preco,
                            boolean controlado, Fornecedor f) {
        return new Medicamento(cod, nome, "", "pa", validade, qtd, new BigDecimal(preco), controlado, f);
    }

    private List<Medicamento> amostra() {
        return List.of(
                med("AAA0001", "Alfa", HOJE.plusDays(10), 3, "10.00", false, f1),   // vence logo + estoque baixo
                med("AAA0002", "Beta", HOJE.plusDays(30), 50, "2.00", true, f1),    // limite exato de 30 dias
                med("AAA0003", "Gama", HOJE.plusDays(31), 5, "5.00", false, f2),    // fora da janela, estoque = 5
                med("AAA0004", "Delta", HOJE.minusDays(2), 0, "7.00", true, f2));   // já vencido, estoque 0
    }

    @Test
    void proximosAoVencimentoInclui30DiasEExcluiVencidos() {
        List<Medicamento> r = service.proximosAoVencimento(amostra(), HOJE);
        assertEquals(List.of("AAA0001", "AAA0002"), r.stream().map(Medicamento::getCodigo).toList());
    }

    @Test
    void estoqueBaixoEMenorQueCinco() {
        List<Medicamento> r = service.estoqueBaixo(amostra());
        assertEquals(List.of("AAA0004", "AAA0001"), r.stream().map(Medicamento::getCodigo).toList());
    }

    @Test
    void valorTotalPorFornecedor() {
        List<ValorEstoqueFornecedor> r = service.valorTotalPorFornecedor(amostra());
        assertEquals(2, r.size());
        // Forn A: 3*10 + 50*2 = 130 ; Forn B: 5*5 + 0*7 = 25
        assertEquals("Forn A", r.get(0).razaoSocial());
        assertEquals(0, new BigDecimal("130.00").compareTo(r.get(0).valorTotal()));
        assertEquals(53, r.get(0).totalUnidades());
        assertEquals(0, new BigDecimal("25.00").compareTo(r.get(1).valorTotal()));
    }

    @Test
    void valorTotalGeral() {
        assertEquals(0, new BigDecimal("155.00").compareTo(service.valorTotalGeral(amostra())));
    }

    @Test
    void controladosVersusNaoControlados() {
        Map<Boolean, List<Medicamento>> r = service.controladosVersusNaoControlados(amostra());
        assertEquals(2, r.get(true).size());
        assertEquals(2, r.get(false).size());
    }

    @Test
    void listaVaziaNaoQuebra() {
        assertTrue(service.proximosAoVencimento(List.of(), HOJE).isEmpty());
        assertTrue(service.valorTotalPorFornecedor(List.of()).isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(service.valorTotalGeral(List.of())));
    }
}
