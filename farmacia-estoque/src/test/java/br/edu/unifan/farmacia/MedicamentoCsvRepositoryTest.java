package br.edu.unifan.farmacia;

import br.edu.unifan.farmacia.exception.PersistenciaException;
import br.edu.unifan.farmacia.exception.ValidacaoException;
import br.edu.unifan.farmacia.model.Fornecedor;
import br.edu.unifan.farmacia.model.Medicamento;
import br.edu.unifan.farmacia.repository.MedicamentoCsvRepository;
import br.edu.unifan.farmacia.service.MedicamentoService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class MedicamentoCsvRepositoryTest {

    @TempDir
    Path pasta;

    private Medicamento novo(String codigo) {
        Fornecedor f = new Fornecedor("11.222.333/0001-81", "Forn; Teste", "(62) 3222-1100",
                "x@y.com", "Goiânia", "go");
        return new Medicamento(codigo, "Dipirona", "linha1\nlinha2; com ponto-e-vírgula", "Dipirona",
                LocalDate.now().plusDays(60), 10, new BigDecimal("9.9"), true, f);
    }

    @Test
    void criaArquivoComCabecalhoQuandoNaoExiste() throws Exception {
        Path csv = pasta.resolve("dados").resolve("med.csv");
        new MedicamentoCsvRepository(csv);
        assertTrue(Files.exists(csv));
        assertTrue(Files.readAllLines(csv, StandardCharsets.UTF_8).get(0).startsWith("codigo;nome;"));
    }

    @Test
    void salvaERecarregaOsDados() throws Exception {
        Path csv = pasta.resolve("med.csv");
        MedicamentoService service = new MedicamentoService(new MedicamentoCsvRepository(csv));
        service.cadastrar(novo("ab12345"));

        // novo repositório lendo o mesmo arquivo = "reiniciar o sistema"
        MedicamentoService recarregado = new MedicamentoService(new MedicamentoCsvRepository(csv));
        Medicamento m = recarregado.consultar("AB12345").orElseThrow();
        assertEquals("Dipirona", m.getNome());
        assertEquals("11222333000181", m.getFornecedor().getCnpj());
        assertEquals("GO", m.getFornecedor().getEstado());
        assertTrue(m.isControlado());
        assertEquals(0, new BigDecimal("9.90").compareTo(m.getPreco()));
    }

    @Test
    void naoPermiteCodigoDuplicado() throws Exception {
        MedicamentoService service = new MedicamentoService(new MedicamentoCsvRepository(pasta.resolve("med.csv")));
        service.cadastrar(novo("AB12345"));
        assertThrows(ValidacaoException.class, () -> service.cadastrar(novo("AB12345")));
    }

    @Test
    void excluiEPersiste() throws Exception {
        Path csv = pasta.resolve("med.csv");
        MedicamentoService service = new MedicamentoService(new MedicamentoCsvRepository(csv));
        service.cadastrar(novo("AB12345"));
        service.excluir("ab12345");

        assertTrue(new MedicamentoService(new MedicamentoCsvRepository(csv)).listar().isEmpty());
        assertThrows(ValidacaoException.class, () -> service.excluir("AB12345"));
    }

    @Test
    void csvCorrompidoGeraPersistenciaException() throws Exception {
        Path csv = pasta.resolve("med.csv");
        Files.writeString(csv, "cabecalho\nlinha;quebrada\n", StandardCharsets.UTF_8);
        assertThrows(PersistenciaException.class, () -> new MedicamentoCsvRepository(csv));
    }
}
