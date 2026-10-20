package br.edu.uniesp.assistencia.internal.ordemservico.controller;

import br.edu.uniesp.assistencia.config.exception.GlobalExceptionHandler;
import br.edu.uniesp.assistencia.internal.ordemservico.dto.CriarOrdemServicoResponse;
import br.edu.uniesp.assistencia.internal.ordemservico.entity.StatusOrdemServico;
import br.edu.uniesp.assistencia.internal.ordemservico.service.OrdemServicoService;
import br.edu.uniesp.assistencia.shared.exception.RecursoNaoEncontradoException;
import br.edu.uniesp.assistencia.shared.exception.RegraNegocioException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrdemServicoControllerTest {

    private OrdemServicoService ordemServicoService;
    private MockMvc mockMvc;

    @BeforeEach
    void configurar() {
        ordemServicoService = mock(OrdemServicoService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new OrdemServicoController(ordemServicoService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void deveAbrirOrdemServicoComStatusCreated() throws Exception {
        when(ordemServicoService.abrirOrdemServico(any())).thenReturn(
                new CriarOrdemServicoResponse(1L, "OS-20261020-A1B2",
                        StatusOrdemServico.RECEBIDA, LocalDateTime.of(2026, 10, 20, 10, 30)));

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": 1,
                                  "equipamentoId": 2,
                                  "descricaoProblema": "Notebook não liga."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.numeroProtocolo").value("OS-20261020-A1B2"))
                .andExpect(jsonPath("$.status").value("RECEBIDA"))
                .andExpect(jsonPath("$.dataAbertura").exists());
    }

    @Test
    void deveRetornarBadRequestParaCamposInvalidos() throws Exception {
        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clienteId": null,
                                  "equipamentoId": null,
                                  "descricaoProblema": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erros.clienteId").exists())
                .andExpect(jsonPath("$.erros.equipamentoId").exists())
                .andExpect(jsonPath("$.erros.descricaoProblema").exists());

        org.mockito.Mockito.verifyNoInteractions(ordemServicoService);
    }

    @Test
    void deveRetornarNotFoundQuandoClienteNaoExiste() throws Exception {
        when(ordemServicoService.abrirOrdemServico(any()))
                .thenThrow(new RecursoNaoEncontradoException("Cliente", 999L));

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.recurso").value("Cliente"));
    }

    @Test
    void deveRetornarNotFoundQuandoEquipamentoNaoExiste() throws Exception {
        when(ordemServicoService.abrirOrdemServico(any()))
                .thenThrow(new RecursoNaoEncontradoException("Equipamento", 999L));

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.recurso").value("Equipamento"));
    }

    @Test
    void deveRetornarUnprocessableContentQuandoRegraDeNegocioForViolada() throws Exception {
        String mensagem = "Não é permitido abrir Ordem de Serviço para um cliente inativo.";
        when(ordemServicoService.abrirOrdemServico(any()))
                .thenThrow(new RegraNegocioException(mensagem));

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(mensagem));
    }

    @Test
    void deveRetornarUnprocessableContentQuandoEquipamentoNaoPertenceAoCliente() throws Exception {
        String mensagem = "O equipamento informado não pertence ao cliente indicado.";
        when(ordemServicoService.abrirOrdemServico(any()))
                .thenThrow(new RegraNegocioException(mensagem));

        mockMvc.perform(post("/ordens")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestValido()))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(mensagem));
    }

    private String requestValido() {
        return """
                {
                  "clienteId": 1,
                  "equipamentoId": 2,
                  "descricaoProblema": "Notebook não liga."
                }
                """;
    }
}
