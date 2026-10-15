package br.edu.unifan.farmacia;

import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;
import br.edu.unifan.farmacia.validation.Validador;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValidadorTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 15);

    private Fornecedor fornecedorValido() {
        return new Fornecedor("11222333000181", "Distribuidora Teste", "62999998888",
                "contato@teste.com.br", "Goiânia", "GO");
    }

    private Medicamento medicamentoValido() {
        return new Medicamento("AB12345", "Dipirona", "desc", "Dipirona sódica",
                HOJE.plusDays(10), 10, new BigDecimal("9.90"), false, fornecedorValido());
    }

    @Test
    void cnpjValidoComESemMascara() {
        assertTrue(Validador.cnpjValido("11222333000181"));
        assertTrue(Validador.cnpjValido("11.222.333/0001-81"));
    }

    @Test
    void cnpjInvalido() {
        assertFalse(Validador.cnpjValido("11222333000182"));   // dígito errado
        assertFalse(Validador.cnpjValido("11111111111111"));   // sequência repetida
        assertFalse(Validador.cnpjValido("123"));              // tamanho errado
        assertFalse(Validador.cnpjValido(null));
    }

    @Test
    void formataCnpj() {
        assertEquals("11.222.333/0001-81", Validador.formatarCnpj("11222333000181"));
    }

    @Test
    void medicamentoValidoNaoGeraErros() {
        assertTrue(Validador.validar(medicamentoValido(), HOJE).isEmpty());
    }

    @Test
    void codigoDeveTerSeteAlfanumericos() {
        Medicamento m = medicamentoValido();
        m.setCodigo("ABC12");
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setCodigo("AB-1234");
        assertFalse(Validador.validar(m, HOJE).isEmpty());
    }

    @Test
    void nomeVazioOuCurtoEInvalido() {
        Medicamento m = medicamentoValido();
        m.setNome("  ");
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setNome("ab");
        assertFalse(Validador.validar(m, HOJE).isEmpty());
    }

    @Test
    void dataPassadaEInvalidaMasHojeEValida() {
        Medicamento m = medicamentoValido();
        m.setDataValidade(HOJE.minusDays(1));
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setDataValidade(HOJE);
        assertTrue(Validador.validar(m, HOJE).isEmpty());
    }

    @Test
    void quantidadeNegativaEInvalida() {
        Medicamento m = medicamentoValido();
        m.setQuantidadeEstoque(-1);
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setQuantidadeEstoque(0);
        assertTrue(Validador.validar(m, HOJE).isEmpty());
    }

    @Test
    void precoDevePositivo() {
        Medicamento m = medicamentoValido();
        m.setPreco(BigDecimal.ZERO);
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setPreco(new BigDecimal("-1"));
        assertFalse(Validador.validar(m, HOJE).isEmpty());
        m.setPreco(null);
        assertFalse(Validador.validar(m, HOJE).isEmpty());
    }

    @Test
    void coletaTodosOsErros() {
        Medicamento m = new Medicamento();
        List<String> erros = Validador.validar(m, HOJE);
        assertTrue(erros.size() >= 6, "deveria listar vários erros de uma vez");
    }
}
