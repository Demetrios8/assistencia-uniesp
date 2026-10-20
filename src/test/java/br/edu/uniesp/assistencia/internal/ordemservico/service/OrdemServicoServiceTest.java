package br.edu.uniesp.assistencia.internal.ordemservico.service;

import br.edu.uniesp.assistencia.internal.cliente.entity.ClienteEntity;
import br.edu.uniesp.assistencia.internal.cliente.repository.ClienteRepository;
import br.edu.uniesp.assistencia.internal.equipamento.entity.EquipamentoEntity;
import br.edu.uniesp.assistencia.internal.equipamento.repository.EquipamentoRepository;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoRequest;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.OrdemServicoEntity;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.repository.OrdemServicoRepository;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrdemServicoServiceTest {

    private OrdemServicoRepository ordemServicoRepository;
    private ClienteRepository clienteRepository;
    private EquipamentoRepository equipamentoRepository;
    private OrdemServicoService service;

    @BeforeEach
    void configurar() {
        ordemServicoRepository = mock(OrdemServicoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        equipamentoRepository = mock(EquipamentoRepository.class);
        service = new OrdemServicoService(
                ordemServicoRepository, clienteRepository, equipamentoRepository);
    }

    @Test
    void deveSalvarOrdemComProtocoloUnico() {
        ClienteEntity cliente = new ClienteEntity("Cliente", "12345678901", "cliente@example.com");
        EquipamentoEntity equipamento = new EquipamentoEntity("Notebook", "Marca", "Modelo", "123");
        equipamento.associarCliente(cliente);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.of(equipamento));
        when(ordemServicoRepository.existsByNumeroProtocolo(anyString())).thenReturn(true, false);
        when(ordemServicoRepository.save(any(OrdemServicoEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CriarOrdemServicoResponse response = service.abrirOrdemServico(
                new CriarOrdemServicoRequest(1L, 2L, "Notebook não liga."));

        ArgumentCaptor<OrdemServicoEntity> ordemCaptor = ArgumentCaptor.forClass(OrdemServicoEntity.class);
        verify(ordemServicoRepository).save(ordemCaptor.capture());
        verify(ordemServicoRepository, times(2)).existsByNumeroProtocolo(anyString());
        OrdemServicoEntity ordemSalva = ordemCaptor.getValue();

        assertEquals(StatusOrdemServico.RECEBIDA, response.status());
        assertTrue(ordemSalva.getNumeroProtocolo().matches(
                "OS-" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "-[A-F0-9]{4}"));
        assertNotNull(response.dataAbertura());
        assertEquals(1, ordemSalva.getHistorico().size());
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoExiste() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.abrirOrdemServico(new CriarOrdemServicoRequest(
                        1L, 2L, "Notebook não liga.")));

        assertEquals("Cliente", exception.getRecurso());
        verify(ordemServicoRepository, org.mockito.Mockito.never())
                .save(any(OrdemServicoEntity.class));
    }

    @Test
    void deveRetornarNotFoundQuandoEquipamentoNaoExiste() {
        ClienteEntity cliente = new ClienteEntity("Cliente", "12345678901", "cliente@example.com");
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(equipamentoRepository.findById(2L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> service.abrirOrdemServico(new CriarOrdemServicoRequest(
                        1L, 2L, "Notebook não liga.")));

        assertEquals("Equipamento", exception.getRecurso());
        verify(ordemServicoRepository, org.mockito.Mockito.never())
                .save(any(OrdemServicoEntity.class));
    }
}
