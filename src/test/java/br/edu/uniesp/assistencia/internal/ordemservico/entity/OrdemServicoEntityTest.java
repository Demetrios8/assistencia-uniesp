package br.edu.uniesp.assistencia.internal.ordemservico.entity;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrdemServicoEntityTest {

    @Test
    void deveCriarOrdemRecebidaComHistoricoDeAbertura() {
        ClienteEntity cliente = cliente("cliente");
        EquipamentoEntity equipamento = equipamentoDo(cliente);

        OrdemServicoEntity ordem = new OrdemServicoEntity(
                "OS-20261020-A1B2", cliente, equipamento, "Notebook não liga.");

        assertEquals(StatusOrdemServico.RECEBIDA, ordem.getStatus());
        assertNotNull(ordem.getDataAbertura());
        assertEquals(1, ordem.getHistorico().size());
        assertEquals(StatusOrdemServico.RECEBIDA, ordem.getHistorico().get(0).getNovoStatus());
        assertEquals("SISTEMA", ordem.getHistorico().get(0).getResponsavel());
        assertEquals("Abertura da ordem de serviço.", ordem.getHistorico().get(0).getObservacao());
    }

    @Test
    void naoDeveAbrirOrdemParaClienteInativo() {
        ClienteEntity cliente = cliente("cliente");
        cliente.inativar();

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> new OrdemServicoEntity("OS-20261020-A1B2", cliente,
                        equipamentoDo(cliente), "Notebook não liga."));

        assertEquals("Não é permitido abrir Ordem de Serviço para um cliente inativo.",
                exception.getMessage());
    }

    @Test
    void naoDeveAbrirOrdemComEquipamentoDeOutroCliente() {
        ClienteEntity cliente = cliente("cliente");
        EquipamentoEntity equipamentoDeOutroCliente = equipamentoDo(cliente("outro cliente"));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> new OrdemServicoEntity("OS-20261020-A1B2", cliente,
                        equipamentoDeOutroCliente, "Notebook não liga."));

        assertEquals("O equipamento informado não pertence ao cliente indicado.",
                exception.getMessage());
    }

    private ClienteEntity cliente(String nome) {
        return new ClienteEntity(nome, "12345678901", nome + "@example.com");
    }

    private EquipamentoEntity equipamentoDo(ClienteEntity cliente) {
        EquipamentoEntity equipamento = new EquipamentoEntity("Notebook", "Marca", "Modelo", "123");
        equipamento.associarCliente(cliente);
        return equipamento;
    }
}
